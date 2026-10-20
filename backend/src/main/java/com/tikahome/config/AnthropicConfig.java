package com.tikahome.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Official Anthropic client. The key (ANTHROPIC_API_KEY) comes from the environment or the git-ignored
 * local .env file, never from a committed config file: the repository is public. Without a key the app still starts; the assistant then reports itself
 * unavailable (see AssistantService#isConfigured).
 */
@Configuration
public class AnthropicConfig {

    @Bean(destroyMethod = "close")
    AnthropicClient anthropicClient(@Value("${ANTHROPIC_API_KEY:}") String apiKey) {
        AnthropicOkHttpClient.Builder builder = AnthropicOkHttpClient.builder().fromEnv();
        if (!apiKey.isBlank()) {
            builder.apiKey(apiKey); // also found in .env, which the SDK alone would not read
        }
        return builder.build();
    }

    /**
     * Streams assistant answers to the browser. Virtual threads: each answer mostly waits on the network
     * (Claude, then the guest's connection), so cheap threads are the right tool. On shutdown, close()
     * waits for the answers in progress.
     */
    @Bean(destroyMethod = "close")
    ExecutorService assistantExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
