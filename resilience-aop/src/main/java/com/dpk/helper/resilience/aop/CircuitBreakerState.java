package com.dpk.helper.resilience.aop;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe state holder for a single circuit breaker instance.
 */
class CircuitBreakerState {

    enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final long resetTimeoutMs;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong openedAt = new AtomicLong(0);

    CircuitBreakerState(int failureThreshold, long resetTimeoutMs) {
        this.failureThreshold = failureThreshold;
        this.resetTimeoutMs = resetTimeoutMs;
    }

    State getState() {
        if (state.get() == State.OPEN) {
            if (System.currentTimeMillis() - openedAt.get() >= resetTimeoutMs) {
                state.compareAndSet(State.OPEN, State.HALF_OPEN);
            }
        }
        return state.get();
    }

    void recordSuccess() {
        failureCount.set(0);
        state.set(State.CLOSED);
    }

    void recordFailure() {
        int count = failureCount.incrementAndGet();
        if (count >= failureThreshold) {
            state.set(State.OPEN);
            openedAt.set(System.currentTimeMillis());
        }
    }

    // Visible for testing
    void reset() {
        state.set(State.CLOSED);
        failureCount.set(0);
        openedAt.set(0);
    }

    // Visible for testing
    void forceOpen() {
        state.set(State.OPEN);
        openedAt.set(System.currentTimeMillis());
    }

    int getFailureCount() {
        return failureCount.get();
    }
}
