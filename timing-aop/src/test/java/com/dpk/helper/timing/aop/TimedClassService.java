package com.dpk.helper.timing.aop;

import com.dpk.helper.timing.Tag;
import com.dpk.helper.timing.Timed;
import com.dpk.helper.timing.TimedClass;

/**
 * A service annotated with {@code @TimedClass} for testing class-level instrumentation.
 */
@TimedClass(prefix = "service", tags = {@Tag(key = "component", value = "order")})
public class TimedClassService {

    public String processOrder(String orderId) {
        return "processed:" + orderId;
    }

    public int calculateTotal(int a, int b) {
        return a + b;
    }

    /**
     * Method-level {@code @Timed} should override class-level {@code @TimedClass}.
     */
    @Timed("overridden.metric")
    public String overriddenMethod() {
        return "overridden";
    }

    // Non-public method — should NOT be instrumented by @TimedClass
    String packagePrivateMethod() {
        return "hidden";
    }
}
