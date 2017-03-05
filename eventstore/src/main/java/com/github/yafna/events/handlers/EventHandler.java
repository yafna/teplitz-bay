package com.github.yafna.events.handlers;

/**
 * A handler that is allowed to emit other events, but is not attached to aggregate.
 *
 * @param <T> Event type
 */
public interface EventHandler<T> {
}
