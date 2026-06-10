package com.terraforged.mod.worldgen.biome;

import com.mojang.serialization.DynamicOps;
import com.terraforged.mod.data.codec.WorldGenCodec;
import com.terraforged.mod.worldgen.GeneratorPreset;
import com.terraforged.mod.worldgen.biome.Source;
import net.minecraft.core.RegistryAccess;

public class SourceCodec
implements WorldGenCodec<Source> {
    @Override
    public <T> Source decode(DynamicOps<T> ops, T input, RegistryAccess access) {
        return GeneratorPreset.createBiomeSource(0L, null, access);
    }

    @Override
    public <T> T encode(Source source, DynamicOps<T> ops) {
        return (T)ops.empty();
    }
}
