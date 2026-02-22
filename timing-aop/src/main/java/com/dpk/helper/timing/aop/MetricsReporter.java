package com.dpk.helper.timing.aop;

import java.util.Map;

/**
 * Pluggable interface for reporting method timing metrics.
 *
 * <p>Implement this interface to integrate with your preferred metrics
 * backend (Micrometer, StatsD, Prometheus, etc.).
 */
public interface MetricsReporter {

    /**
     * Reports a timing measurement.
     *
     * @param metricName   the metric name (e.g. {@code "MyService.process"})
     * @param durationNanos elapsed time in nanoseconds
     * @param tags         key-value tags associated with this measurement
     * @param exception    the exception thrown during execution, or {@code null} on success
     */
    void report(String metricName, long durationNanos, Map<String, String> tags, Throwable exception);
}
