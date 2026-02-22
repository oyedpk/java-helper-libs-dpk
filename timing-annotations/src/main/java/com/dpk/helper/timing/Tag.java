package com.dpk.helper.timing;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A key-value tag to attach to a timing metric.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Tag {
    String key();
    String value();
}
