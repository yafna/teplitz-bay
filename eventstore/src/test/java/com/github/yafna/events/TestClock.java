package com.github.yafna.events;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Clock that can be adjusted externally
 */
public class TestClock extends Clock {

    private Clock delegate;

    public static TestClock of(String date, String time) {
        return new TestClock(date, time);
    }
    
    private TestClock(String date, String time) {
        this.delegate = Clock.fixed(instant(date, time), ZoneId.of("UTC"));
    }

    public void adjust(Duration dt) {
        delegate = Clock.offset(delegate, dt);
    }

    @Override
    public Instant instant() {
        return delegate.instant();
    }

    @Override
    public ZoneId getZone() {
        return delegate.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return delegate.withZone(zone);
    }

    public static Instant instant(String date, String time) {
        return Instant.parse(date + "T" + time + ":00Z");
    }
}
