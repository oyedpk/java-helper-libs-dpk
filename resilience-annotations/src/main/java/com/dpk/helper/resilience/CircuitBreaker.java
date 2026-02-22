package com.dpk.helper.resilience;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applies a circuit breaker pattern to the annotated method.
 *
 * <p>The circuit breaker tracks failures and opens when the failure threshold
 * is reached, causing subsequent calls to fail immediately with
 * {@link CircuitBreakerOpenException} until the reset timeout expires.
 *
 * <p>States:
 * <ul>
 *   <li><b>CLOSED</b> — normal operation, failures are counted</li>
 *   <li><b>OPEN</b> — calls fail immediately without executing the method</li>
 *   <li><b>HALF_OPEN</b> — after reset timeout, one trial call is allowed through</li>
 * </ul>
 *
 * <pre>{@code
 * @CircuitBreaker(failureThreshold = 5, resetTimeoutMs = 30000)
 * public String callExternalService() { ... }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CircuitBreaker {

    /**
     * Name of this circuit breaker instance. Methods with the same name
     * share circuit breaker state. Defaults to {@code ClassName.methodName}.
     */
    String name() default "";

    /**
     * Number of consecutive failures before the circuit opens.
     */
    int failureThreshold() default 5;

    /**
     * Time in milliseconds before the circuit transitions from OPEN to HALF_OPEN.
     */
    long resetTimeoutMs() default 30_000;

    /**
     * Exception types that count as failures. Empty means all exceptions.
     */
    Class<? extends Throwable>[] failOn() default {};
}
