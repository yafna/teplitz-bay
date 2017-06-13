package com.github.yafna.events.store.file;

import com.github.yafna.events.Event;
import com.github.yafna.events.store.EventStore;
import com.github.yafna.events.store.ProtoEvent;
import com.github.yafna.events.store.StoredEvent;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@AllArgsConstructor
public class FileEventStore implements EventStore {
    private static final Comparator<Path> BY_NAME = Comparator.comparing(Path::toString);
    private static final BiPredicate<Path, BasicFileAttributes> NOT_DIRECTORY = (p, attrs) -> !attrs.isDirectory();
    private static final String NAME_PATTERN_TIME = "{0}={1}={2}.evt";
    private static final String PATTERN_SEQ = "{0,number,00000000}";
    private static final String NAME_PATTERN_SEQ = PATTERN_SEQ + "={1}={2}={3}.evt";
    private static final Collector<Path, ?, Optional<Path>> TO_LAST = Collectors.maxBy(Comparator.comparing(Path::toString));

    private final Clock clock;
    private final File rootDir;
    private final Function<StoredEvent, byte[]> serializer;
    private final Function<byte[], StoredEvent> deserializer;

    private final ConcurrentMap<String, ConcurrentMap<String, List<Consumer<Event>>>> subscribers = new ConcurrentHashMap<>();

    /**
     * Retrieves events for a given aggregate.
     *
     * @param origin Aggregate type
     * @param aggregateId aggregate id
     * @param fromSeq event sequence number after which events should be returned
     */
    @Override
    public Stream<Event> getEvents(String origin, String aggregateId, Long fromSeq) {
        BiPredicate<Path, BasicFileAttributes> filter = Optional.ofNullable(fromSeq).map(
                seq -> NOT_DIRECTORY.and(isAfter(seq))
        ).orElse(NOT_DIRECTORY);
        Path path = getDirectory(origin, Optional.ofNullable(aggregateId));

        return Files.exists(path) ? readEvents(ensureDirectory(path), filter).sequential() : Stream.empty();
    }

    @Override
    @SneakyThrows(IOException.class)
    public Spliterator<Event> subscribe(String origin, String type, Instant since, Consumer<Event> callback) {
        Path path = path(origin);
        if (Files.exists(path)) {
            Spliterator<Event> spliterator = Files.find(
                    path, 2, isFileCreatedAfter(since, type)
            ).map(this::readEvent).filter(
                    event -> event.getType().equals(type)
            ).sorted(
                    Comparator.comparing(Event::getStored)
            ).collect(
                    // TODO Replace combination of sorted() and collect() that will load all files into memory
                    // with a limited-size collector that will only keep a limited number of first items  
                    Collectors.toList()
            ).spliterator();

            if (spliterator.getExactSizeIfKnown() != 0) {
                return spliterator;
            }
        }
        subscribers.computeIfAbsent(
                origin, o -> new ConcurrentHashMap<>()
        ).computeIfAbsent(
                type, t -> new LinkedList<>()
        ).add(callback);
        return null;
    }

    @SneakyThrows(IOException.class)
    private Stream<Event> readEvents(Path subdir, BiPredicate<Path, BasicFileAttributes> pathfff) {
        return Files.find(subdir, 1, pathfff).sorted(BY_NAME).map(this::readEvent);
    }

    @SneakyThrows(IOException.class)
    private Event readEvent(Path path) {
        return deserializer.apply(Files.readAllBytes(path));
    }

    @Override
    public Event persist(String origin, String aggregateId, String type, String payload) {
        StoredEvent event = new StoredEvent();
        event.setAggregateId(aggregateId);
        event.setOrigin(origin);
        event.setType(type);
        event.setPayload(payload);
        event.setId(generateId());
        event.setStored(clock.instant());
        return write(event);
    }

    @Override
    public long persist(String causeId, String corrId, ProtoEvent[] events) {
        Instant timestamp = clock.instant();
        List<StoredEvent> stored = Stream.of(events).map(p -> {
            StoredEvent event = new StoredEvent();
            event.setCauseId(causeId);
            event.setCorrId(corrId);
            event.setAggregateId(p.getAggregateId());
            event.setOrigin(p.getOrigin());
            event.setType(p.getType());
            event.setPayload(p.getPayload());
            event.setId(generateId());
            event.setStored(timestamp);
            return write0(event);
        }).collect(Collectors.toList());
        // Have to ensure all the events are stored before calling subscribers 
        stored.forEach(this::callSubscribers);
        return stored.size(); 
    }

    private StoredEvent write(StoredEvent event) {
        StoredEvent stored = write0(event);
        callSubscribers(stored);
        return stored;
    }

    private void callSubscribers(StoredEvent stored) {
        Optional.ofNullable(
                subscribers.get(stored.getOrigin())
        ).map(
                byType -> byType.get(stored.getType())
        ).ifPresent(
                callbacks -> callbacks.forEach(callback -> callback.accept(stored))
        );
    }

    private StoredEvent write0(StoredEvent event) {
        String dir = event.getOrigin();
        Optional<String> aggregateId = Optional.ofNullable(event.getAggregateId());
        Path directory = getDirectory(dir, aggregateId);
        Path path = ensureDirectoryExists(directory);

        String name = aggregateId.map(aid -> {
            Long seq = lastEvent(path).map(p -> getSeq(p) + 1).orElse(0L);
            event.setSeq(seq);
            return MessageFormat.format(NAME_PATTERN_SEQ, seq, event.getStored(), event.getId(), event.getType());
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

    /**
     * Extracts sequence number form event file name.
     */
    private static Long getSeq(Path path) {
        return ((Number) parse(path)[0]).longValue();
    }

    private static Object[] parse(Path path) {
        String name = path.toString();
        try {
            return new MessageFormat(NAME_PATTERN_SEQ).parse(name);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Error parsing file path: " + name, e);
        }
    }

    private static String generateId() {
        return UUID.randomUUID().toString();
    }

    private static Path ensureDirectoryExists(Path directory) {
        if (Files.exists(directory)) {
            return ensureDirectory(directory);
        } else {
            try {
                return Files.createDirectories(directory);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create dir: [" + String.valueOf(directory) + "]", e);
            }
        }
    }

    private static Path ensureDirectory(Path path) {
        if (Files.isDirectory(path)) {
            return path;
        } else {
            throw new IllegalStateException("Must be a directory: [" + String.valueOf(path) + "]");
        }
    }

    private static BiPredicate<Path, BasicFileAttributes> isAfter(Long seq) {
        String prefix = MessageFormat.format(PATTERN_SEQ, seq);
        return (file, attrs) -> {
            String name = file.getFileName().toString();
            return name.compareTo(prefix) > 0 && !name.startsWith(prefix);
        };
    }

    private static BiPredicate<Path, BasicFileAttributes> isFileCreatedAfter(Instant since, String type) {
        String sinceString = since.toString();
        return (path, attributes) -> {
            if (Files.isDirectory(path)) {
                return false;
            } else {
                Object[] parse = parse(path.getFileName());
                return ((String) parse[1]).compareTo(sinceString) > 0 && Objects.equals(type, (String) parse[3]);
            }
        };
    }

    private static String formatTime(Instant stored) {
        return String.valueOf(stored).replace(":", "-");
    }


}
