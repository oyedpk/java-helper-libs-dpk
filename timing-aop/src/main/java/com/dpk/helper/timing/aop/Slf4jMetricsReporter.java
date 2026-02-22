package com.dpk.helper.timing.aop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Default {@link MetricsReporter} that logs timing information via SLF4J.
 */
public class Slf4jMetricsReporter implements MetricsReporter {

    private static final Logger log = LoggerFactory.getLogger(Slf4jMetricsReporter.class);

    @Override
    public void report(String metricName, long durationNanos, Map<String, String> tags, Throwable exception) {
        String durationMs = String.format("%.2f", durationNanos / 1_000_000.0);

        if (exception != null) {
            log.warn("TIMED [{}] failed in {}ms tags={} exception={}",
                    metricName, durationMs, tags, exception.getClass().getName());
        } else {
            log.info("TIMED [{}] completed in {}ms tags={}",
                    metricName, durationMs, tags);
        }
    }
}
