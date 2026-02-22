package com.dpk.helper.resilience.aop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(ResilienceTestConfig.class)
class FallbackAspectTest {

    @Autowired
    private FallbackTestService service;

    @Test
    void fallbackWithThrowable_receivesExceptionAndArgs() {
        String result = service.methodWithFallback("test");
        assertThat(result).isEqualTo("fallback:test:primary failed");
    }

    @Test
    void simpleFallback_receivesOriginalArgs() {
        String result = service.methodWithSimpleFallback("test");
        assertThat(result).isEqualTo("simple-fallback:test");
    }

    @Test
    void selectiveFallback_doesNotApplyForNonMatchingException() {
        // applyOn = IllegalStateException, but method throws IllegalArgumentException
        assertThatThrownBy(() -> service.selectiveFallback("test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("not matched");
    }

    @Test
    void selectiveFallback_appliesForMatchingException() {
        String result = service.selectiveFallbackMatched("test");
        assertThat(result).isEqualTo("should-not-reach");
    }

    @Test
    void missingFallbackMethod_throwsIllegalState() {
        assertThatThrownBy(() -> service.missingFallback())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No suitable fallback method");
    }

    @Test
    void successfulMethod_doesNotInvokeFallback() {
        String result = service.successMethod("test");
        assertThat(result).isEqualTo("primary:test");
    }
}
