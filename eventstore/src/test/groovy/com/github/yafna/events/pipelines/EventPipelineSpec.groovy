package com.github.yafna.events.pipelines

import com.github.yafna.events.Event
import com.github.yafna.events.dispatcher.GsonEventDispatcher
import com.github.yafna.events.handlers.count.RabbitCountingHandler
import com.github.yafna.events.handlers.count.Rabbits
import com.github.yafna.events.rabbits.Rabbit
import com.github.yafna.events.rabbits.RabbitAdded
import com.github.yafna.events.store.file.GsonFileEventStore
import com.google.gson.Gson
import spock.lang.Specification

import java.time.Clock
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

class EventPipelineSpec extends Specification {

    ForkJoinPool executor = new ForkJoinPool()
    Clock clock = Clock.systemUTC()
    GsonFileEventStore store = new GsonFileEventStore(clock, File.createTempDir())

    def 'given [rabbit.added] event persisted should call EventHandler for it'() {
        given:
            def handler = new RabbitCountingHandler()
            EventPipeline<Rabbit, RabbitAdded> subj = new EventPipeline(
                    new GsonEventDispatcher(store, executor), RabbitAdded.class, handler, "rabbit-counter", clock, executor 
            )
        when: "rabbit is added"
            store.persist("rabbit", "1", "added", payload("Alfa", "Longear"))
            executor.awaitQuiescence(10, TimeUnit.SECONDS)
        then: "count increases to 1"
            handler.count.get() == 1

        when: "rabbit is added again"
            store.persist("rabbit", "2", "added", payload("Beta", "Furtail"))
            executor.awaitQuiescence(10, TimeUnit.SECONDS)
            List<Event> countedEvents = store.getEvents("rabbits", Rabbits.ID, null).collect(Collectors.toList())
        then: "count increases to 2"
            handler.count.get() == 2
            countedEvents.collect({it.payload}) == ['{"count":2,"message":"cool"}']

        when: "rabbit is fried"
            store.persist("rabbit", "5", "fried", payload("Gamma", "Hot"))
            executor.awaitQuiescence(10, TimeUnit.SECONDS)
        then: "count does not change - a fried rabbit is still a rabbit"
            handler.count.get() == 2

        when: "chicken is added"
            store.persist("chicken", "6", "added", payload("Bush", "Quadleg"))
            executor.awaitQuiescence(10, TimeUnit.SECONDS)
        then: "count does not change - it is not a rabbit"
            handler.count.get() == 2 
    }

    private static String payload(String name, String key) {
        new Gson().toJson(new RabbitAdded(name, key))
    }

}
