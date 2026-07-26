package com.terraforged.mod.util.map;

import java.util.IdentityHashMap;
import java.util.Map;

public class Object2FloatCache<T> {
   protected final Object2FloatCache.Value[] values;
   protected final Map<T, Object2FloatCache.Value> map;

   public Object2FloatCache(int size) {
      this.values = new Object2FloatCache.Value[size];
      this.map = new IdentityHashMap<>();

      for (int i = 0; i < this.values.length; i++) {
         this.values[i] = new Object2FloatCache.Value();
      }
   }

   public void clear() {
      this.map.clear();
   }

   public void put(T t, float value) {
      int i = this.map.size();
      Object2FloatCache.Value object2floatcache$value = this.values[i];
      object2floatcache$value.value = value;
      this.map.put(t, object2floatcache$value);
   }

   public float get(T t) {
      Object2FloatCache.Value object2floatcache$value = this.map.get(t);
      return object2floatcache$value == null ? Float.NaN : object2floatcache$value.value;
   }

   protected static class Value {
      protected float value;
   }
}
