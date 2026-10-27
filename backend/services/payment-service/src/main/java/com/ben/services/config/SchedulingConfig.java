package com.ben.services.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on the @Scheduled jobs (outbox relay, booking expiry job). On by default; integration tests set
 * app.scheduling.enabled=false and call the jobs directly, so a background run can't interfere with a test.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
