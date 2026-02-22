package com.dpk.helper.logging.aop;

import com.dpk.helper.logging.LogEntry;
import com.dpk.helper.logging.LogExit;
import com.dpk.helper.logging.LogLevel;
import com.dpk.helper.logging.LogPerformance;
import com.dpk.helper.logging.MaskField;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.StringJoiner;

/**
 * AspectJ aspect that processes {@link LogEntry}, {@link LogExit},
 * and {@link LogPerformance} annotations to produce structured log output.
 */
@Aspect
public class LoggingAspect {

    @Around("@annotation(logEntry) && @annotation(logExit)")
    public Object logEntryAndExit(ProceedingJoinPoint pjp, LogEntry logEntry, LogExit logExit) throws Throwable {
        Logger log = getLogger(pjp);
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();

        logMethodEntry(log, logEntry.level(), method, pjp.getArgs(), logEntry.message());

        long startNanos = System.nanoTime();
        try {
            Object result = pjp.proceed();
            logMethodExit(log, logExit.level(), method, result, logExit.includeReturnValue());
            return result;
        } catch (Throwable t) {
            if (logExit.logExceptions()) {
                log.warn("<-- {}.{}() threw {} in {}ms",
                        method.getDeclaringClass().getSimpleName(), method.getName(),
                        t.getClass().getSimpleName(),
                        formatMs(System.nanoTime() - startNanos));
            }
            throw t;
        }
    }

    @Around("@annotation(logEntry) && !@annotation(com.dpk.helper.logging.LogExit)")
    public Object logEntryOnly(ProceedingJoinPoint pjp, LogEntry logEntry) throws Throwable {
        Logger log = getLogger(pjp);
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        logMethodEntry(log, logEntry.level(), method, pjp.getArgs(), logEntry.message());
        return pjp.proceed();
    }

    @Around("@annotation(logExit) && !@annotation(com.dpk.helper.logging.LogEntry)")
    public Object logExitOnly(ProceedingJoinPoint pjp, LogExit logExit) throws Throwable {
        Logger log = getLogger(pjp);
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();

        try {
            Object result = pjp.proceed();
            logMethodExit(log, logExit.level(), method, result, logExit.includeReturnValue());
            return result;
        } catch (Throwable t) {
            if (logExit.logExceptions()) {
                log.warn("<-- {}.{}() threw {}",
                        method.getDeclaringClass().getSimpleName(), method.getName(),
                        t.getClass().getSimpleName());
            }
            throw t;
        }
    }

    @Around("@annotation(logPerf)")
    public Object logPerformance(ProceedingJoinPoint pjp, LogPerformance logPerf) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Logger log = getLogger(pjp);

        long startNanos = System.nanoTime();
        Throwable caught = null;
        Object result = null;

        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            caught = t;
            throw t;
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            if (durationMs >= logPerf.thresholdMs()) {
                String className = method.getDeclaringClass().getSimpleName();
                String methodName = method.getName();

                StringBuilder sb = new StringBuilder();
                sb.append("PERF ").append(className).append(".").append(methodName).append("(");

                if (logPerf.includeArgs()) {
                    sb.append(buildArgString(method, pjp.getArgs()));
                }
                sb.append(")");

                if (caught != null) {
                    sb.append(" threw ").append(caught.getClass().getSimpleName());
                } else if (logPerf.includeReturnValue()) {
                    sb.append(" => ").append(result);
                }

                sb.append(" [").append(durationMs).append("ms]");

                if (caught != null) {
                    doLog(log, LogLevel.WARN, sb.toString());
                } else {
                    doLog(log, logPerf.level(), sb.toString());
                }
            }
        }
    }

    private void logMethodEntry(Logger log, LogLevel level, Method method, Object[] args, String customMessage) {
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String argString = buildArgString(method, args);

        if (!customMessage.isEmpty()) {
            doLog(log, level, "--> {}.{}({}) [{}]", className, methodName, argString, customMessage);
        } else {
            doLog(log, level, "--> {}.{}({})", className, methodName, argString);
        }
    }

    private void logMethodExit(Logger log, LogLevel level, Method method, Object result, boolean includeReturnValue) {
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();

        if (method.getReturnType() == void.class || !includeReturnValue) {
            doLog(log, level, "<-- {}.{}()", className, methodName);
        } else {
            doLog(log, level, "<-- {}.{}() => {}", className, methodName, result);
        }
    }

    String buildArgString(Method method, Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }

        MethodSignature sig = null;
        String[] paramNames = method.getParameters().length > 0
                ? java.util.Arrays.stream(method.getParameters())
                    .map(java.lang.reflect.Parameter::getName)
                    .toArray(String[]::new)
                : new String[0];

        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        StringJoiner joiner = new StringJoiner(", ");

        for (int i = 0; i < args.length; i++) {
            String name = (i < paramNames.length) ? paramNames[i] : ("arg" + i);
            String value = getMaskedValue(paramAnnotations[i], args[i]);
            joiner.add(name + "=" + value);
        }

        return joiner.toString();
    }

    private String getMaskedValue(Annotation[] annotations, Object value) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof MaskField mask) {
                return mask.mask();
            }
        }
        return String.valueOf(value);
    }

    private Logger getLogger(ProceedingJoinPoint pjp) {
        return LoggerFactory.getLogger(pjp.getTarget().getClass());
    }

    private String formatMs(long nanos) {
        return String.format("%.2f", nanos / 1_000_000.0);
    }

    private void doLog(Logger log, LogLevel level, String format, Object... args) {
        switch (level) {
            case TRACE -> log.trace(format, args);
            case DEBUG -> log.debug(format, args);
            case INFO -> log.info(format, args);
            case WARN -> log.warn(format, args);
            case ERROR -> log.error(format, args);
        }
    }
}
