package com.github.yafna.events.handlers.count;

import com.github.yafna.events.EmittedEvent;
import com.github.yafna.events.Event;
import com.github.yafna.events.handlers.EventHandler;
import com.github.yafna.events.rabbits.RabbitAdded;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Slf4j
public class RabbitCountingHandler implements EventHandler<RabbitAdded> {
    private AtomicLong count = new AtomicLong(0);
    
    @Override
    public Stream<EmittedEvent<?>> apply(Event meta, RabbitAdded payload) {
        long v = count.incrementAndGet();
        log.info("Counting rabbit {} as #{}", payload.getName(), v);
        if (v % 2 == 0) {
            return Stream.of(EmittedEvent.of(Rabbits.ID, new RabbitNumberIsEven(v, "cool"))); 
        } else { 
            return Stream.empty();
        }
    }
}
