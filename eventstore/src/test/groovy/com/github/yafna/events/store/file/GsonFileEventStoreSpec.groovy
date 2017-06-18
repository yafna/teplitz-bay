package com.github.yafna.events.store.file

import com.github.yafna.events.Event
import com.github.yafna.events.TestClock
import com.github.yafna.events.XJson
import com.github.yafna.events.store.ProtoEvent
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import java.util.stream.StreamSupport

public class GsonFileEventStoreSpec extends Specification {
    private final static String origin = "hedgehog"

    File root = File.createTempDir()
    String now = "2002-05-19T22:33:11Z"

    Clock clock = Clock.fixed(Instant.parse(now), ZoneId.of("UTC"))
    FileEventStore subj = new GsonFileEventStore(clock, root)

    @Unroll
    def "given event [#type] should persist it under [#subdir]"() {
        given:
            String now = "2002-05-19T22:33:11Z"
            String origin = "hedgehog"
        when:
            Event event = subj.persist(origin, aggregate, type, "12345")
            Path path = Paths.get(root.getPath(), subdir)
            List<String> body = Files.list(path).filter({ !Files.isDirectory(it) }).collect(readFile)
        then:
            event.id != null
            XJson.parse(body[0]).matches(data)
            XJson.parse(body[0]).matches([
                    "origin": "hedgehog",
                    "stored": now,
                    "payload": "12345"
            ])
        where:
            aggregate  | type          | subdir             | data
            null       | "war.started" | origin             | ["type": "war.started"]
            "43a0f882" | "created"     | "$origin/43a0f882" | ["type": "created", "aggregateId": "43a0f882"]
    }

    def "given event should persist it and read event stream"() {
        given:
            String aggregateId = "43a0f882"
            String type = "created"
        when:
            Event event = subj.persist(origin, aggregateId, type, "12345")
        then: "Newly created event has seq == 0"
            event.seq == 0L
        when:
            def getEvents = { Long from ->
                subj.getEvents(origin, aggregateId, from).collect({
                    [it.origin, it.aggregateId, it.seq, it.id, it.type, it.stored]
                })
            }
        then: "polling for all events on aggregate returns 1 event"
            def instant = Instant.parse(now)
            getEvents(null) == [[origin, aggregateId, 0, event.id, type, instant]]
            getEvents(-1) == [[origin, aggregateId, 0, event.id, type, instant]]
        and: "polling for events after seq=0 returns no event"
            getEvents(0) == []
        when:
            Event event2 = subj.persist(origin, aggregateId, type, "12345")
        then: "polling for all events on aggregate returns 2 events"
            getEvents(null) == [
                    [origin, aggregateId, 0, event.id, type, instant],
                    [origin, aggregateId, 1, event2.id, type, instant]
            ]
            getEvents(-1) == [
                    [origin, aggregateId, 0, event.id, type, instant],
                    [origin, aggregateId, 1, event2.id, type, instant]
            ]
        and: "polling for events after seq=0 returns 1 event"
            getEvents(0) == [
                    [origin, aggregateId, 1, event2.id, type, instant]
            ]
    }

    def "given multiple events should persist and read them stream"() {
        given:
            def collector = { [it.origin, it.aggregateId, it.id, it.type] }
        when:
            Event global = subj.persist(origin, null, "global", "12345")
            Event one = subj.persist(origin, "111", "local", "111-123")
            Event two = subj.persist(origin, "222", "local", "222-123")
        then:
            getEvents(origin, null, collector) == [[origin, null, global.id, "global"]]
            getEvents(origin, "111", collector) == [[origin, "111", one.id, "local"]]
            getEvents(origin, "222", collector) == [[origin, "222", two.id, "local"]]
    }


    def "given multiple events should persist them"() {
        given:
            ProtoEvent[] events = [
                    ProtoEvent.of("turkey", "alpha", "evaporated", "622"),
                    ProtoEvent.of("chicken", "beta", "fried", null),
                    ProtoEvent.of("chicken", "beta", "decapitated", null),
            ] as ProtoEvent[]
        when:
            subj.persist("1", "2", events)
        then:
            getEvents("turkey", "alpha", { [it.type, it.payload] }) == [["evaporated", "622"]]
            getEvents("chicken", "beta", { it.type }) == ["fried", "decapitated"]
    }
    
    @Unroll
    def "subscribe(#since) should return #expected"() {
        given:
            def date = "2002-06-01"
            TestClock clock = TestClock.of(date, "05:30")
            FileEventStore subj = new GsonFileEventStore(clock, root)
            Closure<Event> persist = { String time, String aggregateId, String type ->
                clock.adjust(time)
                return subj.persist(origin, aggregateId, type, null)
            }
        and:
            persist('05:30', "miles", "born")
            persist('06:00', "miles", "wake")
            persist('06:00', "sonic", "born")
            persist('08:00', "sonic", "wake")
            persist('08:00', "scourge", "wake")
            persist('08:30', "sonic", "run")
            persist('09:15', "amy", "wake")
            persist('10:00', "miles", "jump")
            persist('11:00', "sonic", "eat")
        and:
            clock.adjust('11:45')
            def throwingCallback = { throw new RuntimeException("no callback invokation expected") }
        when:
            Spliterator<Event> result = subj.subscribe(origin, "wake", TestClock.instant(date, since), throwingCallback)
            def events = StreamSupport.stream(result, false).collect({ [time(it.stored), it.aggregateId] }).sort()
        then:
            events == expected.sort()
        where:
            since   | expected
            '05:50' | [["06:00", "miles"], ["08:00", "scourge"], ["08:00", "sonic"], ["09:15", "amy"]]
            '06:00' | [["08:00", "scourge"], ["08:00", "sonic"], ["09:15", "amy"]]
            '08:00' | [["09:15", "amy"]]
            '08:30' | [["09:15", "amy"]]
    }

    def "subscribe to event on empty store"() {
        given: "empty store"
            def date = "2002-06-01"
            TestClock clock = TestClock.of(date, "05:30")
            FileEventStore subj = new GsonFileEventStore(clock, root)
            AtomicInteger callbacks = new AtomicInteger(0);
        when: "subscribe to 'wake'" 
            def spliterator = subj.subscribe(origin, "wake", TestClock.instant(date, "05:30"), { callbacks.incrementAndGet() })
        then: "returned spliterator is null"
            spliterator == null
            callbacks.get() == 0
        when: "new 'wake' event is persisted then callback is invoked"
            subj.persist(origin, "miles", "wake", null)
        then: "callback is invoked"
            callbacks.get() == 1
        when: "new 'born' event is persisted then callback is invoked" 
            subj.persist(origin, "sonic", "born", null)
        then: "callback is not invoked"
            callbacks.get() == 1
    }

    def "subscribe to event on store that already has event of that type"() {
        given: "store with 'wake' event"
            def date = "2002-06-01"
            TestClock clock = TestClock.of(date, "05:30")
            FileEventStore subj = new GsonFileEventStore(clock, root)
            AtomicInteger callbacks = new AtomicInteger(0);
            subj.persist(origin, "amy", "wake", null)
        when: "subscribe to 'wake'" 
            def spliterator1 = subj.subscribe(origin, "wake", TestClock.instant(date, "05:00"), { callbacks.incrementAndGet() })
            def events = StreamSupport.stream(spliterator1, false).collect(Collectors.toList()).collect({ it.getAggregateId() })
        then: "returned spliterator is null"
            events == ["amy"]
            callbacks.get() == 0
        when: "subscribe to 'wake'"
            def spliterator2 = subj.subscribe(origin, "wake", TestClock.instant(date, "05:30"), { callbacks.incrementAndGet() })
        then: "returned spliterator is null"
            spliterator2 == null
        when: "new 'wake' event is persisted then callback is invoked"
            subj.persist(origin, "miles", "wake", null)
        then: "callback is invoked"
            callbacks.get() == 1
        when: "new 'born' event is persisted then callback is invoked"
            subj.persist(origin, "sonic", "born", null)
        then: "callback is not invoked"
            callbacks.get() == 1
    }

    def <T> List<T> getEvents(String origin, String aggregateId, Closure<T> collector) {
        return subj.getEvents(origin, aggregateId, null).collect(collector)
    }
    
    private static readFile = { Path it -> new String(Files.readAllBytes(it), StandardCharsets.UTF_8) }

    private static time = { Instant instant ->
        DateTimeFormatter.ofPattern("kk:mm").format(LocalDateTime.ofInstant(instant, ZoneOffset.UTC))
    }

}