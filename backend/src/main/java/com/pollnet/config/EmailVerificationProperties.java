package com.pollnet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pollnet.email-verification")
public record EmailVerificationProperties(int tokenTtlHours) {}
