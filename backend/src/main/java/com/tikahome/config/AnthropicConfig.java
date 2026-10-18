package com.tikahome.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Official Anthropic client. Credentials come from the environment (ANTHROPIC_API_KEY), never from config
 * files: the repository is public. Without a key the app still starts; the assistant then reports itself
 * unavailable (see AssistantService#isConfigured).
 */
@Configuration
public class AnthropicConfig {

    @Bean(destroyMethod = "close")
    AnthropicClient anthropicClient() {
        return AnthropicOkHttpClient.fromEnv();
    }
}
