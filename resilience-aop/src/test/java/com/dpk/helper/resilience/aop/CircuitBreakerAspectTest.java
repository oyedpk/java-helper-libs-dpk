package com.dpk.helper.resilience.aop;

import com.dpk.helper.resilience.CircuitBreakerOpenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(ResilienceTestConfig.class)
class CircuitBreakerAspectTest {

    @Autowired
    private CircuitBreakerTestService service;

    @Autowired
    private CircuitBreakerAspect circuitBreakerAspect;

    @BeforeEach
    void setUp() {
        service.setShouldFail(false);
        circuitBreakerAspect.clearAll();
    }

    @Test
    void closedCircuit_callsSucceed() {
        assertThat(service.protectedMethod()).isEqualTo("ok");
    }

    @Test
    void circuitOpens_afterFailureThreshold() {
        service.setShouldFail(true);

        // 3 failures to reach threshold
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.protectedMethod())
                    .isInstanceOf(RuntimeException.class);
        }

        // 4th call should get CircuitBreakerOpenException
        assertThatThrownBy(() -> service.protectedMethod())
                .isInstanceOf(CircuitBreakerOpenException.class)
                .hasMessageContaining("testCb");
    }

    @Test
    void circuitBreakerOpenException_hasCircuitName() {
        service.setShouldFail(true);

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.protectedMethod())
                    .isInstanceOf(RuntimeException.class);
        }

        try {
            service.protectedMethod();
        } catch (CircuitBreakerOpenException e) {
            assertThat(e.getCircuitName()).isEqualTo("testCb");
        }
    }

    @Test
    void circuitRecovers_afterResetTimeout() throws InterruptedException {
        service.setShouldFail(true);

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.protectedMethod())
                    .isInstanceOf(RuntimeException.class);
        }

        // Circuit is now OPEN, wait for reset timeout (500ms)
        Thread.sleep(600);

        // Now the circuit should be HALF_OPEN, allow one trial call
        service.setShouldFail(false);
        assertThat(service.protectedMethod()).isEqualTo("ok");

        // Circuit should be CLOSED again
        assertThat(service.protectedMethod()).isEqualTo("ok");
    }

    @Test
    void failOn_onlyCountsSpecificExceptions() {
        service.setShouldFail(true);

        // specificFailures uses failOn = IllegalStateException.class
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> service.specificFailures())
                    .isInstanceOf(IllegalStateException.class);
        }

        // Circuit should be OPEN now
        assertThatThrownBy(() -> service.specificFailures())
                .isInstanceOf(CircuitBreakerOpenException.class);
    }

    @Test
    void failOn_doesNotCountNonMatchingExceptions() {
        service.setShouldFail(true);

        // uncountedFailure throws IllegalArgumentException but failOn = IllegalStateException
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.uncountedFailure())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        // Circuit should still be CLOSED because failures weren't counted
        service.setShouldFail(false);
        assertThat(service.uncountedFailure()).isEqualTo("ok");
    }

    @Test
    void successResetsFailureCount() {
        service.setShouldFail(true);

        // 2 failures (threshold is 3)
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> service.protectedMethod())
                    .isInstanceOf(RuntimeException.class);
        }

        // One success resets counter
        service.setShouldFail(false);
        assertThat(service.protectedMethod()).isEqualTo("ok");

        // 2 more failures should NOT open the circuit (counter was reset)
        service.setShouldFail(true);
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> service.protectedMethod())
                    .isInstanceOf(RuntimeException.class);
        }

        service.setShouldFail(false);
        assertThat(service.protectedMethod()).isEqualTo("ok");
    }
}
