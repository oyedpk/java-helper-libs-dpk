package com.dpk.helper.timing.aop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(TestConfig.class)
class TimedClassTest {

    @Autowired
    private TimedClassService timedClassService;

    @Autowired
    private InMemoryMetricsReporter reporter;

    @BeforeEach
    void setUp() {
        reporter.clear();
    }

    @Test
    void timedClass_instrumentsPublicMethods() {
        timedClassService.processOrder("ORD-123");

        assertThat(reporter.getRecords()).hasSize(1);
        InMemoryMetricsReporter.TimingRecord record = reporter.getLastRecord();
        assertThat(record.metricName()).isEqualTo("service.processOrder");
        assertThat(record.tags()).containsEntry("component", "order");
        assertThat(record.durationNanos()).isPositive();
    }

    @Test
    void timedClass_instrumentsMultiplePublicMethods() {
        timedClassService.processOrder("ORD-1");
        timedClassService.calculateTotal(10, 20);

        assertThat(reporter.getRecords()).hasSize(2);
        assertThat(reporter.getRecords().get(0).metricName()).isEqualTo("service.processOrder");
        assertThat(reporter.getRecords().get(1).metricName()).isEqualTo("service.calculateTotal");
    }

    @Test
    void timedClass_classTags_areAppliedToAllMethods() {
        timedClassService.processOrder("ORD-1");
        timedClassService.calculateTotal(10, 20);

        for (InMemoryMetricsReporter.TimingRecord record : reporter.getRecords()) {
            assertThat(record.tags()).containsEntry("component", "order");
        }
    }

    @Test
    void timedClass_methodLevelTimedOverridesClassLevel() {
        timedClassService.overriddenMethod();

        assertThat(reporter.getRecords()).hasSize(1);
        InMemoryMetricsReporter.TimingRecord record = reporter.getLastRecord();
        // Should use method-level @Timed name, not class-level prefix
        assertThat(record.metricName()).isEqualTo("overridden.metric");
        // Method-level @Timed has no tags, so class-level tags should NOT be present
        assertThat(record.tags()).isEmpty();
    }
}
