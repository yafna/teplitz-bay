package com.github.yafna.events;

import org.json.JSONException;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.Map;

@FunctionalInterface
public interface XJson<T> {
    void matches(T t);

    static XJson<Map<String, ?>> parse(String actual) throws JSONException {
        JSONObject object = new JSONObject(actual);
        return expected -> {
            try {
                JSONAssert.assertEquals(new JSONObject(expected), object, JSONCompareMode.STRICT_ORDER);
            } catch (JSONException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        };
    }
}
