package com.github.yafna.events.aggregate;

import com.github.yafna.events.DomainEvent;
import com.github.yafna.events.annotations.EvType;
import com.github.yafna.events.annotations.Origin;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Groups methods to deduce things from event payload.
 */
public class PayloadUtils {

    /**
     * Resolves event type from payload class.
     * @param clazz event payload type
     */
    public static EvType eventType(Class<?> clazz) {
        return findAnnotation(clazz, EvType.class);
    }

    /**
     * Resolves origin from payload class.
     * @param clazz event payload type
     */
    public static <T extends DomainEvent<?>> String origin(Class<? extends T> clazz) {
        return findAnnotation(getDomain(clazz), Origin.class).value();
    }

    private static <T extends DomainEvent<?>> Class<?> getDomain(Class<T> eventType) {
        for (Class<?> c = eventType; c != Object.class; c = c.getSuperclass()) {
            for (Type t : c.getGenericInterfaces()) {
                if (t instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) t;
                    Type raw = pt.getRawType();
                    if (raw instanceof Class) {
                        if (((Class<?>) raw).isAssignableFrom(DomainEvent.class)) {
                            return (Class<?>) pt.getActualTypeArguments()[0];
                        }
                    }
                }
            }
        }
        throw new IllegalArgumentException("Unable to resolve domain [" + eventType.getName() + "]");
    }

    private static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationClass) {
        for (Class<?> c = clazz; c != Object.class; c = c.getSuperclass()) {
            A annotation = c.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        throw new IllegalArgumentException("Unable to resolve origin for [" + clazz.getName() + "]");
    }

}
