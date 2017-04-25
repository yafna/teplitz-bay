package com.github.yafna.events.pipelines;

import com.github.yafna.events.Event;
import com.github.yafna.events.aggregate.Aggregate;
import com.github.yafna.events.annotations.EvType;
import com.github.yafna.events.annotations.Origin;
import com.github.yafna.events.handlers.DomainHandlerRegistry;
import com.github.yafna.events.store.EventStore;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class AggregatePipeline<A extends Aggregate> {
    private final static String UNKNOWN_TYPE = "Unknown type";

    private final Gson gson = new Gson();

    private final EventStore store;
    private final Map<String, A> objects = new HashMap<>();
    private final String origin;
    private final DomainHandlerRegistry<A> handlers;
    private final Map<String, Class<?>> index;
    private final Function<String, A> constructor;

    /**
     * @param clazz Aggregate class
     * @param store event store to use
     * @param eventTypes index map (event type name) -> (event class)
     * @param handlers registry resolves event class to a list of handlers
     * @param constructor function to create new aggregate instance for a given id
     */
    public AggregatePipeline(
            Class<A> clazz,
            EventStore store,
            Map<String, Class<?>> eventTypes,
            DomainHandlerRegistry<A> handlers,
            Function<String, A> constructor
    ) {
        origin = clazz.getAnnotation(Origin.class).value();
        this.store = store;
        this.handlers = handlers;
        this.index = eventTypes;
        this.constructor = constructor;
    }


    public <T> Event push(String aggregateId, T event) {
        return store(event, aggregateId);
    }

    public A get(String id) {
        A aggregate = objects.computeIfAbsent(id, constructor);
        AtomicLong last = aggregate.getLastEvent();
        long seq = last.get();
        Stream<Event> events = store.getEvents(origin, id, seq);

        for (Iterator<Event> it = events.iterator(); it.hasNext(); ) {
            Event event = it.next();
            process(event, aggregate);
            last.set(event.getSeq());
        }
        return aggregate;
    }

    protected <T> Event store(T event, String aggregateId) {
        String type = event.getClass().getAnnotation(EvType.class).value();
        String json = gson.toJson(event);
        return store.persist(aggregateId).apply(origin, type, json);
    }

    private void process(Event event, A aggregate) {
        String type = event.getType();
        Class<?> clazz = index.get(type);
        if (clazz == null) {
            log.error("Event #{} [{}->{}] ignored: [{}]", event.getId(), origin, type, UNKNOWN_TYPE);
            String knownTypes = index.keySet().stream().collect(Collectors.joining(","));
            log.warn("Known types for [{}] are:\n{}", origin, knownTypes);
        } else {
            handle(event, clazz, aggregate);
        }
    }

    private <T> void handle(Event event, Class<T> type, A object) {
        String id = event.getId();
        log.debug("Handling {} [{}/{}]", id, event.getType(), event.getAggregateId());
        T payload = gson.fromJson(event.getPayload(), type);
        fold(handlers.get(type).stream().map(
                h -> (Function<A, A>) a -> h.apply(a, event, payload)
        )).apply(object);
        log.debug("Processed [{}]: {}", id, event.getType());
    }

    private static <T> UnaryOperator<T> fold(Stream<Function<T, T>> operations) {
        return initial -> {
            T value = initial;
            for (Iterator<Function<T, T>> it = operations.iterator(); it.hasNext();) {
                value = it.next().apply(value);
            }
            return value;
        };
    }

}
