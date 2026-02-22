package com.dpk.helper.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Logs method entry with parameter names and values.
 *
 * <p>Parameters annotated with {@link MaskField} will have their values masked.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogEntry {

    /**
     * Log level. Defaults to DEBUG.
     */
    LogLevel level() default LogLevel.DEBUG;

    /**
     * Custom message prefix. Empty uses the default format.
     */
    String message() default "";
}
