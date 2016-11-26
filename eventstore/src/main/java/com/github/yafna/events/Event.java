package com.github.yafna.events;

import java.time.Instant;

/**
 *
 */
public interface Event {
    /**
     * Origin id affected by the event.
     * Optional. Missing aggregate id indicates an 'global' event that cannot be associated with a single aggregate.
     *
     */
    String getAggregateId();

    /**
     * Sequence number of the event in scope of a given aggregate id.
     * This is used to ensure that domain events are replayed in the same sequence.
     */
    Long getSeq();

    /**
     * Unique identifier of the event - normally a uuid.
     */
    String getId();

    /**
     * Correlation id. If correlation id is present on the event that has caused
     * the new event to be emitted, it is copied from the original event.
     * Otherwise, the original correlation id will be assiged from the id of the original event
     */
    String getCorrId();

    /**
     * {@code null} for events that have arrived from outside. This includes user commands, events
     * from other publishers and possibly other cases - such as timer events, especially those not
     * tied to any past events.
     */
    String getCauseId();

    /**
     * Specifies the domain, with which event is associated.
     * Optional.
     */
    String getOrigin();

    /**
     * Event type identifier. We try to stick to the following notation when naming the events:
     * [aggregate].[action]
     * <br/>{@code aggregate} is the name of the aggregate being affected by the event.
     * It is important that this name only makes sense in the domain of emitter - if event is
     * external we might not have a first-class entity that matches o it
     * <br/>{@code action} describes what happened.
     * <p>
     * Examples:
     * peer.added, backup.created
     */
    String getType();

//    /**
//     * Timestamp when the event was originally created. This can be received from external system
//     * and can be heavily different with 'stored' in replay scenarios, when we catch up with
//     * external log and receive events much later than they have happened.
//     * This field should be null for events that do not carry the moment of creation themselves,
//     * such as user actions.
//     */
//    Instant getCreated();

    /**
     * Timestamp when we have placed the event in our storage.
     */
    Instant getStored();

    String getPayload();
}
