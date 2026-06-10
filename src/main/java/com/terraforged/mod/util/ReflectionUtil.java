package com.terraforged.mod.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.function.Predicate;

public class ReflectionUtil {
    public static MethodHandle field(Class<?> owner, Class<?> type, String ... names) {
        try {
            Field field = ReflectionUtil.getField(owner, type, f -> ReflectionUtil.contains(names, f.getName()));
            return ReflectionUtil.lookup(owner).unreflectGetter(field);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static MethodHandle setter(Class<?> owner, Class<?> type, String ... names) {
        try {
            Field field = ReflectionUtil.getField(owner, type, f -> ReflectionUtil.contains(names, f.getName()));
            return ReflectionUtil.lookup(owner).unreflectSetter(field);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static MethodHandles.Lookup lookup(Class<?> owner) throws IllegalAccessException {
        try {
            return MethodHandles.privateLookupIn(owner, MethodHandles.lookup());
        }
        catch (IllegalAccessException e) {
            return MethodHandles.lookup().in(owner);
        }
    }

    public static Field getField(Class<?> owner, Class<?> fieldType, Predicate<Field> predicate) {
        return (Field)ReflectionUtil.accessMember(owner, fieldType, owner.getDeclaredFields(), Field::getType, predicate);
    }

    public static <T extends AccessibleObject> T accessMember(Class<?> owner, Class<?> type, T[] members, Function<T, Class<?>> typeGetter, Predicate<T> predicate) {
        for (T member : members) {
            if (typeGetter.apply(member) != type || !predicate.test(member)) continue;
            ((AccessibleObject)member).setAccessible(true);
            return member;
        }
        throw new IllegalStateException("Unable to find matching member in class " + owner);
    }

    private static <T> boolean contains(T[] array, T value) {
        if (array.length > 0) {
            for (T t : array) {
                if (!t.equals(value)) continue;
                return true;
            }
            return false;
        }
        return true;
    }
}
