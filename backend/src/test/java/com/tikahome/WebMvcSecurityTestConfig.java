package com.tikahome;

import com.tikahome.config.AdminProperties;
import com.tikahome.config.SecurityConfig;
import com.tikahome.security.JwtConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/** The real security setup for @WebMvcTest slices (they don't load the whole application). */
@TestConfiguration
@Import({SecurityConfig.class, JwtConfig.class})
@EnableConfigurationProperties(AdminProperties.class)
public class WebMvcSecurityTestConfig {
}
