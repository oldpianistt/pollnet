package com.pollnet.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pollnet.messaging")
public record MessagingProperties(String secret) {}
