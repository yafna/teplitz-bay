package com.github.yafna.events.dispatcher;

import com.github.yafna.events.store.EventStore;
import com.google.gson.Gson;

/**
 * This class has the following responsibilities:
 * 1. Manage serialization and deserialization of events
 * 2. Notify pipelines about stored events
 * 3. Manage transactions if event store behind it supports them
 */
public class GsonEventDispatcher extends EventDispatcher {
    private final Gson gson = new Gson();

    public GsonEventDispatcher(EventStore store) {
        super(store);
    }

    @Override
    protected <T> String serialize(T event) {
        return gson.toJson(event);
    }

    @Override
    protected <T> T deserialize(String payload, Class<T> clazz) {
        return gson.fromJson(payload, clazz);
    }
}
