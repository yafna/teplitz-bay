package com.github.yafna.events.rabbits;

import com.github.yafna.events.DomainEvent;
import com.github.yafna.events.annotations.EvType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@EvType("removed")
@Getter
@NoArgsConstructor
public class RabbitRemoved implements DomainEvent<Rabbit> {
}
