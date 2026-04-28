// language: java
package com.example.mr_backend_wh.security;

import com.example.mr_backend_wh.security.oauth.CustomAuthenticationSuccessHandler;
import com.example.mr_backend_wh.security.oauth.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JWTFilter jwtFilterChain;
  private final AuthenticationProvider authenticationProvider;
  private final CustomOAuth2UserService customOAuth2UserService;
  private final CustomAuthenticationSuccessHandler customAuthSuccessHandler;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringRequestMatchers(request -> {
                      String uri = request.getRequestURI();
                      if (uri == null) return false;
                      // Ignorar CSRF para endpoints de auth y para la API de guitarra (POST desde cliente/API)
                      return uri.contains("/auth") || uri.startsWith("/guitarra");
                    })
            )
            .authorizeHttpRequests(req ->
                    req
                            .requestMatchers(("/auth/**")).permitAll()
                            // permitir explícitamente login y endpoints OAuth para evitar que /login quede protegida
                            .requestMatchers("/login", "/oauth2/**", "/oauth2/authorization/**", "/registro").permitAll()
                            .requestMatchers("/guitarra/**").permitAll()
                            .requestMatchers(POST, "/guitarra/**").permitAll()
                            .requestMatchers(GET, "/wordle/**").permitAll()
                            .requestMatchers(POST, "/wordle/**").permitAll()
                            .requestMatchers(GET, "/usuarios/banear").hasAnyAuthority("true")
                            .requestMatchers(GET, "/publicacion/eliminarPublicacion").permitAll()
                            .requestMatchers(GET, "/publicacion/eliminarComentario").permitAll()
                            .requestMatchers(GET, "/publicacion/**").permitAll()
                            .requestMatchers(GET, "/chat/**").permitAll()
                            .requestMatchers(GET, "/perfil/**").permitAll()
                            .requestMatchers(GET, "/comentario/**").permitAll()
                            .requestMatchers("/mensajes/**").permitAll()
                            .requestMatchers("/chat/**").permitAll()
                            .requestMatchers(GET,"/publicacion/buscar").permitAll()
                            .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthenticationEntryPoint))
            // Desactivar formLogin por defecto para evitar redirecciones automáticas a /login
            .formLogin(form -> form.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .authenticationProvider(authenticationProvider)
            .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                    .successHandler(customAuthSuccessHandler)
            );

    http.addFilterBefore(jwtFilterChain, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    config.setAllowedOrigins(List.of("http://localhost:3000")); // frontend
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);

    return source;
  }
}
