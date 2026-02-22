package com.dpk.helper.resilience.aop;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class ResilienceTestConfig {

    @Bean
    public RetryAspect retryAspect() {
        return new RetryAspect();
    }

    @Bean
    public CircuitBreakerAspect circuitBreakerAspect() {
        return new CircuitBreakerAspect();
    }

    @Bean
    public FallbackAspect fallbackAspect() {
        return new FallbackAspect();
    }

    @Bean
    public RetryTestService retryTestService() {
        return new RetryTestService();
    }

    @Bean
    public CircuitBreakerTestService circuitBreakerTestService() {
        return new CircuitBreakerTestService();
    }

    @Bean
    public FallbackTestService fallbackTestService() {
        return new FallbackTestService();
    }
}
