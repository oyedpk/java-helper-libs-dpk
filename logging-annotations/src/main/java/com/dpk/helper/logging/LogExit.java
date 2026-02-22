package com.dpk.helper.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Logs method exit with return value (or exception).
 *
 * <p>For void methods, only the exit event is logged without a return value.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogExit {

    /**
     * Log level. Defaults to DEBUG.
     */
    LogLevel level() default LogLevel.DEBUG;

    /**
     * Whether to include the return value in the log. Defaults to true.
     */
    boolean includeReturnValue() default true;

    /**
     * Whether to log on exception. Defaults to true (logged at WARN).
     */
    boolean logExceptions() default true;
}
