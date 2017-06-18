package com.github.yafna.events.handlers;

import com.github.yafna.events.EmittedEvent;
import com.github.yafna.events.Event;

import java.util.stream.Stream;

/**
 * A handler that is allowed to emit other events, but is not attached to aggregate.
 *
 * @param <T> Event type
 */
@FunctionalInterface
public interface EventHandler<T> {

    /**
     * Apply event
     * @param meta metadata of the event being processed
     * @param payload event payload
     * @return events emitted as a result of processing this event
     */
    Stream<EmittedEvent<?>> apply(Event meta, T payload);
}
