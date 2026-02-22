package com.dpk.helper.resilience.aop;

import com.dpk.helper.resilience.Backoff;
import com.dpk.helper.resilience.Retry;

import java.util.concurrent.atomic.AtomicInteger;

public class RetryTestService {

    private final AtomicInteger callCount = new AtomicInteger(0);
    private int failUntilAttempt = 0;

    public void setFailUntilAttempt(int n) {
        this.failUntilAttempt = n;
        this.callCount.set(0);
    }

    public int getCallCount() {
        return callCount.get();
    }

    public void resetCount() {
        callCount.set(0);
    }

    @Retry(maxAttempts = 3, backoff = @Backoff(delay = 10))
    public String retryableMethod() {
        int attempt = callCount.incrementAndGet();
        if (attempt <= failUntilAttempt) {
            throw new RuntimeException("attempt " + attempt + " failed");
        }
        return "success on attempt " + attempt;
    }

    @Retry(maxAttempts = 2, backoff = @Backoff(delay = 10))
    public String retryTwice() {
        int attempt = callCount.incrementAndGet();
        if (attempt <= failUntilAttempt) {
            throw new RuntimeException("attempt " + attempt + " failed");
        }
        return "success on attempt " + attempt;
    }

    @Retry(maxAttempts = 3, backoff = @Backoff(delay = 10, multiplier = 2))
    public String retryWithBackoff() {
        int attempt = callCount.incrementAndGet();
        if (attempt <= failUntilAttempt) {
            throw new RuntimeException("attempt " + attempt + " failed");
        }
        return "success";
    }

    @Retry(maxAttempts = 3, retryOn = IllegalStateException.class, backoff = @Backoff(delay = 10))
    public String retryOnSpecific() {
        int attempt = callCount.incrementAndGet();
        if (attempt <= failUntilAttempt) {
            throw new IllegalArgumentException("not retryable");
        }
        return "success";
    }

    @Retry(maxAttempts = 3, noRetryOn = IllegalArgumentException.class, backoff = @Backoff(delay = 10))
    public String noRetryOnSpecific() {
        int attempt = callCount.incrementAndGet();
        if (attempt <= failUntilAttempt) {
            throw new IllegalArgumentException("excluded");
        }
        return "success";
    }
}
