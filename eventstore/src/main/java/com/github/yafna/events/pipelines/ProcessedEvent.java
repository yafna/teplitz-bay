package com.github.yafna.events.pipelines;

import com.github.yafna.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProcessedEvent<T> {
    Event event;
    T payload;
}
