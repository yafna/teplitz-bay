package com.github.yafna.events.handlers.count;

import com.github.yafna.events.EmittedEvent;
import com.github.yafna.events.Event;
import com.github.yafna.events.annotations.Handler;
import com.github.yafna.events.handlers.EventHandler;
import com.github.yafna.events.rabbits.RabbitAdded;

import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Handler
public class RabbitCountingHandler implements EventHandler<RabbitAdded> {
    private AtomicLong count = new AtomicLong(0);
    
    @Override
    public Stream<EmittedEvent<?>> apply(Event meta, RabbitAdded payload) {
        count.incrementAndGet();
        return Stream.of();
    }
}
