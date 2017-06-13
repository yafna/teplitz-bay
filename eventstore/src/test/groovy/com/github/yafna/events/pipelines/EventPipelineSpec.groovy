package com.github.yafna.events.pipelines

import com.github.yafna.events.dispatcher.GsonEventDispatcher
import com.github.yafna.events.handlers.count.RabbitCountingHandler
import com.github.yafna.events.rabbits.Rabbit
import com.github.yafna.events.rabbits.RabbitAdded
import com.github.yafna.events.store.file.GsonFileEventStore
import com.google.gson.Gson
import spock.lang.Specification

import java.time.Clock
import java.util.concurrent.TimeUnit

class EventPipelineSpec extends Specification {

    Clock clock = Clock.systemUTC()
    GsonFileEventStore store = new GsonFileEventStore(clock, File.createTempDir())

    def 'given [rabbit.added] event persisted should call EventHandler for it'() {
        given:
            def handler = new RabbitCountingHandler()
            EventPipeline<Rabbit, RabbitAdded> subj = new EventPipeline(
                    new GsonEventDispatcher(store), RabbitAdded.class, handler, "rabbit-counter", clock
            )
        when:
            store.persist("rabbit", "1", "added", payload("Alfa", "Longear"))
            subj.executor.awaitQuiescence(10, TimeUnit.SECONDS)
        then:
            handler.count.get() == 1
        when:
            store.persist("rabbit", "2", "added", payload("Beta", "Furtail"))
            subj.executor.awaitQuiescence(10, TimeUnit.SECONDS)
        then:
            handler.count.get() == 2
        when:
            store.persist("rabbit", "5", "fried", payload("Gamma", "Hot"))
            subj.executor.awaitQuiescence(10, TimeUnit.SECONDS)
        then:
            handler.count.get() == 2
        when:
            store.persist("chicken", "6", "added", payload("Bush", "Quadleg"))
            subj.executor.awaitQuiescence(10, TimeUnit.SECONDS)
        then:
            handler.count.get() == 2 
    }

    private static String payload(String name, String key) {
        new Gson().toJson(new RabbitAdded(name, key))
    }

}
