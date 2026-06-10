package com.terraforged.mod.worldgen.asset;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.terraforged.mod.data.codec.LazyCodec;
import com.terraforged.mod.registry.lazy.LazyTag;
import com.terraforged.mod.util.seed.ContextSeedable;
import com.terraforged.mod.worldgen.biome.viability.Viability;
import com.terraforged.mod.worldgen.biome.viability.ViabilityCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public class VegetationConfig
implements ContextSeedable<VegetationConfig> {
    public static final VegetationConfig NONE = new VegetationConfig(0.0f, 0.0f, 0.0f, (LazyTag<Biome>)null, Viability.NONE);
    public static final Codec<VegetationConfig> CODEC = LazyCodec.record(instance -> instance.group(Codec.FLOAT.optionalFieldOf("frequency", Float.valueOf(1.0f)).forGetter(VegetationConfig::frequency), Codec.FLOAT.optionalFieldOf("jitter", Float.valueOf(1.0f)).forGetter(VegetationConfig::jitter), Codec.FLOAT.optionalFieldOf("density", Float.valueOf(1.0f)).forGetter(VegetationConfig::density), TagKey.hashedCodec(Registry.BIOME_REGISTRY).fieldOf("biomes").forGetter(VegetationConfig::biomes), ViabilityCodec.CODEC.fieldOf("viability").forGetter(VegetationConfig::viability)).apply(instance, VegetationConfig::new));
    private final float frequency;
    private final float jitter;
    private final float density;
    private final LazyTag<Biome> biomes;
    private final Viability viability;

    private VegetationConfig(float frequency, float jitter, float density, TagKey<Biome> biomes, Viability viability) {
        this(frequency, jitter, density, LazyTag.of(biomes), viability);
    }

    public VegetationConfig(float frequency, float jitter, float density, LazyTag<Biome> biomes, Viability viability) {
        this.frequency = frequency;
        this.jitter = jitter;
        this.density = density;
        this.biomes = biomes;
        this.viability = viability;
    }

    @Override
    public VegetationConfig withSeed(long seed) {
        Viability viability = this.withSeed(seed, this.viability(), Viability.class);
        return new VegetationConfig(this.frequency, this.jitter, this.density, this.biomes, viability);
    }

    public TagKey<Biome> biomes() {
        return (TagKey)this.biomes.get();
    }

    public float frequency() {
        return this.frequency;
    }

    public float jitter() {
        return this.jitter;
    }

    public float density() {
        return this.density;
    }

    public Viability viability() {
        return this.viability;
    }

    public String toString() {
        return "VegetationConfig{frequency=" + this.frequency + ", jitter=" + this.jitter + ", density=" + this.density + ", biomes=" + this.biomes + ", viability=" + this.viability + "}";
    }
}
