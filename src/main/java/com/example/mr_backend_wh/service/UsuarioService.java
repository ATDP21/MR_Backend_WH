package com.example.mr_backend_wh.service;

import com.example.mr_backend_wh.DTO.RegistroDTO;
import com.example.mr_backend_wh.DTO.UsuarioDTO;
import com.example.mr_backend_wh.DTO.UsuarioSecureDTO;
import com.example.mr_backend_wh.converter.UsuarioMapper;
import com.example.mr_backend_wh.model.Rol;
import com.example.mr_backend_wh.model.Usuario;
import com.example.mr_backend_wh.repository.UsuarioRepository;
import com.example.mr_backend_wh.security.JWTService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Service
@AllArgsConstructor
public class UsuarioService implements UserDetailsService {
    private final JWTService jwtService;
    private UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    private final UsuarioMapper usuarioMapper;


    public Usuario registrarUsuario(RegistroDTO dto){
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNomusuario(dto.getNombreUsuario());
        nuevoUsuario.setNomcompleto(dto.getNombreCompleto());
        nuevoUsuario.setContrasenia(passwordEncoder.encode(dto.getPassword()));
        nuevoUsuario.setEmail(dto.getEmail());
        nuevoUsuario.setRolid(new Rol(1, "USER"));

        return usuarioRepository.save(nuevoUsuario);
    }
    public UsuarioDTO save(UsuarioDTO usuarioDTO){
        return usuarioMapper.toDTO(usuarioRepository.save(usuarioMapper.toEntity(usuarioDTO)));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Intentar buscar por nomusuario -> email -> nombre (consulta nativa)
        return usuarioRepository.findTopByNomusuario(username)
                .or(() -> usuarioRepository.findTopByEmail(username))
                .or(() -> usuarioRepository.findTopByNombre(username))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
    public UsuarioDTO getByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findTopByNomusuario(username).orElse(null);

        if (usuario!=null){
            return usuarioMapper.toDTO(usuario);
        }else{
            throw  new UsernameNotFoundException("Usuario no encontrado");
        }

    }

    public UsuarioDTO getByEmail(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findTopByEmail(email).orElse(null);

        if (usuario!=null){
            return usuarioMapper.toDTO(usuario);
        }else{
            throw  new UsernameNotFoundException("Usuario no encontrado");
        }

    }
    @Transactional
    public UsuarioSecureDTO obtenerPerfilUsuarioLoggeado() {
        // Intentar obtener el username del SecurityContext (establecido por JWTFilter)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = null;

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                username = (String) principal;
            }
        }

        // Si no hay authentication, caer al método anterior que lee header o cookie
        if (username == null) {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String authHeader = request.getHeader("Authorization");

            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            } else if (request.getCookies() != null) {
                for (Cookie c : request.getCookies()) {
                    if ("X-Auth-Token".equals(c.getName()) || "JWT".equals(c.getName())) {
                        token = c.getValue();
                        break;
                    }
                }
            }

            if (token == null) {
                throw new RuntimeException("Token JWT no presente o mal formado");
            }

            username = jwtService.extractTokenData(token).getUsername();
        }

        System.out.println("🔹 Usuario autenticado (resolved): " + username);

        // Buscar usuario por nombre en la base de datos
        Usuario usuario = usuarioRepository.findTopByNomusuario(username)
                .orElseThrow(() -> new RuntimeException("❌ Usuario no encontrado"));

        System.out.println("✅ Usuario encontrado: " + usuario.getNomusuario());

        // Devolver datos no sensibles
        return new UsuarioSecureDTO(
                usuario.getNomusuario(),
                usuario.getEmail(),
                usuario.getNomcompleto(),
                isAdmin(usuario)
        );
    }
    private Boolean isAdmin(Usuario usuario) {
        if (usuario.getRolid().getId() == 1) {
            return false; // Usuario normal
        } else if (usuario.getRolid().getId() == 2) {
            return true;
        }
        throw new UsernameNotFoundException("Rol desconocido para el usuario");
    }
    public Boolean existByCredentials(String email, String password){
        Usuario usuario = usuarioRepository.findTopByEmail(email).orElse(null);
        return usuario != null  && passwordEncoder.matches(password,usuario.getContrasenia());
    }

    public boolean credencialDisponible(String nombreUsuario) {

        Usuario usuario = usuarioRepository.findTopByNomusuario(nombreUsuario).orElse(null);

        if (usuario == null){
            return true;
        }
        return false;
    }
}
