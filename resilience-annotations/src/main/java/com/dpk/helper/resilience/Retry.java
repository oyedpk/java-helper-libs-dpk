package com.dpk.helper.resilience;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Retries a method invocation on failure.
 *
 * <pre>{@code
 * @Retry(maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
 * public String callExternalService() { ... }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Retry {

    /**
     * Maximum number of attempts (including the initial call).
     * For example, {@code maxAttempts = 3} means 1 initial call + 2 retries.
     */
    int maxAttempts() default 3;

    /**
     * Backoff configuration between retries.
     */
    Backoff backoff() default @Backoff;

    /**
     * Exception types that trigger a retry. Empty means all exceptions.
     */
    Class<? extends Throwable>[] retryOn() default {};

    /**
     * Exception types that should NOT trigger a retry (takes precedence over {@code retryOn}).
     */
    Class<? extends Throwable>[] noRetryOn() default {};
}
