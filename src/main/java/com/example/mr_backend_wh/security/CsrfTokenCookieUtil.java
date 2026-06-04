package com.example.mr_backend_wh.security;

import org.springframework.http.ResponseCookie;

import java.security.SecureRandom;
import java.util.Base64;

public final class CsrfTokenCookieUtil {

    public static final String XSRF_COOKIE_NAME = "XSRF-TOKEN";
    public static final String XSRF_HEADER_NAME = "X-XSRF-TOKEN";
    public static final String COOKIE_PATH = "/";

    private static final SecureRandom RNG = new SecureRandom();

    private CsrfTokenCookieUtil() {
    }

    public static String generateToken() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static ResponseCookie buildXsrfCookie(String token) {
        return ResponseCookie.from(XSRF_COOKIE_NAME, token)
                .httpOnly(false)
                .secure(false)
                .path(COOKIE_PATH)
                .sameSite("Lax")
                .build();
    }
}
