package com.github.yafna.events.aggregate

import com.github.yafna.events.woodpecker.Woodpecker
import spock.lang.Specification

class EventScannerSpec extends Specification {
    def "given multiple handlers for the same event should throw exception"() {
        when:
            EventScanner.handlers(Woodpecker)
        then:
            thrown(IllegalStateException)
    }

    def "given multiple event classes with the same name declared should throw exception"() {
        when:
            EventScanner.events(Woodpecker)
        then:
            thrown(IllegalStateException)
    }
}
