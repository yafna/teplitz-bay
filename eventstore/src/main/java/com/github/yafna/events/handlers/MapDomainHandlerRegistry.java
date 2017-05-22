package com.github.yafna.events.handlers;

import com.github.yafna.events.aggregate.AggregateUtils;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * A trivial handler registry, backed up by a map
 * @param <A>
 */
@AllArgsConstructor
public class MapDomainHandlerRegistry<A> implements DomainHandlerRegistry<A> {
    private Map<String, List<DomainHandler<A, ?>>> handlers;

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<DomainHandler<A, T>> get(Class<T> clazz) {
        return (List<DomainHandler<A, T>>) (Object) handlers.get(AggregateUtils.eventType(clazz).value());
    }
}
