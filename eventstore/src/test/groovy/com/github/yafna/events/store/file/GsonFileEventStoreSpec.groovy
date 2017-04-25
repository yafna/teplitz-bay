package com.github.yafna.events.store.file

import com.github.yafna.events.Event
import com.github.yafna.events.XJson
import com.github.yafna.events.store.file.FileEventStore
import com.github.yafna.events.store.file.GsonFileEventStore
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

public class GsonFileEventStoreSpec extends Specification {
    private final static String origin = "hedgehog"

    File root = File.createTempDir()
    String now = "2002-05-19T22:33:11Z"

    Clock clock = Clock.fixed(Instant.parse(now), ZoneId.of("UTC"))
    GsonFileEventStore subj = new GsonFileEventStore(clock, root)

    @Unroll
    def "given event [#type] should persist it under [#subdir]"() {
        given:
            String now = "2002-05-19T22:33:11Z"
            String origin = "hedgehog"
        when:
            Event event = method(subj).apply(origin, type, "12345")
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
            aggregate  | type          | method                     | subdir             | data
            null       | "war.started" | { it.persist() }           | origin             | ["type": "war.started"]
            "43a0f882" | "created"     | { it.persist("43a0f882") } | "$origin/43a0f882" | ["type": "created", "aggregateId": "43a0f882"]

    }

    def "given event should persist it and read event stream"() {
        given:
            String aggregateId = "43a0f882"
            String type = "created"
        when:
            Event event = subj.persist(aggregateId).apply(origin, type, "12345")
        then: "Newly created event has seq == 0"
            event.seq == 0L
        when:
            def getEvents = {Long from -> subj.getEvents(origin, aggregateId, from).collect({
                [it.origin, it.aggregateId, it.seq, it.id, it.type, it.stored]
            })}
        then: "polling for all events on aggregate returns 1 event"
            def instant = Instant.parse(now)
            getEvents(null) == [[origin, aggregateId, 0, event.id, type, instant]]
            getEvents(-1) == [[origin, aggregateId, 0, event.id, type, instant]]
        and: "polling for events after seq=0 returns no event"
            getEvents(0) == []
        when:
            Event event2 = subj.persist(aggregateId).apply(origin, type, "12345")
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
            def getEvents = { String aggregateId -> subj.getEvents(origin, aggregateId, null).collect({
                [it.origin, it.aggregateId, it.id, it.type]
            })}
        when:
            Event global = subj.persist().apply(origin, "global", "12345")
            Event one = subj.persist("111").apply(origin, "local", "111-123")
            Event two = subj.persist("222").apply(origin, "local", "222-123")
        then:
            getEvents(null) == [[origin, null, global.id, "global"]]
            getEvents("111") == [[origin, "111", one.id, "local"]]
            getEvents("222") == [[origin, "222", two.id, "local"]]
    }


    private static readFile = { Path it -> new String(Files.readAllBytes(it), StandardCharsets.UTF_8) }


}