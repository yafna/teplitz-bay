package com.github.yafna.events.woodpecker;


import com.github.yafna.events.Event;
import com.github.yafna.events.aggregate.Aggregate;
import com.github.yafna.events.annotations.Handler;
import com.github.yafna.events.annotations.Origin;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

@Getter
@Origin("woodpecker")
@Slf4j
@RequiredArgsConstructor
public class Woodpecker implements Aggregate {
    private final String id;
    private AtomicLong lastEvent = new AtomicLong(-1);

    @Handler
    public void action(Event meta, WoodpeckerRemoved data) {
    }

    @Handler
    public void action2(Event meta, WoodpeckerRemoved data) {
    }
}
