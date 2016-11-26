package com.github.yafna.events.rabbits;

import com.github.yafna.events.DomainEvent;
import com.github.yafna.events.annotations.EvType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@EvType("added")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RabbitAdded implements DomainEvent<Rabbit> {
    private String name;
    private String publicKey;
}
