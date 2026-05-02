package com.pollnet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pollnet.mail")
public record MailProperties(
        String from,
        boolean enabled,
        String publicBaseUrl
) {}
