package com.github.yafna.events.rabbits;

import com.github.yafna.events.DomainEvent;
import com.github.yafna.events.annotations.EvType;

@EvType("init")
public class RabbitInit implements DomainEvent<Rabbit> {
}
