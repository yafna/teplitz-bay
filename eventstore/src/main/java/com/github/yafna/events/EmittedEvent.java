package com.github.yafna.events;

import com.github.yafna.events.aggregate.PayloadUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event that was not persisted yet. Designed to be returned as emitted in event handlers. 
 * Implementing this as a separate class provides hard compile-time guarantee that no event 
 * could be handled before it was persisted. * 
 */
@AllArgsConstructor
@Getter
public class EmittedEvent<T extends DomainEvent> {
    final String origin;
    final String aggregateId;
    final String type;
    final T payload;

    public static EmittedEvent<?> of(String origin, String aggregateId, String type) {
        return new EmittedEvent<>(origin, aggregateId, type, null);
    }

    public static <T extends DomainEvent> EmittedEvent<T> of(String aggregateId, T payload) {
        String origin = PayloadUtils.origin(payload.getClass());
        String type = PayloadUtils.eventType(payload.getClass()).value();
        return new EmittedEvent<>(origin, aggregateId, type, payload);
    }
}
