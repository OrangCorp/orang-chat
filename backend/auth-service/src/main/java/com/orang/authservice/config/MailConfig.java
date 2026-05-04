package com.orang.authservice.config;

import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

/**
 * Mail configuration to handle graceful email service failures.
 * Disables the aggressive mail health check that prevents app startup when
 * MailTrap is unreachable or has reached email limits.
 */
@Configuration
@Slf4j
public class MailConfig {

    /**
     * Custom mail health indicator that doesn't perform eager connection tests.
     * Email service failures should not prevent the application from starting.
     * Users can still use the app, but email-dependent features will fail gracefully.
     * 
     * This is disabled via management.health.mail.enabled=false in application.yaml
     * but we keep this bean as a reference for future health check implementations.
     */
    @Bean
    @ConditionalOnEnabledHealthIndicator("mail")
    public HealthIndicator mailHealthIndicator() {
        return () -> {
            // Simple health check that doesn't attempt to connect to the mail server
            // Actual email connectivity is tested only when emails are actually sent
            log.debug("Mail health check called - returning UP status (no actual connection test)");
            return Health.up()
                    .withDetail("message", "Mail service will be tested when emails are sent")
                    .build();
        };
    }
}
