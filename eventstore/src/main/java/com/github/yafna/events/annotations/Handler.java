package com.github.yafna.events.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks methods of aggregate object that describe event handlers.
 * Such methods must match one of the following contracts:
 * a) One argument of type assignment compatible with event payload
 * b) Two arguments, first is event metadata, second is
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Handler {
    /**
     * Event name, in case multiple events are described by a single payload class,
     * or payload argument type does not math event payload type exactly.
     */
    String[] value() default {};
}
