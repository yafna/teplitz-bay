package com.github.yafna.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Deserialized event with metadata.
 */
@Getter
@AllArgsConstructor
public class EventMeta<T> {
    private final Event meta;
    private final T payload;
    
    public static <T> EventMeta<T> of(Event meta, T payload) {
        return new EventMeta<>(meta, payload); 
    } 
}
