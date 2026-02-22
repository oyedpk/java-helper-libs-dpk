package com.dpk.helper.timing.aop;

import com.dpk.helper.timing.Tag;
import com.dpk.helper.timing.Timed;
import com.dpk.helper.timing.TimedClass;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AspectJ aspect that intercepts methods annotated with {@link Timed}
 * or classes annotated with {@link TimedClass}, measures execution time,
 * and reports via a {@link MetricsReporter}.
 */
@Aspect
public class TimedAspect {

    private static final Logger log = LoggerFactory.getLogger(TimedAspect.class);

    private final MetricsReporter reporter;
    private final ExpressionParser spelParser = new SpelExpressionParser();

    public TimedAspect(MetricsReporter reporter) {
        this.reporter = reporter;
    }

    public TimedAspect() {
        this(new Slf4jMetricsReporter());
    }

    @Pointcut("@annotation(com.dpk.helper.timing.Timed)")
    public void timedMethod() {}

    @Pointcut("@within(com.dpk.helper.timing.TimedClass)")
    public void timedClass() {}

    /**
     * Intercepts methods directly annotated with {@code @Timed}.
     */
    @Around("timedMethod() && @annotation(timed)")
    public Object timeMethod(ProceedingJoinPoint pjp, Timed timed) throws Throwable {
        String metricName = resolveMetricName(timed.value(), pjp);
        Map<String, String> tags = collectStaticTags(timed.tags());
        resolveDynamicTags(timed.dynamicTags(), pjp, tags);

        return executeAndReport(pjp, metricName, tags, timed.reportExceptions());
    }

    /**
     * Intercepts methods in classes annotated with {@code @TimedClass},
     * but only when the method itself is NOT annotated with {@code @Timed}
     * (method-level takes precedence).
     */
    @Around("timedClass() && !timedMethod() && @within(timedClass)")
    public Object timeClassMethod(ProceedingJoinPoint pjp, TimedClass timedClass) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();

        // Only instrument public methods
        if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
            return pjp.proceed();
        }

        String prefix = timedClass.prefix();
        String methodName = method.getName();
        String metricName = prefix.isEmpty() ? methodName : prefix + "." + methodName;

        Map<String, String> tags = collectStaticTags(timedClass.tags());

        return executeAndReport(pjp, metricName, tags, true);
    }

    private Object executeAndReport(ProceedingJoinPoint pjp, String metricName,
                                     Map<String, String> tags, boolean reportExceptions) throws Throwable {
        long startNanos = System.nanoTime();
        Throwable caught = null;

        try {
            return pjp.proceed();
        } catch (Throwable t) {
            caught = t;
            throw t;
        } finally {
            long durationNanos = System.nanoTime() - startNanos;
            if (caught == null || reportExceptions) {
                try {
                    reporter.report(metricName, durationNanos, tags, caught);
                } catch (Exception e) {
                    log.warn("Failed to report timing metric [{}]", metricName, e);
                }
            }
        }
    }

    private String resolveMetricName(String explicitName, ProceedingJoinPoint pjp) {
        if (!explicitName.isEmpty()) {
            return explicitName;
        }
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        return sig.getDeclaringType().getSimpleName() + "." + sig.getMethod().getName();
    }

    private Map<String, String> collectStaticTags(Tag[] tags) {
        Map<String, String> tagMap = new LinkedHashMap<>();
        for (Tag tag : tags) {
            tagMap.put(tag.key(), tag.value());
        }
        return tagMap;
    }

    void resolveDynamicTags(String[] dynamicTags, ProceedingJoinPoint pjp, Map<String, String> tags) {
        if (dynamicTags.length == 0) {
            return;
        }

        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("args", pjp.getArgs());
        context.setVariable("target", pjp.getTarget());

        // Also set named parameters if available
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String[] paramNames = sig.getParameterNames();
        if (paramNames != null) {
            Object[] args = pjp.getArgs();
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        for (String dynamicTag : dynamicTags) {
            int eqIdx = dynamicTag.indexOf('=');
            if (eqIdx <= 0) {
                log.warn("Invalid dynamic tag format '{}', expected 'key=expression'", dynamicTag);
                continue;
            }
            String key = dynamicTag.substring(0, eqIdx).trim();
            String expression = dynamicTag.substring(eqIdx + 1).trim();

            try {
                Object value = spelParser.parseExpression(expression).getValue(context);
                tags.put(key, value != null ? value.toString() : "null");
            } catch (Exception e) {
                log.warn("Failed to evaluate dynamic tag expression '{}': {}", expression, e.getMessage());
                tags.put(key, "ERROR");
            }
        }
    }
}
