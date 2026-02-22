package com.dpk.helper.logging.aop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(LoggingTestConfig.class)
class MaskFieldTest {

    @Autowired
    private LoggingTestService service;

    @Test
    void maskedField_doesNotAffectReturnValue() {
        assertThat(service.withMaskedField("admin", "secret123")).isEqualTo("ok");
    }

    @Test
    void maskedField_defaultMask_isAppliedInArgString() throws Exception {
        LoggingAspect aspect = new LoggingAspect();
        Method method = LoggingTestService.class.getMethod("withMaskedField", String.class, String.class);
        String argString = aspect.buildArgString(method, new Object[]{"admin", "secret123"});

        assertThat(argString).contains("admin");
        assertThat(argString).contains("***");
        assertThat(argString).doesNotContain("secret123");
    }

    @Test
    void maskedField_customMask_isAppliedInArgString() throws Exception {
        LoggingAspect aspect = new LoggingAspect();
        Method method = LoggingTestService.class.getMethod("withCustomMask", String.class, String.class);
        String argString = aspect.buildArgString(method, new Object[]{"admin", "mytoken"});

        assertThat(argString).contains("admin");
        assertThat(argString).contains("[REDACTED]");
        assertThat(argString).doesNotContain("mytoken");
    }

    @Test
    void buildArgString_noArgs_returnsEmpty() throws Exception {
        LoggingAspect aspect = new LoggingAspect();
        Method method = LoggingTestService.class.getMethod("perfBelowThreshold");
        String argString = aspect.buildArgString(method, new Object[]{});

        assertThat(argString).isEmpty();
    }
}
