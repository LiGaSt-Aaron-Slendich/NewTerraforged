package com.terraforged.mod.util.map;

public class FloatMap {
   private final Index index;
   private final float[] data;

   public FloatMap() {
      this.index = Index.CHUNK;
      this.data = new float[256];
   }

   public FloatMap(int border) {
      int i = 16 + border * 2;
      this.index = Index.borderedChunk(border);
      this.data = new float[i * i];
   }

   public Index index() {
      return this.index;
   }

   public float get(int x, int z) {
      return this.get(this.index.of(x, z));
   }

   public void set(int x, int z, float value) {
      this.set(this.index.of(x, z), value);
   }

   public float get(int index) {
      return this.data[index];
   }

   public void set(int index, float value) {
      this.data[index] = value;
   }
}
