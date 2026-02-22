package com.dpk.helper.logging.aop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(LoggingTestConfig.class)
class LogEntryExitTest {

    @Autowired
    private LoggingTestService service;

    @Test
    void logEntry_doesNotAffectReturnValue() {
        assertThat(service.entryOnly("Alice")).isEqualTo("hello Alice");
    }

    @Test
    void logExit_doesNotAffectReturnValue() {
        assertThat(service.exitOnly("Bob")).isEqualTo("goodbye Bob");
    }

    @Test
    void logEntryAndExit_doesNotAffectReturnValue() {
        assertThat(service.entryAndExit("Charlie")).isEqualTo("hi Charlie");
    }

    @Test
    void logEntry_worksWithVoidMethods() {
        service.entryVoid("test");
        // No exception means success
    }

    @Test
    void logExit_excludesReturnValueWhenConfigured() {
        assertThat(service.exitNoReturn("test")).isEqualTo("secret test");
    }

    @Test
    void logEntryExit_exceptionStillPropagates() {
        assertThatThrownBy(() -> service.failingEntryExit())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }

    @Test
    void logExit_silentExceptionStillPropagates() {
        assertThatThrownBy(() -> service.failingSilent())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }

    @Test
    void logEntry_customMessageDoesNotAffectReturn() {
        assertThat(service.entryCustomMessage("Dave")).isEqualTo("Dave");
    }
}
