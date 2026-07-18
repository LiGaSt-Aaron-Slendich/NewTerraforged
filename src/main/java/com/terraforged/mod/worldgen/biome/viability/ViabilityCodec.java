package com.terraforged.mod.worldgen.biome.viability;

import com.mojang.serialization.Codec;
import com.terraforged.mod.codec.SuperCodec;
import com.terraforged.mod.util.DataUtil;

public class ViabilityCodec {
   public static final Codec<Viability> CODEC = SuperCodec.of(Viability.class).stable();

   ViabilityCodec() {
   }

   static {
      DataUtil.registerSub(Viability.class, BiomeEdgeViability.SPEC);
      DataUtil.registerSub(Viability.class, HeightViability.SPEC);
      DataUtil.registerSub(Viability.class, MultViability.SPEC);
      DataUtil.registerSub(Viability.class, NoiseViability.SPEC);
      DataUtil.registerSub(Viability.class, SaturationViability.SPEC);
      DataUtil.registerSub(Viability.class, SlopeViability.SPEC);
      DataUtil.registerSub(Viability.class, SumViability.SPEC);
   }
}
