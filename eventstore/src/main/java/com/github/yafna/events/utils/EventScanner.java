package com.github.yafna.events.utils;

import com.github.yafna.events.Event;
import com.github.yafna.events.annotations.EvType;
import com.github.yafna.events.annotations.Handler;
import com.github.yafna.events.handlers.DomainHandler;
import com.github.yafna.events.handlers.MapDomainHandlerRegistry;
import lombok.SneakyThrows;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collector;
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
        return new MapDomainHandlerRegistry<>(withAnnotation(
                Stream.of(methods), Handler.class
        ).<SimpleEntry<String, DomainHandler<T, ?>>>flatMap(
                EventScanner::toHandlers
        ).collect(Collectors.groupingBy(
                Entry::getKey, Collectors.mapping(Entry::getValue, getDomainHandlerListCollector("two handlers to one Event Type are not allowed"))
        )));
    }

    private static <T> Collector<T, ?, List<T>> getDomainHandlerListCollector(String errorMsg) {
        return Collector.of(ArrayList::new, (list, item) -> {
            if (!list.isEmpty()) {
                throw new IllegalArgumentException(errorMsg);
            }
            list.add(item);
        }, (listR, listL) -> {
            if (listL.isEmpty()) {
                return listR;
            } else if (listR.isEmpty()) {
                return listL;
            }
            throw new IllegalArgumentException(errorMsg);
        }, Collector.Characteristics.IDENTITY_FINISH);
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
                            type.getAnnotation(EvType.class).value(),
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
    public static <T> T invoke(Method method, T object, Object... args) {
        method.invoke(object, args);
        return object;
    }

}
