package com.github.yafna.events.aggregate;

import com.github.yafna.events.Event;
import com.github.yafna.events.annotations.EvType;
import com.github.yafna.events.annotations.Handler;
import com.github.yafna.events.handlers.DomainHandler;
import com.github.yafna.events.handlers.MapDomainHandlerRegistry;
import com.github.yafna.events.utils.Enumerator;
import lombok.SneakyThrows;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class EventScanner {
    public static Map<String, Class<?>> events(Class<?> packageMarker) {
        return withAnnotation(Enumerator.getClasses(packageMarker), EvType.class).collect(
                Collectors.toMap((entry) -> entry.getKey().value(), Entry::getValue)
        );
    }

    public static <T> MapDomainHandlerRegistry<T> handlers(Class<T> clazz) {
        Method[] methods = clazz.getMethods();
        Map<String, List<DomainHandler<T, ?>>> handlers = withAnnotation(
                Stream.of(methods), Handler.class
        ).<SimpleEntry<String, DomainHandler<T, ?>>>flatMap(
                EventScanner::toHandlers
        ).collect(Collectors.groupingBy(
                Entry::getKey, Collectors.mapping(Entry::getValue, Collectors.toList())
        ));
        handlers.forEach((key, value) -> {
            if (value.size() > 1) {
                throw new IllegalStateException(MessageFormat.format(
                        "Multiple domain handlers found for event type [{0}] on [{1}]", key, clazz.getName()
                ));
            }
        });
        return new MapDomainHandlerRegistry<>(handlers);
    }

    private static <T> Stream<SimpleEntry<String, DomainHandler<T, ?>>> toHandlers(Entry<Handler, Method> entry) {
        Method method = entry.getValue();
        Class<?>[] types = method.getParameterTypes();

        switch (types.length) {
            case 2:
                EvType annotation = types[1].getAnnotation(EvType.class);
                if (annotation != null) {
                    return Stream.of(new SimpleEntry<String, DomainHandler<T, ?>>(
                            annotation.value(),
                            (object, meta, payload) -> invoke(method, object, meta, payload)
                    ));
                } else {
                    throw new IllegalStateException(/*TODO*/);
                }
            case 1:
                Class<?> type = types[0];
                if (type.isAssignableFrom(Event.class)) {
                    // Meta only, no payload
                    return Stream.of(
                            method.getAnnotation(Handler.class).value()
                    ).map(event -> new SimpleEntry<String, DomainHandler<T, ?>>(
                            event,
                            (object, meta, payload) -> invoke(method, object, meta)
                    ));
                } else {
                    // Payload only, no metadata
                    return Stream.of(new SimpleEntry<String, DomainHandler<T, ?>>(
                            AggregateUtils.eventType(type).value(),
                            (object, meta, payload) -> invoke(method, object, payload)
                    ));
                }
            default:
                throw new IllegalStateException(/*TODO*/);
        }
    }

    private static <T extends Annotation, V extends AnnotatedElement> Stream<Entry<T, V>> withAnnotation(
            Stream<V> x, Class<T> clazz
    ) {
        return x.<Entry<T, V>>map(
                c -> new SimpleEntry<>(c.getAnnotation(clazz), c)
        ).filter(
                entry -> entry.getKey() != null
        );
    }

    @SneakyThrows({IllegalAccessException.class, InvocationTargetException.class})
    private static <T> T invoke(Method method, T object, Object... args) {
        method.invoke(object, args);
        return object;
    }

}
