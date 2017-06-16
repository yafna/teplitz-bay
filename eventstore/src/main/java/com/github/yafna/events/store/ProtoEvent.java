package com.github.yafna.events.store;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Serialized emitted event that was not persisted yet.  
 */
@AllArgsConstructor
@Getter
public class ProtoEvent {
    final String origin;
    final String aggregateId;
    final String type;
    final String payload;

    public static ProtoEvent of(String origin, String aggregateId, String type, String payload) {
        return new ProtoEvent(origin, aggregateId, type, payload);
    }
}
