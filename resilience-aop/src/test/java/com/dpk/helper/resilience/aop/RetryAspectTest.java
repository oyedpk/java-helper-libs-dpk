package com.dpk.helper.resilience.aop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(ResilienceTestConfig.class)
class RetryAspectTest {

    @Autowired
    private RetryTestService service;

    @BeforeEach
    void setUp() {
        service.resetCount();
    }

    @Test
    void succeedsOnFirstAttempt() {
        service.setFailUntilAttempt(0);
        String result = service.retryableMethod();

        assertThat(result).isEqualTo("success on attempt 1");
        assertThat(service.getCallCount()).isEqualTo(1);
    }

    @Test
    void retriesAndSucceedsOnSecondAttempt() {
        service.setFailUntilAttempt(1);
        String result = service.retryableMethod();

        assertThat(result).isEqualTo("success on attempt 2");
        assertThat(service.getCallCount()).isEqualTo(2);
    }

    @Test
    void retriesAndSucceedsOnThirdAttempt() {
        service.setFailUntilAttempt(2);
        String result = service.retryableMethod();

        assertThat(result).isEqualTo("success on attempt 3");
        assertThat(service.getCallCount()).isEqualTo(3);
    }

    @Test
    void exhaustsAllAttemptsAndThrows() {
        service.setFailUntilAttempt(10);
        assertThatThrownBy(() -> service.retryableMethod())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("attempt 3 failed");

        assertThat(service.getCallCount()).isEqualTo(3);
    }

    @Test
    void maxAttemptsTwoExhausted() {
        service.setFailUntilAttempt(10);
        assertThatThrownBy(() -> service.retryTwice())
                .isInstanceOf(RuntimeException.class);

        assertThat(service.getCallCount()).isEqualTo(2);
    }

    @Test
    void retryOn_doesNotRetryNonMatchingException() {
        service.setFailUntilAttempt(1);
        assertThatThrownBy(() -> service.retryOnSpecific())
                .isInstanceOf(IllegalArgumentException.class);

        // Should not retry because IllegalArgumentException != IllegalStateException
        assertThat(service.getCallCount()).isEqualTo(1);
    }

    @Test
    void noRetryOn_skipsExcludedException() {
        service.setFailUntilAttempt(1);
        assertThatThrownBy(() -> service.noRetryOnSpecific())
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(service.getCallCount()).isEqualTo(1);
    }

    @Test
    void computeNextDelay_withMultiplier() {
        assertThat(RetryAspect.computeNextDelay(100, 2.0, 0)).isEqualTo(200);
        assertThat(RetryAspect.computeNextDelay(200, 2.0, 0)).isEqualTo(400);
    }

    @Test
    void computeNextDelay_cappedByMaxDelay() {
        assertThat(RetryAspect.computeNextDelay(500, 2.0, 800)).isEqualTo(800);
        assertThat(RetryAspect.computeNextDelay(100, 2.0, 150)).isEqualTo(150);
    }

    @Test
    void computeNextDelay_multiplierOfOne() {
        assertThat(RetryAspect.computeNextDelay(100, 1.0, 0)).isEqualTo(100);
    }
}
