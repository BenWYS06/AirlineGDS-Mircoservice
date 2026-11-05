package com.ben.services.config;

import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Resilience for every Feign call booking-service makes (DESIGN.md §1, build step 5).
 *
 * Order of defence per call:  retry (here) → Feign connect/read timeout → circuit breaker (Resilience4j)
 * - Timeouts (application.yaml) turn a hang into an error.
 * - Retry absorbs short blips (a pod restarting, a dropped connection). It is safe because every call on the
 *   booking path is idempotent (step 3: hold, payment initiate, seat confirm, release).
 * - The circuit breaker wraps the whole call: when it is open the call is rejected before it starts,
 *   so there is nothing to retry and the dependency is left alone to recover.
 */
@Configuration
public class FeignRetryConfig {

    // 3 attempts in total, waiting 200 ms then up to 1 s between them (a few seconds at most: the user is waiting)
    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(200, 1_000, 3);
    }

    /**
     * Feign retries only RetryableException. Connection errors and read timeouts already are one;
     * this also makes a 503 retryable. Any other status (409 seat taken, 400 bad input, ...) is a business
     * answer: it is never retried and keeps its own exception type, so fallbacks can still tell them apart.
     */
    @Bean
    public ErrorDecoder feignErrorDecoder() {
        ErrorDecoder defaultDecoder = new ErrorDecoder.Default();
        return (methodKey, response) -> {
            if (response.status() == 503) {
                return new RetryableException(response.status(), "Service unavailable: " + methodKey,
                        response.request().httpMethod(), (Long) null, response.request());
            }
            return defaultDecoder.decode(methodKey, response);
        };
    }
}
