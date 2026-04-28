package com.example.mr_backend_wh.controller;

import com.example.mr_backend_wh.DTO.AuthenticationResponseDTO;
import com.example.mr_backend_wh.DTO.LoginDTO;
import com.example.mr_backend_wh.DTO.RegistroDTO;
import com.example.mr_backend_wh.security.AuthenticationService;
import com.example.mr_backend_wh.model.Usuario;
import com.example.mr_backend_wh.service.UsuarioService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

  private static final String PATH = "/";

  private final UsuarioService usuarioService;
  private final AuthenticationService authenticationService;

  @PostMapping("/registro")
  public Usuario registro(@RequestBody RegistroDTO registroDTO){
    // Usar el servicio de usuario que acepta RegistroDTO
    return usuarioService.registrarUsuario(registroDTO);
  }

  @PostMapping("/login")
  public ResponseEntity<AuthenticationResponseDTO> login(@RequestBody LoginDTO loginDTO, HttpServletResponse response){
    if(authenticationService.verifyPassword(loginDTO)){
      AuthenticationResponseDTO authResp = authenticationService.login(loginDTO);
      if(authResp != null && authResp.getToken() != null){
        // Crear cookie JWT (HttpOnly) usando ResponseCookie para incluir SameSite
        // Usar el mismo nombre que el template Google para que TokenAuthenticationFilter lo lea
        ResponseCookie cookie = ResponseCookie.from("X-Auth-Token", authResp.getToken())
                .httpOnly(true)
                .secure(false) // cambiar a true en producción
                .path(PATH)
                .sameSite("Lax")
                .maxAge(60 * 60 * 3)
                .build();

        response.setHeader("Set-Cookie", cookie.toString());

        // Devolver solo mensaje; frontend debe usar cookies
        return ResponseEntity.ok(AuthenticationResponseDTO.builder().message("Login success").build());
      }
    }

    return ResponseEntity.status(401).build();
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(HttpServletResponse response) {
    // Clear JWT cookie (HttpOnly)
    response.addHeader("Set-Cookie", buildClearCookie("X-Auth-Token", PATH, true));
    // Clear XSRF cookie (readable by JS)
    response.addHeader("Set-Cookie", buildClearCookie("XSRF-TOKEN", PATH, false));
    // opcional: limpiar cookie de refresh si existe
    response.addHeader("Set-Cookie", buildClearCookie("REFRESH", PATH, true));
  }

  private String buildClearCookie(String name, String path, boolean httpOnly) {
    StringBuilder sb = new StringBuilder();
    sb.append(name).append("=").append("; Max-Age=0; Path=").append(path);
    // En desarrollo no forzamos Secure para que funcione en HTTP local; usar Secure en producción
    sb.append("; SameSite=Lax");
    if (httpOnly) sb.append("; HttpOnly");
    return sb.toString();
  }

  @GetMapping("/credencialDisponible")
  public boolean credencialDisponible(@RequestParam String usuario){
    return usuarioService.credencialDisponible(usuario);
  }

}
