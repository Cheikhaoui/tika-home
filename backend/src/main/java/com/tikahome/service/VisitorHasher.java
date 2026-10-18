package com.tikahome.service;

import com.tikahome.config.AssistantProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * Turns an IP address into an anonymous, stable visitor id: HMAC-SHA256 with a server-side secret.
 * A plain SHA-256 would not be enough: there are only ~4 billion IPv4 addresses, so anyone could hash
 * them all and reverse it. Without the secret key, the HMAC can't be reversed.
 */
@Component
public class VisitorHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public VisitorHasher(AssistantProperties properties) {
        this.key = new SecretKeySpec(properties.visitorHashSecret().getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String hash(String ipAddress) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(ipAddress.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }
}
