package com.github.yafna.events.pipelines;

import com.github.yafna.events.Event;
import com.github.yafna.events.EventMeta;
import com.github.yafna.events.aggregate.Aggregate;
import com.github.yafna.events.annotations.Origin;
import com.github.yafna.events.dispatcher.EventDispatcher;
import com.github.yafna.events.handlers.DomainHandlerRegistry;
import com.github.yafna.events.utils.StreamUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
public class AggregatePipeline<A extends Aggregate> {
    private final EventDispatcher dispatcher;
    private final Map<String, A> objects = new HashMap<>();
    private final String origin;
    private final DomainHandlerRegistry<A> handlers;
    private final Map<String, Class<?>> index;
    private final Function<String, A> constructor;

    /**
     * @param clazz Aggregate class
     * @param dispatcher event dispatcher to use
     * @param eventTypes index map (event type name) -> (event class)
     * @param handlers registry resolves event class to a list of handlers
     * @param constructor function to create new aggregate instance for a given id
     */
    public AggregatePipeline(
            Class<A> clazz,
            EventDispatcher dispatcher,
            Map<String, Class<?>> eventTypes,
            DomainHandlerRegistry<A> handlers,
            Function<String, A> constructor
    ) {
        origin = clazz.getAnnotation(Origin.class).value();
        this.dispatcher = dispatcher;
        this.handlers = handlers;
        this.index = eventTypes;
        this.constructor = constructor;
    }


    public <T> Event push(String aggregateId, T event) {
        return dispatcher.store(origin, aggregateId, event);
    }

    public A get(String id) {
        A aggregate = objects.computeIfAbsent(id, constructor);
        AtomicLong last = aggregate.getLastEvent();
        long seq = last.get();
        Stream<EventMeta<?>> events = dispatcher.getEvents(origin, id, seq, index);

        for (Iterator<EventMeta<?>> it = events.iterator(); it.hasNext(); ) {
            EventMeta<?> event = it.next();
            handle(aggregate, event.getMeta(), event.getPayload());
            last.set(event.getMeta().getSeq());
        }
        return aggregate;
    }

    protected <T> Event store(T event, String aggregateId) {
        return dispatcher.store(origin, aggregateId, event);
    }

    private <T> void handle(A object, Event event, T payload) {
        // Here we rely that deserializer in dispatcher produces plain objects, not some crazy reflection / asm stuff
        // TODO This is not the greatest assumption to make, we need to reconsider the approach here 
        Class<T> type = (Class<T>) payload.getClass();
        String id = event.getId();
        log.debug("Handling {} [{}/{}]", id, event.getType(), event.getAggregateId());
        StreamUtils.fold(handlers.get(type).stream().map(
                h -> (Function<A, A>) a -> h.apply(a, event, payload)
        )).apply(object);
        log.debug("Processed [{}]: {}", id, event.getType());
    }

}
