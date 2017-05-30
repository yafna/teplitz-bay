package com.github.yafna.events.store.file;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.github.yafna.events.store.StoredEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

public class GsonFileEventStore extends FileEventStore {
    private final static Gson gson = Converters.registerAll(new GsonBuilder()).create();

    public GsonFileEventStore(Clock clock, File rootDir) {
        this(clock, rootDir, 1);
    }

    public GsonFileEventStore(Clock clock, File rootDir, long recapWindow) {
        super(clock, rootDir, recapWindow, GsonFileEventStore::toBytes, GsonFileEventStore::parse);
    }

    private static StoredEvent parse(byte[] bytes) {
        return gson.fromJson(new String(bytes, StandardCharsets.UTF_8), StoredEvent.class);
    }

    private static byte[] toBytes(StoredEvent event) {
        return gson.toJson(event).getBytes(StandardCharsets.UTF_8);
    }
}
