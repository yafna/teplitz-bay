package com.github.yafna.events.store;

import com.github.yafna.events.Event;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface EventStore {
    Stream<Event> getEvents(String origin, String aggregateId, Long fromSeq);

    /**
     * Conditionally subscribes to events from a given moment.
     * If there are no events with matching origin and type present in store 
     * since the given instant, returns null and subscribes provided callback to be invoked 
     * when matching event has been persisted. 
     * If there are matching events present, the subscription is not set up and one or more 
     * of them will be returned. The returned events will contain at least all the events that 
     * happened *exactly* at a given instant, however there are no other guarantees regarding them.
     * 
     * @param origin origin to subscribe to
     * @param type subscription event type 
     * @param since the moment in time from which pas events are requested 
     * @param callback function to be invoked on each event.
     * @return List of events present in store since (non-inclusive) the given instant,   
     */
    List<Event> subscribe(String origin, String type, Instant since, Consumer<Event> callback);

    Persister persist(String aggregateId);

    Persister persist(String causeId, String corrId, String aggregateId);

    Persister persist(String causeId, String corrId);

    Persister persist();

    @FunctionalInterface
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
