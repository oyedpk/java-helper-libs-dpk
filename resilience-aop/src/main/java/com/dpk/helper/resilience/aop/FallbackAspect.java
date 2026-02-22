package com.dpk.helper.resilience.aop;

import com.dpk.helper.resilience.Fallback;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * AspectJ aspect that invokes a fallback method when the annotated method
 * throws an exception matching the configured criteria.
 */
@Aspect
public class FallbackAspect {

    private static final Logger log = LoggerFactory.getLogger(FallbackAspect.class);

    @Around("@annotation(fallback)")
    public Object handleFallback(ProceedingJoinPoint pjp, Fallback fallback) throws Throwable {
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            if (!shouldApplyFallback(t, fallback)) {
                throw t;
            }

            MethodSignature sig = (MethodSignature) pjp.getSignature();
            String methodName = sig.getMethod().getName();
            String fallbackMethodName = fallback.fallbackMethod();

            log.debug("Method {} failed with {}, invoking fallback {}",
                    methodName, t.getClass().getSimpleName(), fallbackMethodName);

            Method fallbackMethod = findFallbackMethod(pjp, sig, fallbackMethodName, t);
            if (fallbackMethod == null) {
                throw new IllegalStateException(
                        "No suitable fallback method '%s' found in %s. Expected same parameters as %s, optionally with a trailing Throwable parameter."
                                .formatted(fallbackMethodName,
                                        pjp.getTarget().getClass().getSimpleName(), methodName), t);
            }

            try {
                return invokeFallback(pjp, fallbackMethod, t);
            } catch (InvocationTargetException ite) {
                throw ite.getCause();
            }
        }
    }

    private boolean shouldApplyFallback(Throwable t, Fallback fallback) {
        if (fallback.applyOn().length == 0) {
            return true;
        }
        for (Class<? extends Throwable> cls : fallback.applyOn()) {
            if (cls.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    private Method findFallbackMethod(ProceedingJoinPoint pjp, MethodSignature sig,
                                       String fallbackMethodName, Throwable t) {
        Class<?> targetClass = pjp.getTarget().getClass();
        Class<?>[] originalParamTypes = sig.getParameterTypes();

        // Try: same params + Throwable
        Class<?>[] withThrowable = Arrays.copyOf(originalParamTypes, originalParamTypes.length + 1);
        withThrowable[withThrowable.length - 1] = Throwable.class;

        try {
            return targetClass.getMethod(fallbackMethodName, withThrowable);
        } catch (NoSuchMethodException ignored) {}

        // Try: same params only
        try {
            return targetClass.getMethod(fallbackMethodName, originalParamTypes);
        } catch (NoSuchMethodException ignored) {}

        return null;
    }

    private Object invokeFallback(ProceedingJoinPoint pjp, Method fallbackMethod, Throwable t)
            throws InvocationTargetException, IllegalAccessException {
        Object[] originalArgs = pjp.getArgs();
        Class<?>[] paramTypes = fallbackMethod.getParameterTypes();

        if (paramTypes.length > originalArgs.length
                && Throwable.class.isAssignableFrom(paramTypes[paramTypes.length - 1])) {
            Object[] argsWithThrowable = Arrays.copyOf(originalArgs, originalArgs.length + 1);
            argsWithThrowable[argsWithThrowable.length - 1] = t;
            return fallbackMethod.invoke(pjp.getTarget(), argsWithThrowable);
        }

        return fallbackMethod.invoke(pjp.getTarget(), originalArgs);
    }
}
