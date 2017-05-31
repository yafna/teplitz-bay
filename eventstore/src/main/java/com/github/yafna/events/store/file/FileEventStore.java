package com.github.yafna.events.store.file;

import com.github.yafna.events.Event;
import com.github.yafna.events.store.EventStore;
import com.github.yafna.events.store.StoredEvent;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@AllArgsConstructor
public class FileEventStore implements EventStore {
    private static final Comparator<Path> BY_NAME = Comparator.comparing(Path::toString);
    private static final Predicate<Path> NOT_DIRECTORY = path -> !Files.isDirectory(path);
    private static final String NAME_PATTERN_TIME = "{0}={1}={2}.evt";
    private static final String PATTERN_SEQ = "{0,number,00000000}";
    private static final String NAME_PATTERN_SEQ = PATTERN_SEQ + "={1}={2}.evt";
    private static final Collector<Path, ?, Optional<Path>> TO_LAST = Collectors.maxBy(Comparator.comparing(Path::toString));

    private final Clock clock;
    private final File rootDir;
    private final long recapWindow;
    private final Function<StoredEvent, byte[]> serializer;
    private final Function<byte[], StoredEvent> deserializer;

    /**
     * Retrieves events for a given aggregate.
     *
     * @param origin      Aggregate type
     * @param aggregateId aggregate id
     * @param fromSeq     event sequence number after which events should be returned
     */
    @Override
    public Stream<Event> getEvents(String origin, String aggregateId, Long fromSeq) {
        Predicate<Path> filter = Optional.ofNullable(fromSeq).map(FileEventStore::isAfter).orElse(f -> true);
        Path path = getDirectory(origin, Optional.ofNullable(aggregateId));
        return exists(path).map(
                p -> readEvents(p, filter).sequential()
        ).orElseGet(Stream::empty);
    }

    @Override
    @SneakyThrows(IOException.class)
    public List<Event> subscribe(String origin, String type, Instant since, Consumer<Event> callback) {
        List<Event> events = Files.walk(path(origin)).filter(NOT_DIRECTORY).map(this::readEvent).filter(
                event -> event.getStored().isAfter(since) && event.getType().equals(type)
        ).sorted(
                Comparator.comparing(Event::getStored)
        ).collect(Collectors.toList());
        List<Event> result = new ArrayList<>();
        int number = 0;
        Instant instant = events.isEmpty()? null:events.get(0).getStored();
        for (int i = 0; i < events.size() && number < recapWindow ; ++i) {
            if (!instant.equals(events.get(i).getStored())) {
                instant = events.get(i).getStored();
                number++;
                if (result.size() >= recapWindow){
                    return  result;
                }
            }
            if(number < recapWindow){
                result.add(events.get(i));
            }
        }
        return result;
    }

    @SneakyThrows(IOException.class)
    private Stream<Event> readEvents(Path subdir, Predicate<Path> filter) {
        return Files.list(subdir).filter(NOT_DIRECTORY).filter(filter).sorted(BY_NAME).map(this::readEvent);
    }

    @SneakyThrows(IOException.class)
    private Event readEvent(Path path) {
        return deserializer.apply(Files.readAllBytes(path));
    }

    @Override
    public Persister persist(String aggregateId) {
        return fn(this::create).then(
                ev -> ev.setAggregateId(aggregateId)
        ).then(this::write);
    }

    @Override
    public Persister persist(String causeId, String corrId, String aggregateId) {
        return fn(this::create).then(
                addCorrelation(causeId, corrId)
        ).then(
                ev -> ev.setAggregateId(aggregateId)
        ).then(this::write);
    }

    @Override
    public Persister persist(String causeId, String corrId) {
        return fn(this::create).then(
                addCorrelation(causeId, corrId)
        ).then(this::write);
    }

    @Override
    public Persister persist() {
        return fn(this::create).then(this::write);
    }

    private StoredEvent create(String origin, String type, String payload) {
        StoredEvent event = new StoredEvent();
        event.setId(generateId());
        event.setOrigin(origin);
        event.setType(type);
        event.setStored(clock.instant());
        event.setPayload(payload);
        return event;
    }

    private StoredEvent write(StoredEvent event) {
        String dir = event.getOrigin();
        Optional<String> aggregateId = Optional.ofNullable(event.getAggregateId());
        Path directory = getDirectory(dir, aggregateId);
        Path path = exists(directory).orElseGet(() -> mkDirs(directory));

        String name = aggregateId.map(aid -> {
            Long seq = lastEvent(path).map(this::getSeqFromFileEvent).orElse(0L);
            event.setSeq(seq);
            return MessageFormat.format(NAME_PATTERN_SEQ, seq, event.getId(), event.getType());
        }).orElseGet(() -> MessageFormat.format(
                NAME_PATTERN_TIME, formatTime(event.getStored()), event.getId(), event.getType()
        ));
        Path file = path.resolve(name);
        log.info("Writing:\n    {}", file.toString());
        try {
            Files.write(file, serializer.apply(event));
            return event;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write [" + String.valueOf(file) + "]");
        }
    }

    private Path getDirectory(String dir, Optional<String> aggregateId) {
        Path aggregateDir = path(dir);
        return aggregateId.map(aggregateDir::resolve).orElse(aggregateDir);
    }

    private Path path(String dir) {
        return new File(rootDir, dir).toPath();
    }

    private static Optional<Path> lastEvent(Path dir) {
        try {
            return Files.list(dir).map(Path::getFileName).collect(TO_LAST);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to list [" + String.valueOf(dir) + "]");
        }
    }

    private Long getSeqFromFileEvent(Path p) {
        String name = p.toString();
        try {
            Object[] parsed = new MessageFormat(NAME_PATTERN_SEQ).parse(name);
            return ((Number) parsed[0]).longValue() + 1;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Error parsing file path: " + name, e);
        }
    }

    private static String generateId() {
        return UUID.randomUUID().toString();
    }

    private static Optional<Path> exists(Path path) {
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                return Optional.of(path);
            } else {
                throw new IllegalStateException("Must be a directory: [" + String.valueOf(path) + "]");
            }
        } else {
            return Optional.empty();
        }
    }

    private static Path mkDirs(Path path) {
        try {
            return Files.createDirectories(path);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create dir: [" + String.valueOf(path) + "]", e);
        }
    }

    private static Consumer<StoredEvent> addCorrelation(String causeId, String corrId) {
        return ev -> {
            ev.setCauseId(causeId);
            ev.setCorrId(corrId);
        };
    }

    private static Predicate<Path> isAfter(Long v) {
        String prefix = MessageFormat.format(PATTERN_SEQ, v);
        return file -> {
            String name = file.getFileName().toString();
            return name.compareTo(prefix) > 0 && !name.startsWith(prefix);
        };
    }

    private static Persister fn(Persister create) {
        return create;
    }

    private static String formatTime(Instant stored) {
        return String.valueOf(stored).replace(":", "-");
    }

}
