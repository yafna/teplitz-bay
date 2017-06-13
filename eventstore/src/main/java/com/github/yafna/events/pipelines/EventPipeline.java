package com.github.yafna.events.pipelines;

import com.github.yafna.events.DomainEvent;
import com.github.yafna.events.EmittedEvent;
import com.github.yafna.events.Event;
import com.github.yafna.events.EventMeta;
import com.github.yafna.events.aggregate.PayloadUtils;
import com.github.yafna.events.dispatcher.EventDispatcher;
import com.github.yafna.events.handlers.EventHandler;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

@Slf4j
public class EventPipeline<A, T extends DomainEvent<A>> {
    protected final ForkJoinPool executor = new ForkJoinPool();
    private final EventDispatcher dispatcher;
    private final String origin;
    private final String handlerId;
    private final EventHandler<T> handler;

    public EventPipeline(
            EventDispatcher dispatcher, Class<T> eventType, EventHandler<T> handler, String handlerId, Clock clock
    ) {
        origin = PayloadUtils.origin(eventType);
        this.dispatcher = dispatcher;
        this.handler = handler;
        this.handlerId = handlerId;
        Duration timeWindow = Duration.ofDays(1);
        Instant since = clock.instant().minus(timeWindow);
        executor.submit(() -> recap(dispatcher, eventType, since));
    }

    private void recap(EventDispatcher dispatcher, Class<T> eventType, Instant start) {
        String type = PayloadUtils.eventType(eventType).value();

        for (Instant t = start; ; ) {
            Stream<EventMeta<T>> recap = dispatcher.subscribe(origin, type, t, eventType, this::process);
            if (recap == null) {
                break;
            } else {
                t = recap.map(
                        event -> process(event.getMeta(), event.getPayload())
                ).max(Comparator.naturalOrder()).orElseThrow(() -> {
                    log.error("Got empty stream for [{}].[{}]", origin, type);
                    return new IllegalStateException("Should never return empty stream");
                });
            }
        }
    }

    private Instant process(Event meta, T payload) {
        Stream<EmittedEvent<?>> emitted = Stream.concat(
                handler.apply(meta, payload),
                Stream.of(EmittedEvent.of("handlers", handlerId, "handled"))
        );
        executor.submit(() -> dispatcher.store(meta.getCauseId(), meta.getCorrId(), emitted));
        return meta.getStored();
    }

}
