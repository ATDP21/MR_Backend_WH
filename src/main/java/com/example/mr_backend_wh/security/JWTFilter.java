package com.example.mr_backend_wh.security;

import com.example.mr_backend_wh.model.Usuario;
import com.example.mr_backend_wh.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);

    private final JWTService jwtService;
    private final UsuarioService usuarioService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Permitir preflight CORS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Permitir endpoints de auth (login/register)
        if (request.getServletPath() != null && request.getServletPath().startsWith("/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = resolveToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = null;
        try {
            username = jwtService.extractUsername(token);
            logger.debug("JWT found in request. token(short)={} subject={}", token.length()>40? token.substring(0,40)+"...":token, username);
        } catch (Exception ex) {
            // token malformado / expirado / inválido -> no autenticar
            logger.debug("JWT parsing failed: {}", ex.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Usuario usuario = null;
            try {
                usuario = (Usuario) usuarioService.loadUserByUsername(username);
            } catch (UsernameNotFoundException unfe) {
                logger.info("JWT token refers to unknown user '{}', ignoring token", username);
                // optionally clear cookie here by setting expired Set-Cookie; continue filter chain unauthenticated
                filterChain.doFilter(request, response);
                return;
            } catch (Exception e) {
                logger.warn("Unexpected error while loading user from token: {}", e.getMessage());
                filterChain.doFilter(request, response);
                return;
            }

            try {
                if (jwtService.isTokenValid(token, usuario)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            usuario.getUsername(),
                            usuario.getPassword(),
                            usuario.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception ex) {
                // Validación fallida (por ejemplo token en blacklist) -> no autenticar
                logger.debug("JWT validation failed: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // Primero mirar cookie X-Auth-Token (usada por nuestro flujo cookie-only)
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("X-Auth-Token".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    return c.getValue();
                }
            }
        }
        // Fallback: Authorization header
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }
}
