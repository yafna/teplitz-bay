package com.github.yafna.events.aggregate;

import com.github.yafna.events.annotations.EvType;

import java.lang.annotation.Annotation;

public class AggregateUtils {

    /**
     * Resolves event type from payload class.
     * @param clazz event payload type
     */
    public static EvType eventType(Class<?> clazz) {
        return findAnnotation(clazz, EvType.class);
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
