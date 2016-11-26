package com.github.yafna.events.handlers;

import java.util.List;

@FunctionalInterface
public interface DomainHandlerRegistry<A>  {
    /**
     * Returns list of handlers for a given event class
     */
    <T> List<DomainHandler<A, T>> get(Class<T> clazz);
}
