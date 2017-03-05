package com.github.yafna.events.store;

import com.github.yafna.events.Event;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
public class StoredEvent implements Event, Serializable {
    private String id;
    private String aggregateId;
    private Long seq;
    private String corrId;
    private String causeId;
    private String origin;
    private String type;
    private Instant stored;

    /**
     * JSON representation of the event.
     */
    private String payload;
}
