package com.dpk.helper.timing.aop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(TestConfig.class)
class DynamicTagResolutionTest {

    @Autowired
    private TestService testService;

    @Autowired
    private InMemoryMetricsReporter reporter;

    @BeforeEach
    void setUp() {
        reporter.clear();
    }

    @Test
    void dynamicTag_resolvesArgsExpression() {
        testService.withDynamicTag("user-42");

        InMemoryMetricsReporter.TimingRecord record = reporter.getLastRecord();
        assertThat(record.tags()).containsEntry("userId", "user-42");
    }

    @Test
    void dynamicTag_resolvesNamedParameters() {
        testService.withNamedParams("Alice", 5);

        InMemoryMetricsReporter.TimingRecord record = reporter.getLastRecord();
        assertThat(record.tags())
                .containsEntry("name", "Alice")
                .containsEntry("count", "2");
    }
}
