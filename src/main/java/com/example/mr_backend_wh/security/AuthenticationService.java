package com.example.mr_backend_wh.security;

import com.example.mr_backend_wh.DTO.AuthenticationResponseDTO;
import com.example.mr_backend_wh.DTO.LoginDTO;
import com.example.mr_backend_wh.DTO.UsuarioDTO;
import com.example.mr_backend_wh.converter.UsuarioMapper;
import com.example.mr_backend_wh.model.Usuario;
import com.example.mr_backend_wh.repository.UsuarioRepository;
import com.example.mr_backend_wh.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    @Autowired
    private final UsuarioService usuarioService;
    @Autowired
    private final UsuarioMapper usuarioMapper;
    @Autowired
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private final JWTService jwtService;
    @Autowired
    private final AuthenticationManager authenticationManager;
    @Autowired
    private UsuarioRepository usuarioRepository;


    public AuthenticationResponseDTO register(UsuarioDTO usuarioDTO) {
        usuarioDTO.setContraseña(passwordEncoder.encode(usuarioDTO.getContraseña()));
        UsuarioDTO dto = usuarioService.save(usuarioDTO);

        // Intentar recuperar la entidad persistida para generar el token con todos sus campos
        Usuario usuarioEntity = null;
        if (dto != null && dto.getId() != null) {
            usuarioEntity = usuarioRepository.findById(dto.getId()).orElse(null);
        }

        // Si no se recupera la entidad, usar el mapeo como fallback
        if (usuarioEntity == null) {
            usuarioEntity = usuarioMapper.toEntity(dto);
        }

        String token = jwtService.generateToken(usuarioEntity);
        return AuthenticationResponseDTO
                .builder()
                .token(token)
                .build();
    }

    public AuthenticationResponseDTO login(LoginDTO loginDTO) {
        // Buscar el usuario por email (LoginDTO contiene email)
        Usuario usuario = usuarioRepository.findTopByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new RuntimeException("❌ Usuario no encontrado"));

        // Generar token directamente desde la entidad recuperada para asegurar que rolid y demás campos estén presentes
        String token = jwtService.generateToken(usuario);
        return AuthenticationResponseDTO
                .builder()
                .token(token)
                .message("Login success")
                .build();
    }

    public boolean verifyPassword(LoginDTO loginDTO) {
        return usuarioService.existByCredentials(loginDTO.getEmail(), loginDTO.getPassword());

    }

}
