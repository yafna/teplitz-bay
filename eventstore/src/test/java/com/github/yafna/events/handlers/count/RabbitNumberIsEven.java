
package com.github.yafna.events.handlers.count;

import com.github.yafna.events.DomainEvent;
import com.github.yafna.events.annotations.EvType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@EvType(value = "numberIsEven")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RabbitNumberIsEven implements DomainEvent<Rabbits> {
    private long count;
    private String message;
}
