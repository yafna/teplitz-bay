package com.github.yafna.events.aggregate;

import com.github.yafna.events.rabbits.RabbitAdded;
import org.junit.Assert;
import org.junit.Test;

public class PayloadUtilsTest {
    private final static RabbitAdded EVENT = new RabbitAdded("Bill", "Longear") {};

    @Test
    public void typeNormal() {
        Assert.assertEquals("added", PayloadUtils.eventType(RabbitAdded.class).value());
    }

    @Test
    public void typeAnonymousl() {
        Assert.assertEquals("added", PayloadUtils.eventType(EVENT.getClass()).value());
    }

    @Test
    public void originNormal() {
        Assert.assertEquals("rabbit", PayloadUtils.origin(RabbitAdded.class));
    }

    @Test
    public void originAnonymous() {
        Assert.assertEquals("rabbit", PayloadUtils.origin(EVENT.getClass()));
    }
}