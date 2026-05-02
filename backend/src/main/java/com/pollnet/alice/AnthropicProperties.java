package com.pollnet.alice;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pollnet.anthropic")
public record AnthropicProperties(
        String apiKey,
        String model,
        Integer maxTokens
) {
    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
