package com.example.mr_backend_wh.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class CsrfFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(CsrfFilter.class);

    private static final Set<String> SAFE_METHODS = new HashSet<>(Arrays.asList("GET", "HEAD", "OPTIONS"));

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (uri != null && (uri.contains("/auth") || uri.startsWith("/api/webhook"))) {
            filterChain.doFilter(request, response);
            return;
        }

        String cookieToken = readCookie(request, CsrfTokenCookieUtil.XSRF_COOKIE_NAME);

        if (SAFE_METHODS.contains(method)) {
            if (cookieToken == null) {
                String newToken = CsrfTokenCookieUtil.generateToken();
                ResponseCookie cookie = CsrfTokenCookieUtil.buildXsrfCookie(newToken);
                response.addHeader("Set-Cookie", cookie.toString());
                response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
                logger.debug("CsrfFilter: issued {} cookie", CsrfTokenCookieUtil.XSRF_COOKIE_NAME);
            }
            filterChain.doFilter(request, response);
            return;
        }

        String headerToken = request.getHeader(CsrfTokenCookieUtil.XSRF_HEADER_NAME);
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
}
