package com.github.yafna.events.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks aggregate root object. Provides metadata (such as aggregate name) for package scanning tools.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Origin {
    /**
     * Domain name. Used as event origin when storing event.
     */
    String value() default "";
}
