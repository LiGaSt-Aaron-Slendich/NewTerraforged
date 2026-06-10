package com.terraforged.mod.worldgen.terrain;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.data.codec.Codecs;
import com.terraforged.mod.data.codec.LazyCodec;
import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.worldgen.noise.NoiseLevels;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.noise.util.NoiseUtil;
import java.util.function.Supplier;
import net.minecraft.world.level.dimension.DimensionType;

public class TerrainLevels {
    public static final Codec<TerrainLevels> CODEC = LazyCodec.record(instance -> instance.group(Codecs.opt("auto_scale", true, Codec.BOOL).forGetter(l -> l.noiseLevels.auto), Codecs.opt("horizontal_scale", Float.valueOf(1.0f), Codec.floatRange((float)0.0f, (float)10.0f)).forGetter(l -> Float.valueOf(l.noiseLevels.scale)), Codec.intRange((int)Limits.MIN_MIN_Y, (int)0).fieldOf("min_y").forGetter(l -> l.minY), Codec.intRange((int)128, (int)Limits.MAX_MAX_Y).fieldOf("max_y").forGetter(l -> l.maxY), Codec.intRange((int)0, (int)Limits.MAX_MAX_Y).fieldOf("base_height").forGetter(l -> l.baseHeight), Codec.intRange((int)32, (int)Limits.MAX_SEA_LEVEL).fieldOf("sea_level").forGetter(l -> l.seaLevel), Codec.intRange((int)0, (int)Limits.MAX_SEA_FLOOR).fieldOf("sea_floor").forGetter(l -> l.seaFloor)).apply(instance, TerrainLevels::new));
    public static final Supplier<TerrainLevels> DEFAULT = Suppliers.memoize(() -> new TerrainLevels(true, 1.0f, -64, 480, 128, 62, 22));
    public final int minY;
    public final int maxY;
    public final int baseHeight;
    public final int seaFloor;
    public final int seaLevel;
    public final NoiseLevels noiseLevels;

    public TerrainLevels() {
        this.minY = 64;
        this.maxY = 480;
        this.baseHeight = 128;
        this.seaFloor = 22;
        this.seaLevel = 62;
        this.noiseLevels = new NoiseLevels(false, 1.0f, this.seaLevel, this.seaFloor, this.maxY, this.baseHeight);
    }

    public TerrainLevels(boolean autoScale, float scale, int minY, int maxY, int baseHeight, int seaLevel, int seaFloor) {
        this.minY = MathUtil.clamp(minY, Limits.MIN_MIN_Y, 0);
        this.maxY = MathUtil.clamp(maxY, 128, Limits.MAX_MAX_Y);
        this.seaLevel = MathUtil.clamp(seaLevel, 32, maxY >> 1);
        this.seaFloor = MathUtil.clamp(seaFloor, this.minY, this.seaLevel - 1);
        this.baseHeight = MathUtil.clamp(baseHeight, this.seaLevel, this.maxY);
        this.noiseLevels = new NoiseLevels(autoScale, scale, this.seaLevel, this.seaFloor, this.maxY, this.baseHeight);
    }

    public TerrainLevels copy() {
        return new TerrainLevels(this.noiseLevels.auto, this.noiseLevels.scale, this.minY, this.maxY, this.baseHeight, this.seaLevel, this.seaFloor);
    }

    public float getScaledHeight(float heightNoise) {
        return heightNoise * (float)this.maxY;
    }

    public float getScaledBaseLevel(float waterLevelNoise) {
        return this.noiseLevels.toHeightNoise(waterLevelNoise, 0.0f) * (float)this.maxY;
    }

    public int getHeight(float scaledHeight) {
        return NoiseUtil.floor(scaledHeight);
    }

    public String toString() {
        return "TerrainLevels{minY=" + this.minY + ", maxY=" + this.maxY + ", seaFloor=" + this.seaFloor + ", seaLevel=" + this.seaLevel + ", noiseLevels=" + this.noiseLevels + "}";
    }

    public static int getWaterLevel(int x, int z, int seaLevel, TerrainData terrainData) {
        float river = terrainData.getRiver().get(x, z);
        Terrain terrain = terrainData.getTerrain().get(x, z);
        return (terrain.isRiver() || terrain.isLake()) && river == 0.0f ? terrainData.getBaseHeight(x, z) : seaLevel;
    }

    public static class Defaults {
        public static final float SCALE = 1.0f;
        public static final int MIN_Y = -64;
        public static final int MAX_Y = 480;
        public static final int MAX_BASE_HEIGHT = 128;
        public static final int SEA_LEVEL = 62;
        public static final int SEA_FLOOR = 22;
        public static final int LEGACY_GEN_DEPTH = 256;
    }

    public static class Limits {
        public static final int MIN_MIN_Y = DimensionType.MIN_Y;
        public static final int MAX_MIN_Y = 0;
        public static final int MIN_SEA_LEVEL = 32;
        public static final int MAX_SEA_LEVEL;
        public static final int MIN_SEA_FLOOR = 0;
        public static final int MAX_SEA_FLOOR;
        public static final int MIN_MAX_Y = 128;
        public static final int MAX_MAX_Y;

        static {
            MAX_SEA_FLOOR = MAX_SEA_LEVEL = DimensionType.Y_SIZE;
            MAX_MAX_Y = DimensionType.Y_SIZE;
        }
    }
}
