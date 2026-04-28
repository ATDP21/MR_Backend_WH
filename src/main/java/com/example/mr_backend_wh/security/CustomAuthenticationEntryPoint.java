// language: java
package com.example.mr_backend_wh.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String AJAX_HEADER = "X-Requested-With";
    private static final String AJAX_HEADER_VALUE = "XMLHttpRequest";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        String accept = request.getHeader("Accept");
        String xReq = request.getHeader(AJAX_HEADER);
        String uri = request.getRequestURI();
        String method = request.getMethod();

        boolean isAjax = (xReq != null && AJAX_HEADER_VALUE.equalsIgnoreCase(xReq))
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE))
                || (uri != null && (uri.startsWith("/api") || uri.startsWith("/usuario") || uri.startsWith("/mensajes")));

        boolean isSafeMethod = HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method) || HttpMethod.OPTIONS.matches(method);

        if (isAjax || !isSafeMethod) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            try (PrintWriter pw = response.getWriter()) {
                pw.write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
            }
            return;
        }

        // Redirigir al controlador /login sólo para peticiones de navegación seguras
        String loginPath = request.getContextPath() + "/login";
        response.sendRedirect(loginPath);
    }
}
