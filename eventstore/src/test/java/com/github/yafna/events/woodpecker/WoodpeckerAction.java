package com.github.yafna.events.woodpecker;

import com.github.yafna.events.DomainEvent;
import com.github.yafna.events.annotations.EvType;

@EvType("removed")
public class WoodpeckerAction implements DomainEvent<Woodpecker> {
}
