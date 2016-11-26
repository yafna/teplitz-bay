package com.github.yafna.events.handlers;

import com.github.yafna.events.Event;

/**
 * Only handlers of this type of event are allowed to update domain models.
 * However they are not allowed to emit further events (unlike {@link EventHandler}).
 *
 * @param <A> Origin type
 * @param <T> Event type
 */
public interface DomainHandler<A, T> {

    /**
     * Apply event
     * @param object domain object to which state is applied
     * @param meta metadata of the event being processed
     * @param payload event payload
     */
    A apply(A object, Event meta, T payload);
}
