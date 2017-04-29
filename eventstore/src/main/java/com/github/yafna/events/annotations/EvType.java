package com.github.yafna.events.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks classes that describe event payload.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EvType {
    /**
     * Event type name. Currently must be unique per event class, however no code should rely on that.
     * It might be practical to support multiple event classes to represent the same event name.
     */
    String value() default "";
}
