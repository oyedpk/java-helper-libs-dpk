package com.dpk.helper.timing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for automatic timing instrumentation.
 *
 * <p>When a method annotated with {@code @Timed} is invoked, the AOP aspect
 * records its execution duration and reports it via the configured
 * {@link com.dpk.helper.timing.aop.MetricsReporter}.
 *
 * <p>If both {@code @Timed} and {@code @TimedClass} are present, the
 * method-level {@code @Timed} takes precedence.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Timed {

    /**
     * Metric name. Defaults to {@code ClassName.methodName} if empty.
     */
    String value() default "";

    /**
     * Static key-value tags to attach to the metric.
     */
    Tag[] tags() default {};

    /**
     * SpEL expressions for dynamic tags resolved at runtime.
     * Format: {@code "key=expression"}, e.g. {@code "userId=#args[0].id"}.
     */
    String[] dynamicTags() default {};

    /**
     * Whether to report timing even when the method throws an exception.
     */
    boolean reportExceptions() default true;
}
