package com.terraforged.mod.worldgen.noise;

import com.mojang.serialization.Codec;
import com.terraforged.cereal.spec.DataSpecs;
import com.terraforged.engine.module.Ridge;
import com.terraforged.mod.data.codec.SuperCodec;
import com.terraforged.noise.Module;
import com.terraforged.noise.util.NoiseSpec;

public class NoiseCodec {
    public static final Codec<Module> CODEC = SuperCodec.withoutValidator(Module.class).stable();

    private NoiseCodec() {
    }

    public static void init() {
    }

    static {
        NoiseSpec.init();
        DataSpecs.register(Ridge.spec());
    }
}
