package com.dpk.helper.timing.aop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(TestConfig.class)
class TimedAspectTest {

    @Autowired
    private TestService testService;

    @Autowired
    private InMemoryMetricsReporter reporter;

    @BeforeEach
    void setUp() {
        reporter.clear();
    }

    @Test
    void defaultMetricName_usesClassAndMethodName() {
        testService.defaultName();

        assertThat(reporter.getRecords()).hasSize(1);
        InMemoryMetricsReporter.TimingRecord record = reporter.getLastRecord();
        assertThat(record.metricName()).isEqualTo("TestService.defaultName");
        assertThat(record.durationNanos()).isPositive();
        assertThat(record.exception()).isNull();
    }

    @Test
    void customMetricName_usesExplicitValue() {
        testService.customName();

        assertThat(reporter.getRecords()).hasSize(1);
        assertThat(reporter.getLastRecord().metricName()).isEqualTo("custom.metric");
    }

    @Test
    void staticTags_areCollectedCorrectly() {
        testService.withTags();

        InMemoryMetricsReporter.TimingRecord record = reporter.getLastRecord();
        assertThat(record.tags())
                .containsEntry("env", "test")
                .containsEntry("region", "us-east")
                .hasSize(2);
    }

    @Test
    void exception_isReportedWhenReportExceptionsIsTrue() {
        assertThatThrownBy(() -> testService.failingMethod())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        assertThat(reporter.getRecords()).hasSize(1);
        InMemoryMetricsReporter.TimingRecord record = reporter.getLastRecord();
        assertThat(record.exception()).isInstanceOf(IllegalStateException.class);
        assertThat(record.durationNanos()).isPositive();
    }

    @Test
    void exception_isNotReportedWhenReportExceptionsIsFalse() {
        assertThatThrownBy(() -> testService.failingNoReport())
                .isInstanceOf(IllegalStateException.class);

        assertThat(reporter.getRecords()).isEmpty();
    }

    @Test
    void durationNanos_isReasonable() {
        testService.defaultName();

        InMemoryMetricsReporter.TimingRecord record = reporter.getLastRecord();
        // Duration should be positive and less than 1 second for a trivial method
        assertThat(record.durationNanos()).isBetween(0L, 1_000_000_000L);
    }
}
