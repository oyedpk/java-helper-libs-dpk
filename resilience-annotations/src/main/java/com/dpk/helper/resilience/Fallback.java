package com.dpk.helper.resilience;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a fallback method to invoke when the annotated method throws an exception.
 *
 * <p>The fallback method must:
 * <ul>
 *   <li>Be in the same class as the annotated method</li>
 *   <li>Have the same return type</li>
 *   <li>Accept either the same parameters as the original method,
 *       or the same parameters plus a trailing {@link Throwable} parameter</li>
 * </ul>
 *
 * <pre>{@code
 * @Fallback(fallbackMethod = "fallbackHandler")
 * public String callService(String id) { ... }
 *
 * public String fallbackHandler(String id, Throwable ex) {
 *     return "default";
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Fallback {

    /**
     * Name of the fallback method in the same class.
     */
    String fallbackMethod();

    /**
     * Exception types that trigger the fallback. Empty means all exceptions.
     */
    Class<? extends Throwable>[] applyOn() default {};
}
