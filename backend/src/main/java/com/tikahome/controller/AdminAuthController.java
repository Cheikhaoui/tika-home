package com.tikahome.controller;

import com.tikahome.config.AdminProperties;
import com.tikahome.dto.AdminMeResponse;
import com.tikahome.dto.LoginRequest;
import com.tikahome.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService authService;
    private final AdminProperties properties;

    public AdminAuthController(AdminAuthService authService, AdminProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @Operation(summary = "Log in: sets the HttpOnly session cookie. 401 wrong credentials, 429 temporarily locked.")
    @PostMapping("/login")
    public ResponseEntity<AdminMeResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = authService.login(request.email(), request.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(result.token(), properties.tokenTtl()).toString())
                .body(new AdminMeResponse(result.email(), result.expiresAt()));
    }

    @Operation(summary = "Log out: deletes the session cookie")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    @Operation(summary = "Who is logged in (401 if not)")
    @GetMapping("/me")
    public AdminMeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new AdminMeResponse(jwt.getClaimAsString("email"), jwt.getExpiresAt());
    }

    /**
     * HttpOnly: JavaScript can't read it (an XSS bug can't steal the session).
     * Secure: only sent over HTTPS (production). SameSite=Strict: never sent from other websites (CSRF).
     */
    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(properties.cookieName(), value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
