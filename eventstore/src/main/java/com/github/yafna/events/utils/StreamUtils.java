package com.github.yafna.events.utils;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class StreamUtils {
    /**
     * Merges stream of unary operators into a single function that applies them in sequence
     * 
     * @param operations operations to be merged
     * @return Unary operator that sequentally applies functions from stream.
     * If given stream is empty, returns identity function. 
     */
    public static <T> UnaryOperator<T> fold(Stream<Function<T, T>> operations) {
        return initial -> {
            T value = initial;
            for (Iterator<Function<T, T>> it = operations.iterator(); it.hasNext(); ) {
                value = it.next().apply(value);
            }
            return value;
        };
    }

}
