package com.github.yafna.events.store;

import com.github.yafna.events.Event;

import java.time.Instant;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface EventStore {
    Stream<Event> getEvents(String origin, String aggregateId, Long fromSeq);

    /**
     * Persists a single event.
     * 
     * @param origin aggregate type identifier
     * @param aggregateId aggregate id to which event is assiciated
     * @param type event type
     * @param payload serialized event payload
     * @return Persisted event. This allows caller to immediately access id, timestamp and other metadata of the event.  
     */
    Event persist(String origin, String aggregateId, String type, String payload);

    /**
     * Persists multiple events, adding causation id and correlation id.
     * If the implementation supports transactions, all events should be persisted in the same transaction. 
     *
     * @param causeId id of the event that has caused events being persisted
     * @param corrId correlation id of the event that has caused events being persisted.
     * if it has {@code null} correlation id, event's 'normal' id should be used insstead
     * @param events events to per persisted 
     * @return Stream of event that were persisted  
     */
    long persist(String causeId, String corrId, ProtoEvent[] events);


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
    Spliterator<Event> subscribe(String origin, String type, Instant since, Consumer<Event> callback);
}
