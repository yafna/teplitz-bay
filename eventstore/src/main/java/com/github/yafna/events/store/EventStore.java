package com.github.yafna.events.store;

import com.github.yafna.events.Event;
import com.github.yafna.events.store.StoredEvent;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface EventStore {
    Stream<Event> getEvents(String origin, String aggregateId, Long fromSeq);

    Persister persist(String aggregateId);

    Persister persist(String causeId, String corrId, String aggregateId);

    Persister persist(String causeId, String corrId);

    Persister persist();

    interface Persister {
        Event apply(String origin, String type, String payload);

        default Persister then(Consumer<StoredEvent> after) {
            Objects.requireNonNull(after);
            return (origin, type, payload) -> {
                Event event = apply(origin, type, payload);
                after.accept((StoredEvent) event);
                return event;
            };
        }

    }
}
