package com.github.yafna.events.aggregate;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementations of this interface are aggregates (domain objects)
 */
public interface Aggregate {
    /**
     * Aggregate id, as used in pipelines, etc. UUID.
     */
    String getId();

    /**
     * Sequental number of the last event applied to aggregate.
     * Uninitialized aggregate must return negative number.
     * Typically, creation will have number of 0, however there are no guarantees on that.
     */
    AtomicLong getLastEvent();
}
