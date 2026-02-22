package com.dpk.helper.resilience.aop;

import com.dpk.helper.resilience.CircuitBreaker;
import com.dpk.helper.resilience.CircuitBreakerOpenException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * AspectJ aspect that implements the circuit breaker pattern
 * for methods annotated with {@link CircuitBreaker}.
 */
@Aspect
public class CircuitBreakerAspect {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerAspect.class);

    private final ConcurrentMap<String, CircuitBreakerState> circuits = new ConcurrentHashMap<>();

    @Around("@annotation(cb)")
    public Object handleCircuitBreaker(ProceedingJoinPoint pjp, CircuitBreaker cb) throws Throwable {
        String name = resolveName(cb, pjp);
        CircuitBreakerState state = circuits.computeIfAbsent(name,
                k -> new CircuitBreakerState(cb.failureThreshold(), cb.resetTimeoutMs()));

        CircuitBreakerState.State currentState = state.getState();

        if (currentState == CircuitBreakerState.State.OPEN) {
            log.debug("Circuit breaker '{}' is OPEN, rejecting call", name);
            throw new CircuitBreakerOpenException(name);
        }

        try {
            Object result = pjp.proceed();
            state.recordSuccess();
            if (currentState == CircuitBreakerState.State.HALF_OPEN) {
                log.info("Circuit breaker '{}' recovered, now CLOSED", name);
            }
            return result;
        } catch (Throwable t) {
            if (isCountableFailure(t, cb)) {
                state.recordFailure();
                if (state.getState() == CircuitBreakerState.State.OPEN) {
                    log.warn("Circuit breaker '{}' opened after {} failures",
                            name, cb.failureThreshold());
                }
            }
            throw t;
        }
    }

    private String resolveName(CircuitBreaker cb, ProceedingJoinPoint pjp) {
        if (!cb.name().isEmpty()) {
            return cb.name();
        }
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        return sig.getDeclaringType().getSimpleName() + "." + sig.getMethod().getName();
    }

    private boolean isCountableFailure(Throwable t, CircuitBreaker cb) {
        if (cb.failOn().length == 0) {
            return true;
        }
        for (Class<? extends Throwable> failOn : cb.failOn()) {
            if (failOn.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    // Visible for testing
    CircuitBreakerState getCircuitState(String name) {
        return circuits.get(name);
    }

    // Visible for testing
    void clearAll() {
        circuits.clear();
    }
}
