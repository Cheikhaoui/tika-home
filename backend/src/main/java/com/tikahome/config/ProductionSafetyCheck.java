package com.tikahome.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Refuses to start in production with settings that are only acceptable locally: the development
 * JWT secret and admin password are public in the Git repository, and the session cookie must be Secure.
 */
@Component
@Profile("prod")
public class ProductionSafetyCheck implements ApplicationRunner {

    static final String DEV_MARKER = "local-dev";

    private final AdminProperties admin;
    private final NotificationProperties notifications;

    public ProductionSafetyCheck(AdminProperties admin, NotificationProperties notifications) {
        this.admin = admin;
        this.notifications = notifications;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> problems = problems(admin, notifications);
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Unsafe production configuration: " + String.join("; ", problems));
        }
    }

    static List<String> problems(AdminProperties admin, NotificationProperties notifications) {
        List<String> problems = new ArrayList<>();
        if (admin.jwtSecret() == null || admin.jwtSecret().contains(DEV_MARKER) || admin.jwtSecret().length() < 32) {
            problems.add("ADMIN_JWT_SECRET must be a random value of at least 32 characters (not the dev one)");
        }
        if (admin.bootstrapPassword() != null && admin.bootstrapPassword().contains(DEV_MARKER)) {
            problems.add("ADMIN_BOOTSTRAP_PASSWORD must not be the development password");
        }
        if (!admin.cookieSecure()) {
            problems.add("ADMIN_COOKIE_SECURE must be true (HTTPS only)");
        }
        if (notifications.siteUrl() == null || !notifications.siteUrl().startsWith("https://")
                && !notifications.siteUrl().startsWith("http://localhost")) {
            problems.add("SITE_URL must be an https:// URL");
        }
        return problems;
    }
}
