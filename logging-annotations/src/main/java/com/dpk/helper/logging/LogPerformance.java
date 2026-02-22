package com.dpk.helper.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Combined annotation that logs method entry, exit, and execution time
 * in a single structured log line.
 *
 * <p>Equivalent to applying {@link LogEntry} + {@link LogExit} + timing,
 * but produces a single consolidated log message on exit.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogPerformance {

    /**
     * Log level. Defaults to INFO.
     */
    LogLevel level() default LogLevel.INFO;

    /**
     * Whether to include method parameters in the log. Defaults to true.
     */
    boolean includeArgs() default true;

    /**
     * Whether to include the return value in the log. Defaults to false.
     */
    boolean includeReturnValue() default false;

    /**
     * Duration threshold in milliseconds. Only log if execution exceeds this.
     * A value of 0 means always log. Defaults to 0.
     */
    long thresholdMs() default 0;
}
