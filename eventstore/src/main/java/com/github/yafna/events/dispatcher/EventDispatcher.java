package com.github.yafna.events.dispatcher;

import com.github.yafna.events.EmittedEvent;
import com.github.yafna.events.Event;
import com.github.yafna.events.EventMeta;
import com.github.yafna.events.annotations.EvType;
import com.github.yafna.events.store.EventStore;
import com.github.yafna.events.store.ProtoEvent;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class has the following responsibilities:
 * 1. Manage serialization and deserialization of events
 * 2. Notify pipelines about stored events
 * 3. Manage transactions if event store behind it supports them
 */
@AllArgsConstructor
public abstract class EventDispatcher {

    private final EventStore store;

    public <T> Event store(String origin, String aggregateId, T event) {
        String type = event.getClass().getAnnotation(EvType.class).value();
        String json = serialize(event);
        return store.persist(origin, aggregateId, type, json);
    }

    public long store(String causeId, String corrId, Stream<EmittedEvent<?>> events) {
        ProtoEvent[] protoEvents = events.map(emitted -> {
            String json = serialize(emitted.getPayload());
            return new ProtoEvent(emitted.getAggregateId(), emitted.getOrigin(), emitted.getType(), json);
        }).toArray(ProtoEvent[]::new);
        // Array used here to be sure that all the events are successfully serialized at this point
        return store.persist(causeId, corrId, protoEvents);
    }

    public <T> Stream<EventMeta<T>> subscribe(String origin, String type, Instant since, Class<T> clazz, BiConsumer<Event, T> callback) {
        return Optional.ofNullable(
                store.subscribe(
                        origin, type, since,
                        event -> callback.accept(event, deserialize(event.getPayload(), clazz))
                )
        ).map(
                spliterator -> StreamSupport.stream(spliterator, false).map(
                        event -> new EventMeta<>(event, deserialize(event.getPayload(), clazz))
                )
        ).orElse(null);
    }

    protected abstract <T> String serialize(T event);

    protected abstract <T> T deserialize(String payload, Class<T> clazz);
}
