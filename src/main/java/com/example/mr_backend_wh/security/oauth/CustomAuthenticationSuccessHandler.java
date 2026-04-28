package com.example.mr_backend_wh.security.oauth;

import com.example.mr_backend_wh.model.Rol;
import com.example.mr_backend_wh.model.Usuario;
import com.example.mr_backend_wh.repository.RolRepository;
import com.example.mr_backend_wh.repository.UsuarioRepository;
import com.example.mr_backend_wh.security.JWTService;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
  private final TokenProvider tokenProvider;
  private final UsuarioRepository usuarioRepository;
  private final RolRepository rolRepository;
  private final JWTService jwtService;
  private final PasswordEncoder passwordEncoder;
  public static final String COOKIE_NAME = "X-Auth-Token";
  public CustomAuthenticationSuccessHandler
          (TokenProvider tokenProvider, UsuarioRepository usuarioRepository, RolRepository rolRepository, JWTService jwtService, PasswordEncoder passwordEncoder)
            { this.tokenProvider = tokenProvider;
              this.usuarioRepository = usuarioRepository;
              this.rolRepository = rolRepository;
              this.jwtService = jwtService;
              this.passwordEncoder = passwordEncoder; }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
    String principalIdentifier = authentication.getName();

    // Intentar extraer el email del principal de forma segura sin depender de la clase OAuth2User
    Object principal = authentication.getPrincipal();
    String possibleName = null;
    if (principal != null) {
      // 1) intentar método getAttribute("email")
      try {
        Method m = principal.getClass().getMethod("getAttribute", String.class);
        Object attr = m.invoke(principal, "email");
        if (attr != null) principalIdentifier = attr.toString();
        Object nameAttr = m.invoke(principal, "name");
        if (nameAttr != null) possibleName = nameAttr.toString();
      } catch (NoSuchMethodException ignored) {
        // 2) intentar método getAttributes() que devuelva Map
        try {
          Method m2 = principal.getClass().getMethod("getAttributes");
          Object attrs = m2.invoke(principal);
          if (attrs instanceof Map) {
            Object val = ((Map<?,?>)attrs).get("email");
            if (val != null) principalIdentifier = val.toString();
            Object nameVal = ((Map<?,?>)attrs).get("name");
            if (nameVal != null) possibleName = nameVal.toString();
          }
        } catch (Exception ignored2) {
          // no hacer nada, fallback al nombre
        }
      } catch (Exception e) {
        // cualquier fallo en reflexión no debe evitar el login
      }
    }

    // Normalizar principalIdentifier
    if (principalIdentifier == null) principalIdentifier = "unknown";

    // Ahora buscamos usuario por email (principalIdentifier) y preferimos generar token con formato interno (nomusuario)
    String token;
    try {
      Optional<Usuario> maybeUser = usuarioRepository.findTopByEmail(principalIdentifier);
      if (maybeUser.isPresent()) {
        Usuario usuario = maybeUser.get();
        token = jwtService.generateToken(usuario);
      } else {
        // Crear usuario nuevo y persistirlo antes de generar token
        String base = (possibleName != null && !possibleName.isBlank()) ? possibleName.replaceAll("\\s+", "_") : principalIdentifier.split("@")[0];
        String candidate = base.length() > 50 ? base.substring(0,50) : base;
        int suffix = 0;
        while (usuarioRepository.findTopByNomusuario(candidate).isPresent()) {
          suffix++;
          String s = base + suffix;
          candidate = s.length() > 50 ? s.substring(0,50) : s;
        }

        Usuario nuevo = new Usuario();
        nuevo.setNomusuario(candidate);
        nuevo.setEmail(principalIdentifier);
        nuevo.setNomcompleto(possibleName != null ? possibleName : candidate);
        nuevo.setContrasenia(passwordEncoder.encode(UUID.randomUUID().toString()));
        Rol rol = rolRepository.findById(1).orElseGet(() -> {
          Rol r = new Rol(); r.setId(1); r.setRol("USER"); return rolRepository.save(r);
        });
        nuevo.setRolid(rol);
        Usuario saved = usuarioRepository.save(nuevo);
        token = jwtService.generateToken(saved);
      }
    } catch (Exception e) {
      // cualquier fallo: fallback a token generico
      token = tokenProvider.generate(principalIdentifier, "USER");
    }

    ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token).httpOnly(true).secure(false).path("/").maxAge(3600).sameSite("Lax").build();
    response.addHeader("Set-Cookie", cookie.toString());
    getRedirectStrategy().sendRedirect(request, response, "http://localhost:3000/oauth2/redirect");
  }
}
