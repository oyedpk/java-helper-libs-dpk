package com.dpk.helper.logging.aop;

import com.dpk.helper.logging.LogEntry;
import com.dpk.helper.logging.LogExit;
import com.dpk.helper.logging.LogLevel;
import com.dpk.helper.logging.LogPerformance;
import com.dpk.helper.logging.MaskField;

public class LoggingTestService {

    @LogEntry
    public String entryOnly(String name) {
        return "hello " + name;
    }

    @LogExit
    public String exitOnly(String name) {
        return "goodbye " + name;
    }

    @LogEntry
    @LogExit
    public String entryAndExit(String name) {
        return "hi " + name;
    }

    @LogEntry
    public void entryVoid(String name) {
        // no-op
    }

    @LogExit(includeReturnValue = false)
    public String exitNoReturn(String name) {
        return "secret " + name;
    }

    @LogEntry
    public String withMaskedField(String username, @MaskField String password) {
        return "ok";
    }

    @LogEntry
    public String withCustomMask(String username, @MaskField(mask = "[REDACTED]") String token) {
        return "ok";
    }

    @LogEntry
    @LogExit(logExceptions = true)
    public String failingEntryExit() {
        throw new IllegalStateException("boom");
    }

    @LogExit(logExceptions = false)
    public String failingSilent() {
        throw new IllegalStateException("boom");
    }

    @LogPerformance
    public String perfBasic(String input) {
        return "done:" + input;
    }

    @LogPerformance(includeReturnValue = true)
    public String perfWithReturn(String input) {
        return "result:" + input;
    }

    @LogPerformance(includeArgs = false)
    public String perfNoArgs(String input) {
        return "ok";
    }

    @LogPerformance(thresholdMs = 10000)
    public String perfBelowThreshold() {
        return "fast";
    }

    @LogPerformance
    public String perfFailing() {
        throw new RuntimeException("perf-boom");
    }

    @LogEntry(level = LogLevel.INFO, message = "custom note")
    public String entryCustomMessage(String name) {
        return name;
    }
}
