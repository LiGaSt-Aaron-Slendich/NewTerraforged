package com.terraforged.mod.worldgen.asset;

import com.mojang.serialization.Codec;
import com.terraforged.mod.data.codec.LazyCodec;
import com.terraforged.mod.util.seed.ContextSeedable;
import com.terraforged.mod.worldgen.cave.CaveNoise;
import com.terraforged.mod.worldgen.cave.CavePlacementType;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.noise.NoiseCodec;
import com.terraforged.noise.Module;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Конфіг однієї NoiseCave-системи.
 *
 * Нові поля порівняно з оригіналом:
 *   placement_type – FULL_REGION | CEILING_PATCH | ISLAND_PATCH
 *
 * Для type = MEGA або GIGA масштаб береться з CaveSystemConfig
 * (mega_scale / giga_scale), а не з цього файлу.
 *
 * Для CEILING_PATCH:
 *   Верхня частина печери [ceiling_patch_min*height .. ceiling_patch_max*height]
 *   залежно від вологості/температури/висоти поверхневого біому.
 *
 * Для ISLAND_PATCH:
 *   Острівці-гриби на підлозі. Висота = 1.5 * діаметр (2*radius).
 *   island_max_radius береться з CaveSystemConfig.island_max_radius_chunks.
 */
public class NoiseCave implements ContextSeedable<NoiseCave> {
    public static final Codec<NoiseCave> CODEC = LazyCodec.record(instance -> instance.group(
            Codec.INT.optionalFieldOf("seed", 0)
                    .forGetter(c -> c.seed),
            CaveType.CODEC.fieldOf("type")
                    .forGetter(c -> c.type),
            CavePlacementType.CODEC.optionalFieldOf("placement_type", CavePlacementType.FULL_REGION)
                    .forGetter(c -> c.placementType),
            NoiseCodec.CODEC.fieldOf("elevation")
                    .forGetter(c -> c.elevation),
            NoiseCodec.CODEC.fieldOf("shape")
                    .forGetter(c -> c.shape),
            NoiseCodec.CODEC.fieldOf("floor")
                    .forGetter(c -> c.floor),
            Codec.INT.fieldOf("size")
                    .forGetter(c -> c.size),
            Codec.INT.optionalFieldOf("min_y", -32)
                    .forGetter(c -> c.minY),
            Codec.INT.fieldOf("max_y")
                    .forGetter(c -> c.maxY)
    ).apply(instance, NoiseCave::new));

    private final int seed;
    private final CaveType type;
    private final CavePlacementType placementType;
    private final Module elevation;
    private final Module shape;
    private final Module floor;
    private final int size;
    private final int minY;
    private final int maxY;
    private final int rangeY;

    public NoiseCave(int seed, CaveType type, CavePlacementType placementType,
                     Module elevation, Module shape, Module floor,
                     int size, int minY, int maxY) {
        this.seed          = seed;
        this.type          = type;
        this.placementType = placementType;
        this.elevation     = elevation;
        this.shape         = shape;
        this.floor         = floor;
        this.size          = size;
        this.minY          = minY;
        this.maxY          = maxY;
        this.rangeY        = maxY - minY;
    }

    /** Зворотно сумісний конструктор без placementType */
    public NoiseCave(int seed, CaveType type,
                     Module elevation, Module shape, Module floor,
                     int size, int minY, int maxY) {
        this(seed, type, CavePlacementType.FULL_REGION, elevation, shape, floor, size, minY, maxY);
    }

    @Override
    public NoiseCave withSeed(long seed) {
        var e = withSeed(seed, this.elevation, Module.class);
        var s = withSeed(seed, this.shape,     Module.class);
        var f = withSeed(seed, this.floor,     Module.class);
        return new NoiseCave(this.seed, type, placementType, e, s, f, size, minY, maxY);
    }

    public int getSeed()                      { return seed;          }
    public CaveType getType()                 { return type;          }
    public CavePlacementType getPlacementType() { return placementType; }
    public int getMinY()                      { return minY;          }
    public int getMaxY()                      { return maxY;          }

    public int getHeight(int x, int z) { return getHeight(0, x, z); }

    public int getHeight(int seed, int x, int z) {
        return getScaleValue(seed, x, z, 1F, minY, rangeY, elevation);
    }

    public int getCavernSize(int x, int z, float modifier) { return getCavernSize(0, x, z, modifier); }

    public int getCavernSize(int seed, int x, int z, float modifier) {
        return getScaleValue(seed, x, z, modifier, 0, size, shape);
    }

    public int getFloorDepth(int x, int z, int caveSize) { return getFloorDepth(0, x, z, caveSize); }

    public int getFloorDepth(int seed, int x, int z, int caveSize) {
        return getScaleValue(seed, x, z, 1F, 0, caveSize, floor);
    }

    /**
     * Розрахувати висоту стелі для CEILING_PATCH.
     * Повертає кількість блоків від стелі печери що відводяться під цей patch-біом.
     *
     * @param caveHeight повна висота порожнини (блоки)
     * @param patchMin   мінімальний відсоток [0..1]
     * @param patchMax   максимальний відсоток [0..1]
     * @param factor     0..1 (залежить від вологості/температури/висоти)
     */
    public static int calcCeilingPatchHeight(int caveHeight, float patchMin, float patchMax, float factor) {
        float pct = patchMin + factor * (patchMax - patchMin);
        return Math.max(1, NoiseUtil.floor(caveHeight * pct));
    }

    /**
     * Розрахувати радіус острівця ISLAND_PATCH у блоках.
     * maxRadiusChunks – береться з CaveSystemConfig.
     */
    public static int calcIslandRadius(float maxRadiusChunks) {
        return Math.max(1, NoiseUtil.floor(maxRadiusChunks * 16f));
    }

    /**
     * Розрахувати висоту острівця ISLAND_PATCH.
     * height = 1.5 * diameter = 1.5 * (radius * 2) = 3 * radius
     */
    public static int calcIslandHeight(int radiusBlocks) {
        return Math.max(1, (int)(radiusBlocks * 3f));
    }

    @Override
    public String toString() {
        return "NoiseCave{type=" + type +
                ", placement=" + placementType +
                ", minY=" + minY +
                ", maxY=" + maxY + '}';
    }

    private static int getScaleValue(int seed, int x, int z,
                                     float modifier, int min, int range, Module noise) {
        if (range <= 0) return 0;
        return min + NoiseUtil.floor(CaveNoise.sample(noise, seed, x, z) * range * modifier);
    }
}
