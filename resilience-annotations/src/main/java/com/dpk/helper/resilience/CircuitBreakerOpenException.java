package com.dpk.helper.resilience;

/**
 * Thrown when a method protected by {@link CircuitBreaker} is invoked
 * while the circuit is in the OPEN state.
 */
public class CircuitBreakerOpenException extends RuntimeException {

    private final String circuitName;

    public CircuitBreakerOpenException(String circuitName) {
        super("Circuit breaker '%s' is OPEN".formatted(circuitName));
        this.circuitName = circuitName;
    }

    public String getCircuitName() {
        return circuitName;
    }
}
