package com.terraforged.mod.util.map;

public interface Index {
   Index CHUNK = (x, z) -> (z & 15) << 4 | x & 15;

   int of(int var1, int var2);

   static Index borderedChunk(final int border) {
      return new Index() {
         private final int offset = border;
         private final int size = 16 + border * 2;

         @Override
         public int of(int x, int z) {
            x += this.offset;
            z += this.offset;
            return z * this.size + x;
         }
      };
   }
}
