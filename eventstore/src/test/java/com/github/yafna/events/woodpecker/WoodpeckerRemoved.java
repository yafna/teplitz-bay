package com.github.yafna.events.woodpecker;

import com.github.yafna.events.DomainEvent;
import com.github.yafna.events.annotations.EvType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@EvType("removed")
@Getter
@NoArgsConstructor
public class WoodpeckerRemoved implements DomainEvent<Woodpecker> {
}
