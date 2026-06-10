package com.terraforged.mod.worldgen.asset;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.data.codec.LazyCodec;
import com.terraforged.mod.util.seed.ContextSeedable;
import com.terraforged.mod.util.storage.WeightMap;
import com.terraforged.mod.worldgen.asset.TerrainType;
import com.terraforged.mod.worldgen.noise.NoiseCodec;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import java.util.Comparator;
import net.minecraft.core.Holder;

public class TerrainNoise
implements ContextSeedable<TerrainNoise>,
WeightMap.Weighted {
    public static final TerrainNoise NONE = new TerrainNoise((Holder<TerrainType>)Holder.direct(TerrainType.NONE), 0.0f, Source.ZERO);
    public static final Comparator<TerrainNoise> COMPARATOR = Comparator.comparing(t -> t.terrain().getName());
    public static final Codec<TerrainNoise> CODEC = LazyCodec.record(instance -> instance.group(TerrainType.CODEC.fieldOf("type").forGetter(TerrainNoise::type), Codec.FLOAT.fieldOf("weight").forGetter(TerrainNoise::weight), NoiseCodec.CODEC.fieldOf("noise").forGetter(TerrainNoise::noise)).apply(instance, TerrainNoise::new));
    private static final double MIN_NOISE = 0.0196078431372549;
    private final Holder<TerrainType> type;
    private final float weight;
    private final Module noise;

    public TerrainNoise(Holder<TerrainType> type, float weight, Module noise) {
        this.type = type;
        this.weight = weight;
        this.noise = (double)noise.minValue() < 0.0196078431372549 ? noise.bias(0.0196078431372549).clamp(0.0, 1.0) : noise;
    }

    @Override
    public TerrainNoise withSeed(long seed) {
        Module heightmap = this.withSeed(seed, this.noise(), Module.class);
        return new TerrainNoise(this.type, this.weight, heightmap);
    }

    @Override
    public float weight() {
        return this.weight;
    }

    public Holder<TerrainType> type() {
        return this.type;
    }

    public Terrain terrain() {
        return ((TerrainType)this.type().value()).getTerrain();
    }

    public Module noise() {
        return this.noise;
    }

    public String toString() {
        return "TerrainConfig{type=" + this.type + ", weight=" + this.weight + ", noise=" + this.noise + "}";
    }

    static {
        NoiseCodec.init();
    }
}
