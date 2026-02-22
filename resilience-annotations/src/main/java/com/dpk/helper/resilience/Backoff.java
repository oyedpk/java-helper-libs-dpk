package com.dpk.helper.resilience;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures backoff strategy for {@link Retry}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Backoff {

    /**
     * Initial delay in milliseconds before the first retry.
     */
    long delay() default 100;

    /**
     * Multiplier applied to the delay after each retry.
     * For example, with delay=100 and multiplier=2:
     * retry 1 waits 100ms, retry 2 waits 200ms, retry 3 waits 400ms.
     */
    double multiplier() default 1.0;

    /**
     * Maximum delay in milliseconds. Caps the backoff to prevent excessively long waits.
     * A value of 0 means no cap. Defaults to 0.
     */
    long maxDelay() default 0;
}
