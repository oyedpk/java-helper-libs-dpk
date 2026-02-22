package com.dpk.helper.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method parameter so its value is masked in log output.
 *
 * <p>Use on parameters that contain sensitive data (passwords, tokens, SSNs, etc.).
 *
 * <pre>{@code
 * @LogEntry
 * public void login(String username, @MaskField String password) { ... }
 * // logs: --> login(username=admin, password=***)
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface MaskField {

    /**
     * The mask string to use. Defaults to {@code "***"}.
     */
    String mask() default "***";
}
