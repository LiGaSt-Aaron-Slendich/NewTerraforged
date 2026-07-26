package com.terraforged.mod.platform;

import com.terraforged.mod.data.ModTags;
import com.terraforged.mod.worldgen.biome.util.matcher.BiomeMatcher;
import com.terraforged.mod.worldgen.biome.util.matcher.BiomeTagMatcher;

public interface CommonAPI {
   ApiHolder<CommonAPI> HOLDER = new ApiHolder<>(new CommonAPI() {});

   default BiomeMatcher getOverworldMatcher() {
      return new BiomeTagMatcher.Overworld(ModTags.OVERWORLD.get());
   }

   static CommonAPI get() {
      return HOLDER.get();
   }
}
