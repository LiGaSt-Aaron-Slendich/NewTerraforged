package com.terraforged.mod.worldgen.test;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record VolcanoConfig(double density, double jitter, Range radius0, Range radius1, Range radius2, Range height0, Range height1, Range fluidLevel) implements FeatureConfiguration
{
    public static final Codec<VolcanoConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.DOUBLE.fieldOf("density").forGetter(VolcanoConfig::density), Codec.DOUBLE.fieldOf("jitter").forGetter(VolcanoConfig::jitter), Range.CODEC.fieldOf("pool_radius").forGetter(VolcanoConfig::radius0), Range.CODEC.fieldOf("mouth_radius").forGetter(VolcanoConfig::radius1), Range.CODEC.fieldOf("base_radius").forGetter(VolcanoConfig::radius2), Range.CODEC.fieldOf("pool_height").forGetter(VolcanoConfig::height0), Range.CODEC.fieldOf("mouth_height").forGetter(VolcanoConfig::height1), Range.CODEC.fieldOf("fluid_level").forGetter(VolcanoConfig::fluidLevel)).apply(instance, VolcanoConfig::new));

    public boolean validBiome(Holder<Biome> biome) {
        return true;
    }

    public double scale() {
        return (double)this.radius2.max * 1.0;
    }

    public static VolcanoConfig defaultConfig() {
        return new VolcanoConfig(1.0, 0.8, new Range(5, 15), new Range(20, 30), new Range(100, 200), new Range(40, 60), new Range(100, 200), new Range(70, 80));
    }

    public record Range(int min, int max) {
        public static final Codec<Range> CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.INT.fieldOf("min").forGetter(Range::min), Codec.INT.fieldOf("max").forGetter(Range::max)).apply(instance, Range::new));

        public double get(double rand) {
            return Mth.lerp((double)rand, (double)this.min, (double)this.max);
        }
    }
}
