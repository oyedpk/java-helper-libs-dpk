package com.dpk.helper.timing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks all public methods of a class for automatic timing instrumentation.
 *
 * <p>If a method within the annotated class also has {@code @Timed},
 * the method-level annotation takes precedence.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TimedClass {

    /**
     * Prefix for all metric names in this class.
     * The final metric name is {@code prefix.methodName} (or just {@code methodName} if empty).
     */
    String prefix() default "";

    /**
     * Static key-value tags applied to all methods in this class.
     */
    Tag[] tags() default {};
}
