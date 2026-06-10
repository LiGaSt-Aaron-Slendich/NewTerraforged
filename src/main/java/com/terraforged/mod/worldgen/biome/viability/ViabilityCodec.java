package com.terraforged.mod.worldgen.biome.viability;

import com.mojang.serialization.Codec;
import com.terraforged.mod.data.codec.SuperCodec;
import com.terraforged.mod.data.util.DataUtil;
import com.terraforged.mod.worldgen.biome.viability.BiomeEdgeViability;
import com.terraforged.mod.worldgen.biome.viability.HeightViability;
import com.terraforged.mod.worldgen.biome.viability.MultViability;
import com.terraforged.mod.worldgen.biome.viability.NoiseViability;
import com.terraforged.mod.worldgen.biome.viability.SaturationViability;
import com.terraforged.mod.worldgen.biome.viability.SlopeViability;
import com.terraforged.mod.worldgen.biome.viability.SumViability;
import com.terraforged.mod.worldgen.biome.viability.Viability;

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
