package com.pollnet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pollnet.password-reset")
public record PasswordResetProperties(int tokenTtlHours) {}
