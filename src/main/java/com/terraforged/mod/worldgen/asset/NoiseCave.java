package com.terraforged.mod.worldgen.asset;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.terraforged.mod.codec.LazyCodec;
import com.terraforged.mod.util.seed.ContextSeedable;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.noise.NoiseCodec;
import com.terraforged.noise.Module;
import com.terraforged.noise.util.NoiseUtil;

public class NoiseCave implements ContextSeedable<NoiseCave> {
   public static final Codec<NoiseCave> CODEC = LazyCodec.record(
      instance -> instance.group(
            Codec.INT.optionalFieldOf("seed", 0).forGetter(c -> c.seed),
            CaveType.CODEC.fieldOf("type").forGetter(c -> c.type),
            NoiseCodec.CODEC.fieldOf("elevation").forGetter(c -> c.elevation),
            NoiseCodec.CODEC.fieldOf("shape").forGetter(c -> c.shape),
            NoiseCodec.CODEC.fieldOf("floor").forGetter(c -> c.floor),
            Codec.INT.fieldOf("size").forGetter(c -> c.size),
            Codec.INT.optionalFieldOf("min_y", -32).forGetter(c -> c.minY),
            Codec.INT.fieldOf("max_y").forGetter(c -> c.maxY)
         )
         .apply(instance, NoiseCave::new)
   );
   private final int seed;
   private final CaveType type;
   private final Module elevation;
   private final Module shape;
   private final Module floor;
   private final int size;
   private final int minY;
   private final int maxY;
   private final int rangeY;

   public NoiseCave(int seed, CaveType type, Module elevation, Module shape, Module floor, int size, int minY, int maxY) {
      this.seed = seed;
      this.type = type;
      this.elevation = elevation;
      this.shape = shape;
      this.floor = floor;
      this.size = size;
      this.minY = minY;
      this.maxY = maxY;
      this.rangeY = maxY - minY;
   }

   public NoiseCave withSeed(long seed) {
      Module module = this.withSeed(seed, this.elevation, Module.class);
      Module module1 = this.withSeed(seed, this.shape, Module.class);
      Module module2 = this.withSeed(seed, this.floor, Module.class);
      return new NoiseCave(this.seed, this.type, module, module1, module2, this.size, this.minY, this.maxY);
   }

   public int getSeed() {
      return this.seed;
   }

   public CaveType getType() {
      return this.type;
   }

   public int getHeight(int x, int z) {
      return getScaleValue(x, z, 1.0F, this.minY, this.rangeY, this.elevation);
   }

   public int getCavernSize(int x, int z, float modifier) {
      return getScaleValue(x, z, modifier, 0, this.size, this.shape);
   }

   public int getFloorDepth(int x, int z, int size) {
      return getScaleValue(x, z, 1.0F, 0, size, this.floor);
   }

   public int getMinY() {
      return this.minY;
   }

   public int getMaxY() {
      return this.maxY;
   }

   public static int calcCeilingPatchHeight(int caveHeight, float patchMin, float patchMax, float factor) {
      float pct = patchMin + factor * (patchMax - patchMin);
      return Math.max(1, NoiseUtil.floor(caveHeight * pct));
   }

   public static int calcIslandRadius(float maxRadiusChunks) {
      return Math.max(1, NoiseUtil.floor(maxRadiusChunks * 16.0F));
   }

   public static int calcIslandHeight(int radiusBlocks) {
      return Math.max(1, (int)(radiusBlocks * 3.0F));
   }

   @Override
   public String toString() {
      return "NoiseCave{type="
         + this.type
         + ", elevation="
         + this.elevation
         + ", shape="
         + this.shape
         + ", floor="
         + this.floor
         + ", size="
         + this.size
         + ", minY="
         + this.minY
         + ", maxY="
         + this.maxY
         + ", rangeY="
         + this.rangeY
         + "}";
   }

   private static int getScaleValue(int x, int z, float modifier, int min, int range, Module noise) {
      return range <= 0 ? 0 : min + NoiseUtil.floor(noise.getValue(x, z) * range * modifier);
   }
}
