package com.terraforged.mod.worldgen.biome;

import com.mojang.serialization.DynamicOps;
import com.terraforged.mod.codec.WorldGenCodec;
import net.minecraft.core.RegistryAccess;

public class SourceCodec implements WorldGenCodec<Source> {
   public <T> Source decode(DynamicOps<T> ops, T input, RegistryAccess access) {
      return new Source(0L, null, access);
   }

   public <T> T encode(Source source, DynamicOps<T> ops) {
      return (T)ops.empty();
   }
}
