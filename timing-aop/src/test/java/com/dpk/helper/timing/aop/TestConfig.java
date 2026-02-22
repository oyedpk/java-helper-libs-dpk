package com.dpk.helper.timing.aop;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class TestConfig {

    @Bean
    public InMemoryMetricsReporter metricsReporter() {
        return new InMemoryMetricsReporter();
    }

    @Bean
    public TimedAspect timedAspect(InMemoryMetricsReporter reporter) {
        return new TimedAspect(reporter);
    }

    @Bean
    public TestService testService() {
        return new TestService();
    }

    @Bean
    public TimedClassService timedClassService() {
        return new TimedClassService();
    }
}
