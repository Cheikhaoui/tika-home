package com.tikahome.config;

import com.tikahome.service.AdminAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * On startup, creates the owner's admin account from TIKAHOME_ADMIN_BOOTSTRAP_EMAIL / _PASSWORD if no admin
 * exists yet. No password ever lives in Git; once the account exists these variables can be removed.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminAuthService authService;
    private final AdminProperties properties;

    public AdminBootstrap(AdminAuthService authService, AdminProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.bootstrapEmail() == null || properties.bootstrapEmail().isBlank()) {
            return;
        }
        if (authService.createFirstAdmin(properties.bootstrapEmail(), properties.bootstrapPassword())) {
            log.info("Created the first admin account ({})", properties.bootstrapEmail());
        }
    }
}
