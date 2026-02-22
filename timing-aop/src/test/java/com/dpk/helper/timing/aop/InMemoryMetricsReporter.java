package com.dpk.helper.timing.aop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * In-memory {@link MetricsReporter} for testing. Captures all reports
 * so tests can assert on metric names, durations, tags, and exceptions.
 */
public class InMemoryMetricsReporter implements MetricsReporter {

    private final List<TimingRecord> records = new ArrayList<>();

    @Override
    public void report(String metricName, long durationNanos, Map<String, String> tags, Throwable exception) {
        records.add(new TimingRecord(metricName, durationNanos, Map.copyOf(tags), exception));
    }

    public List<TimingRecord> getRecords() {
        return Collections.unmodifiableList(records);
    }

    public TimingRecord getLastRecord() {
        if (records.isEmpty()) {
            throw new IllegalStateException("No records captured");
        }
        return records.get(records.size() - 1);
    }

    public void clear() {
        records.clear();
    }

    public record TimingRecord(
            String metricName,
            long durationNanos,
            Map<String, String> tags,
            Throwable exception
    ) {}
}
