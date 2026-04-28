package com.example.mr_backend_wh.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

@Component
public class CsrfFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(CsrfFilter.class);

    private static final String XSRF_COOKIE = "XSRF-TOKEN";
    private static final String XSRF_HEADER = "X-XSRF-TOKEN";
    private static final Set<String> SAFE_METHODS = new HashSet<>(Arrays.asList("GET", "HEAD", "OPTIONS"));

    private static final SecureRandom RNG = new SecureRandom();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (uri != null && uri.contains("/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        String cookieToken = readCookie(request, XSRF_COOKIE);

        if (SAFE_METHODS.contains(method)) {
            if (cookieToken == null) {
                String newToken = generateToken();
                Cookie cookie = new Cookie(XSRF_COOKIE, newToken);
                cookie.setPath("/");
                cookie.setHttpOnly(false);
                response.addCookie(cookie);
                response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
                logger.debug("CsrfFilter: issued {} cookie", XSRF_COOKIE);
            }
            filterChain.doFilter(request, response);
            return;
        }

        String headerToken = request.getHeader(XSRF_HEADER);
        if (cookieToken == null || headerToken == null || !cookieToken.equals(headerToken)) {
            logger.warn("CSRF validation failed for request {} {} - headerToken: {}, cookieToken: {}",
                    method, uri, headerToken, cookieToken);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF validation failed");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
