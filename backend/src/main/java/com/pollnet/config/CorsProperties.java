package com.pollnet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "pollnet.cors")
public record CorsProperties(List<String> allowedOriginPatterns) {
    public List<String> allowedOriginPatternsOrDefault() {
        return (allowedOriginPatterns == null || allowedOriginPatterns.isEmpty())
                ? List.of("http://localhost:*", "http://127.0.0.1:*")
                : allowedOriginPatterns;
    }
}
