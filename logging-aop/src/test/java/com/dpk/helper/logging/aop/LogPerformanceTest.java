package com.dpk.helper.logging.aop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(LoggingTestConfig.class)
class LogPerformanceTest {

    @Autowired
    private LoggingTestService service;

    @Test
    void logPerformance_basicDoesNotAffectReturn() {
        assertThat(service.perfBasic("test")).isEqualTo("done:test");
    }

    @Test
    void logPerformance_withReturnValueDoesNotAffectReturn() {
        assertThat(service.perfWithReturn("test")).isEqualTo("result:test");
    }

    @Test
    void logPerformance_noArgsDoesNotAffectReturn() {
        assertThat(service.perfNoArgs("test")).isEqualTo("ok");
    }

    @Test
    void logPerformance_belowThresholdStillReturns() {
        assertThat(service.perfBelowThreshold()).isEqualTo("fast");
    }

    @Test
    void logPerformance_exceptionStillPropagates() {
        assertThatThrownBy(() -> service.perfFailing())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("perf-boom");
    }
}
