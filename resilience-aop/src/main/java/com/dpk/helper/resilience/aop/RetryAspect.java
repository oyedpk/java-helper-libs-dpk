package com.dpk.helper.resilience.aop;

import com.dpk.helper.resilience.Retry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AspectJ aspect that retries method invocations based on {@link Retry} configuration.
 */
@Aspect
public class RetryAspect {

    private static final Logger log = LoggerFactory.getLogger(RetryAspect.class);

    @Around("@annotation(retry)")
    public Object retryMethod(ProceedingJoinPoint pjp, Retry retry) throws Throwable {
        int maxAttempts = retry.maxAttempts();
        long delay = retry.backoff().delay();
        double multiplier = retry.backoff().multiplier();
        long maxDelay = retry.backoff().maxDelay();

        String methodName = ((MethodSignature) pjp.getSignature()).getMethod().getName();
        Throwable lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return pjp.proceed();
            } catch (Throwable t) {
                if (!shouldRetry(t, retry)) {
                    throw t;
                }

                lastException = t;

                if (attempt < maxAttempts) {
                    log.debug("Retry {}/{} for {} after {} - {}",
                            attempt, maxAttempts, methodName,
                            t.getClass().getSimpleName(), t.getMessage());

                    Thread.sleep(delay);
                    delay = computeNextDelay(delay, multiplier, maxDelay);
                }
            }
        }

        log.warn("All {} attempts exhausted for {}", maxAttempts, methodName);
        throw lastException;
    }

    private boolean shouldRetry(Throwable t, Retry retry) {
        // noRetryOn takes precedence
        for (Class<? extends Throwable> noRetry : retry.noRetryOn()) {
            if (noRetry.isInstance(t)) {
                return false;
            }
        }

        // If retryOn is empty, retry on all exceptions
        if (retry.retryOn().length == 0) {
            return true;
        }

        for (Class<? extends Throwable> retryOn : retry.retryOn()) {
            if (retryOn.isInstance(t)) {
                return true;
            }
        }

        return false;
    }

    static long computeNextDelay(long currentDelay, double multiplier, long maxDelay) {
        long next = (long) (currentDelay * multiplier);
        if (maxDelay > 0 && next > maxDelay) {
            return maxDelay;
        }
        return next;
    }
}
