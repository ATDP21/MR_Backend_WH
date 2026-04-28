package com.example.mr_backend_wh.security.oauth;

import com.example.mr_backend_wh.model.Rol;
import com.example.mr_backend_wh.model.Usuario;
import com.example.mr_backend_wh.repository.UsuarioRepository;
import com.example.mr_backend_wh.repository.RolRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomOAuth2UserService(UsuarioRepository usuarioRepository, RolRepository rolRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);
        String email = oauthUser.getAttribute("email");
        if (email == null) return oauthUser;

        Optional<Usuario> existing = usuarioRepository.findTopByEmail(email);
        if (existing.isEmpty()) {
            Usuario nuevo = new Usuario();
            String base = email.split("@")[0];
            String username = ensureUniqueUsername(base);
            nuevo.setNomusuario(username);
            nuevo.setEmail(email);
            nuevo.setNomcompleto((String) oauthUser.getAttribute("name"));
            nuevo.setContrasenia(passwordEncoder.encode(UUID.randomUUID().toString()));
            Rol rol = rolRepository.findById(1).orElse(new Rol(1, "USER"));
            nuevo.setRolid(rol);
            usuarioRepository.save(nuevo);
        } else {
            // opcional: actualizar campos
            Usuario u = existing.get();
            String name = oauthUser.getAttribute("name");
            if (name != null && !name.equals(u.getNomcompleto())) {
                u.setNomcompleto(name);
                usuarioRepository.save(u);
            }
        }

        return oauthUser;
    }

    private String ensureUniqueUsername(String base) {
        String candidate = base.length() > 50 ? base.substring(0,50) : base;
        int suffix = 0;
        while (usuarioRepository.findTopByNomusuario(candidate).isPresent()) {
            suffix++;
            String s = base + suffix;
            candidate = s.length() > 50 ? s.substring(0,50) : s;
        }
        return candidate;
    }
}

