package com.pollnet.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        InviteProperties.class,
        MailProperties.class,
        EmailVerificationProperties.class,
        PasswordResetProperties.class,
        CorsProperties.class,
        com.pollnet.media.MediaProperties.class,
        com.pollnet.messaging.MessagingProperties.class,
        com.pollnet.alice.AnthropicProperties.class
})
public class AppPropertiesConfig {
}
