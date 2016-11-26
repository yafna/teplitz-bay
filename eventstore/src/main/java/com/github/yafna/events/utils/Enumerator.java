package com.github.yafna.events.utils;

import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.AbstractMap.SimpleEntry;

public class Enumerator {

    /**
     * Convenience method that can be used when the annotation class being used to mark
     * fields doubles as package marker.
     *
     * @see #indexBeans(Class, Class)
     */
    public static Map<String, Class<?>> indexBeans(Class<? extends Annotation> annotation) {
        return indexBeans(annotation, annotation);
    }

    /**
     * Indexes all beans in a package that carry given annotation on their final static String fields.
     *
     * @param annotation annotation to look for on final static String fields
     * @param packageMarker specifies the location where the classes in the package where this class is
     * @return A map, where key is String field value from the annotated field and value is class name.
     */
    public static Map<String, Class<?>> indexBeans(Class<? extends Annotation> annotation, Class<?> packageMarker) {
        return getClasses(packageMarker).flatMap(
                c -> getFieldsFinalStaticString(c).filter(
                        field -> field.getAnnotation(annotation) != null
                ).map(
                        field -> toEntry(c, field)
                )
        ).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    /**
     * Indexes a stream of bean consumers into a map.
     * Note: 'consumer' mentioned here is a concept, not a specific java.util.function.Consumer interface.
     *
     * @param consumers consumer instances to index
     * @param keyFunction function that calculated key from object
     * @param index bean index map previously provided by one of the indexBeans() methods.
     * It will be verified that keys provided by keyFunction are present in this map
     * and the bean classes are compatible with consumer generic type parameter
     * @param <V> consumer type. Must generic interface with exactly one type parameter.
     */
    public static <V> Map<String, V> indexConsumers(
            Stream<V> consumers, Function<V, Optional<String>> keyFunction, Map<String, Class<?>> index
    ) {
        return consumers.map(
                o -> keyFunction.apply(o).map(
                        key -> verifyVersusIndex(index, o, key)
                ).orElse(null)
        ).filter(
                Objects::nonNull
        ).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> {
            throw new IllegalStateException("Multiple consumers detected on the same key: [" + a + "] and [" + b + "]");
        }));
    }

    private static <V> SimpleEntry<String, V> verifyVersusIndex(Map<String, Class<?>> index, V object, String key) {
        Class<?> clazz = Optional.ofNullable(index.get(key)).orElseThrow(
                () -> new IllegalStateException("Unknown key [" + key + "]")
        );
        Type type = Stream.of(object.getClass().getGenericInterfaces()).findFirst().orElse(null);
        Class<?> parameter = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
        if (parameter.isAssignableFrom(clazz)) {
            return new SimpleEntry<>(key, object);
        } else {
            throw new IllegalStateException("Class [" + clazz.getName() + "] is not assignable from [" + parameter.getName() + "]");
        }
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param packageMarker
     */
    public static Stream<Class<?>> getClasses(Class<?> packageMarker) {
        ClassLoader classLoader = packageMarker.getClassLoader();
        String packageName = packageMarker.getPackage().getName();
        return getClasses(packageName, classLoader);
    }

    @SneakyThrows(IOException.class)
    private static Stream<Class<?>> getClasses(String packageName, ClassLoader classLoader) {
        Enumeration<URL> resources = classLoader.getResources(packageName.replace('.', '/'));
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        return dirs.stream().flatMap(dir -> findClasses(dir, packageName));
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param directory The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     */
    private static Stream<Class<?>> findClasses(File directory, String packageName) {
        return Optional.ofNullable(directory.listFiles()).map(
                files -> Stream.of(files).flatMap(file -> {
                    if (file.isDirectory()) {
                        assert !file.getName().contains(".");
                        return findClasses(file, packageName + "." + file.getName());
                    } else if (file.getName().endsWith(".class")) {
                        try {
                            return Stream.of(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    } else {
                        return Stream.empty();
                    }
                })
        ).orElse(Stream.empty());
    }

    private static <T> Stream<Field> getFieldsFinalStaticString(Class<T> clazz) {
        return Stream.of(clazz.getFields()).filter(
                Enumerator::isStaticFinal
        ).filter(f -> String.class.isAssignableFrom(f.getType()));
    }

    private static boolean isStaticFinal(Field f) {
        int modifiers = f.getModifiers();
        return Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers);
    }

    @SneakyThrows(IllegalAccessException.class)
    private static SimpleEntry<String, ? extends Class<?>> toEntry(Class<?> c, Field field) {
        return new SimpleEntry<>((String) field.get(null), c);
    }

}