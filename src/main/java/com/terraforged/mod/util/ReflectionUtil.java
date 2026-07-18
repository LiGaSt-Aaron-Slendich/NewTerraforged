package com.terraforged.mod.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.function.Function;
import java.util.function.Predicate;

public class ReflectionUtil {
   public static MethodHandle field(Class<?> owner, Class<?> type, String... names) {
      try {
         Field field = getField(owner, type, f -> contains(names, f.getName()));
         return MethodHandles.lookup().in(owner).unreflectGetter(field);
      } catch (IllegalAccessException illegalaccessexception) {
         throw new RuntimeException(illegalaccessexception);
      }
   }

   public static Field getField(Class<?> owner, Class<?> fieldType, Predicate<Field> predicate) {
      return accessMember(owner, fieldType, owner.getDeclaredFields(), Field::getType, predicate);
   }

   public static <T extends AccessibleObject & Member> T accessMember(
      Class<?> owner, Class<?> type, T[] members, Function<T, Class<?>> typeGetter, Predicate<T> predicate
   ) {
      for (T t : members) {
         if (typeGetter.apply(t) == type && predicate.test(t)) {
            t.setAccessible(true);
            return t;
         }
      }

      throw new IllegalStateException("Unable to find matching member in class " + owner);
   }

   private static <T> boolean contains(T[] array, T value) {
      if (array.length > 0) {
         for (T t : array) {
            if (t.equals(value)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }
}
