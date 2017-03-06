package com.github.yafna.events.rabbits;


import com.github.yafna.events.Event;
import com.github.yafna.events.aggregate.Aggregate;
import com.github.yafna.events.annotations.Origin;
import com.github.yafna.events.annotations.Handler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Origin("rabbit")
@Slf4j
@RequiredArgsConstructor
public class Rabbit implements Aggregate {
    private final String id;
    private Instant born;
    private Instant dead;
    private String name;
    private String publicKey;
    private AtomicLong lastEvent = new AtomicLong(-1);

    @Handler(value = "init")
    public void create(Event meta) {
        born = meta.getStored();
        log.info("On [{}] rabbit [{}] was born, named [{}]", born, id, name);
    }

    @Handler
    public void create(Event meta, RabbitAdded data) {
        born = meta.getStored();
        name = data.getName();
        publicKey = data.getPublicKey();
        log.info("On [{}] rabbit [{}] was born, named [{}]", born, id, name);
    }

    @Handler
    public void updateName(RabbitNameUpdated data) {
        log.info("Rabbit [{}] renamed to [{}]", id, data.getName());
        name = data.getName();
    }

    @Handler
    public void remove(Event meta, RabbitRemoved data) {
        dead = meta.getStored();
        log.info("On [{}] rabbit [{}] was violently slaughtered by sword #[{}]", dead, id, meta.getId());
    }
}
