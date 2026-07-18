package com.terraforged.mod.util.map;

import java.util.function.IntFunction;
import java.util.function.Supplier;

public class ObjectMap<T> {
   private final Index index;
   private final T[] data;

   public ObjectMap(IntFunction<T[]> constructor) {
      this.index = Index.CHUNK;
      this.data = (T[])((Object[])constructor.apply(256));
   }

   public ObjectMap(int border, IntFunction<T[]> constructor) {
      int i = 16 + border * 2;
      this.index = Index.borderedChunk(border);
      this.data = (T[])((Object[])constructor.apply(i * i));
   }

   public Index getIndex() {
      return this.index;
   }

   public T get(int x, int z) {
      return this.get(this.index.of(x, z));
   }

   public void set(int x, int z, T value) {
      this.set(this.index.of(x, z), value);
   }

   public T get(int index) {
      return this.data[index];
   }

   public void set(int index, T value) {
      this.data[index] = value;
   }

   public void fill(Supplier<T> supplier) {
      for (int i = 0; i < this.data.length; i++) {
         this.data[i] = supplier.get();
      }
   }
}
