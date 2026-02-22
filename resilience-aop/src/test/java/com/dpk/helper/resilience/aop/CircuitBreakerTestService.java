package com.dpk.helper.resilience.aop;

import com.dpk.helper.resilience.CircuitBreaker;

import java.util.concurrent.atomic.AtomicBoolean;

public class CircuitBreakerTestService {

    private final AtomicBoolean shouldFail = new AtomicBoolean(false);

    public void setShouldFail(boolean fail) {
        shouldFail.set(fail);
    }

    @CircuitBreaker(name = "testCb", failureThreshold = 3, resetTimeoutMs = 500)
    public String protectedMethod() {
        if (shouldFail.get()) {
            throw new RuntimeException("service down");
        }
        return "ok";
    }

    @CircuitBreaker(name = "specificCb", failureThreshold = 2, failOn = IllegalStateException.class)
    public String specificFailures() {
        if (shouldFail.get()) {
            throw new IllegalStateException("counted failure");
        }
        return "ok";
    }

    @CircuitBreaker(name = "uncountedCb", failureThreshold = 2, failOn = IllegalStateException.class)
    public String uncountedFailure() {
        if (shouldFail.get()) {
            throw new IllegalArgumentException("not counted");
        }
        return "ok";
    }
}
