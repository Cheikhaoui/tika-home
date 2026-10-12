package com.tikahome.service;

import com.tikahome.config.AdminProperties;
import com.tikahome.entity.AdminUser;
import com.tikahome.exception.InvalidCredentialsException;
import com.tikahome.exception.TooManyAttemptsException;
import com.tikahome.repository.AdminUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AdminAuthService {

    private final AdminUserRepository admins;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final AdminProperties properties;
    private final Clock clock;
    /** Hash of a random password, compared when the email is unknown so both paths take the same time. */
    private final String dummyHash;

    public AdminAuthService(AdminUserRepository admins, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                            AdminProperties properties, Clock clock) {
        this.admins = admins;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("timing-equaliser-" + System.nanoTime());
    }

    /**
     * Checks the credentials and returns a signed JWT. Wrong passwords are counted per account;
     * after {@code maxFailedLogins} the account is locked for {@code lockDuration} (brute-force protection).
     * noRollbackFor: a failed attempt must still be saved even though we throw.
     */
    @Transactional(noRollbackFor = {InvalidCredentialsException.class, TooManyAttemptsException.class})
    public LoginResult login(String email, String password) {
        Instant now = clock.instant();
        Optional<AdminUser> found = admins.findByEmail(normalise(email));
        if (found.isEmpty()) {
            passwordEncoder.matches(password, dummyHash); // same cost as a real check: no account enumeration by timing
            throw new InvalidCredentialsException();
        }
        AdminUser admin = found.get();
        if (admin.isLocked(now)) {
            throw new TooManyAttemptsException(admin.getLockedUntil());
        }
        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            admin.recordFailedLogin(now, properties.maxFailedLogins(), properties.lockDuration());
            if (admin.isLocked(now)) {
                throw new TooManyAttemptsException(admin.getLockedUntil());
            }
            throw new InvalidCredentialsException();
        }
        admin.recordSuccessfulLogin(now);
        Instant expiresAt = now.plus(properties.tokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("tikahome")
                .subject(admin.getId().toString())
                .claim("email", admin.getEmail())
                .claim("roles", List.of("ADMIN"))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new LoginResult(token, admin.getEmail(), expiresAt);
    }

    /** Creates an admin if none exists (first start). Returns true if one was created. */
    @Transactional
    public boolean createFirstAdmin(String email, String password) {
        if (admins.count() > 0) {
            return false;
        }
        if (password == null || password.length() < 12) {
            throw new IllegalStateException("The first admin password must be at least 12 characters");
        }
        admins.save(new AdminUser(normalise(email), passwordEncoder.encode(password), clock.instant()));
        return true;
    }

    private static String normalise(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    public record LoginResult(String token, String email, Instant expiresAt) {
    }
}
