package com.pollnet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pollnet.invite")
public record InviteProperties(
        int quotaMonthly,
        int tokenTtlDays
) {}
