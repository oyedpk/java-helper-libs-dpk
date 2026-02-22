package com.dpk.helper.timing.aop;

import com.dpk.helper.timing.Tag;
import com.dpk.helper.timing.Timed;

/**
 * A simple service used for testing {@code @Timed} method-level annotation.
 */
public class TestService {

    @Timed
    public String defaultName() {
        return "ok";
    }

    @Timed("custom.metric")
    public String customName() {
        return "ok";
    }

    @Timed(tags = {@Tag(key = "env", value = "test"), @Tag(key = "region", value = "us-east")})
    public String withTags() {
        return "ok";
    }

    @Timed(dynamicTags = {"userId=#args[0]"})
    public String withDynamicTag(String userId) {
        return "hello " + userId;
    }

    @Timed(dynamicTags = {"name=#name", "count=#args.length"})
    public String withNamedParams(String name, int count) {
        return name + ":" + count;
    }

    @Timed(reportExceptions = true)
    public String failingMethod() {
        throw new IllegalStateException("boom");
    }

    @Timed(reportExceptions = false)
    public String failingNoReport() {
        throw new IllegalStateException("boom");
    }
}
