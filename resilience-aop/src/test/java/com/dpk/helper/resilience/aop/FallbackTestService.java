package com.dpk.helper.resilience.aop;

import com.dpk.helper.resilience.Fallback;

public class FallbackTestService {

    @Fallback(fallbackMethod = "fallbackWithThrowable")
    public String methodWithFallback(String input) {
        throw new RuntimeException("primary failed");
    }

    public String fallbackWithThrowable(String input, Throwable ex) {
        return "fallback:" + input + ":" + ex.getMessage();
    }

    @Fallback(fallbackMethod = "simpleFallback")
    public String methodWithSimpleFallback(String input) {
        throw new RuntimeException("primary failed");
    }

    public String simpleFallback(String input) {
        return "simple-fallback:" + input;
    }

    @Fallback(fallbackMethod = "noopFallback", applyOn = IllegalStateException.class)
    public String selectiveFallback(String input) {
        throw new IllegalArgumentException("not matched");
    }

    public String noopFallback(String input) {
        return "should-not-reach";
    }

    @Fallback(fallbackMethod = "noopFallback", applyOn = IllegalStateException.class)
    public String selectiveFallbackMatched(String input) {
        throw new IllegalStateException("matched");
    }

    @Fallback(fallbackMethod = "noSuchMethod")
    public String missingFallback() {
        throw new RuntimeException("primary failed");
    }

    @Fallback(fallbackMethod = "simpleFallbackNoThrow")
    public String successMethod(String input) {
        return "primary:" + input;
    }

    public String simpleFallbackNoThrow(String input) {
        return "fallback:" + input;
    }
}
