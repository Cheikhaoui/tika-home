package com.tikahome.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.tikahome.config.AdminProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * JWTs signed with HMAC-SHA256 (HS256): the backend both issues and verifies them, so one shared
 * secret is enough (RS256 with key pairs is for when another service must verify our tokens).
 */
@Configuration
public class JwtConfig {

    private final SecretKey key;

    public JwtConfig(AdminProperties properties) {
        String secret = properties.jwtSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("tikahome.admin.jwt-secret must be at least 32 characters (256 bits)");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** BCrypt by default; the "{bcrypt}" prefix in stored hashes allows upgrading the algorithm later. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
