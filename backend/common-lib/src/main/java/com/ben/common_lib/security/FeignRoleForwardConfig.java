package com.ben.common_lib.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Only active in services that have Feign on the classpath.
@Configuration
@ConditionalOnClass(name = "feign.RequestInterceptor")
public class FeignRoleForwardConfig {

    @Bean
    public FeignRoleForwardInterceptor feignRoleForwardInterceptor() {
        return new FeignRoleForwardInterceptor();
    }
}
