package com.terraforged.mod.worldgen.biome;

import com.terraforged.mod.data.ModBiomes;
import com.terraforged.mod.util.map.WeightMap;
import com.terraforged.mod.worldgen.biome.util.BiomeMapManager;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.noise.util.Noise;
import com.terraforged.noise.util.NoiseUtil;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.BiomeCategory;

public class CaveBiomeSampler {
   public static final int OFFSET = 124897;
   protected final int seed;
   protected final int scale;
   protected final float frequency;
   protected final Map<CaveType, WeightMap<Holder<Biome>>> typeMap = new EnumMap<>(CaveType.class);

   public CaveBiomeSampler(long seed, int scale, BiomeMapManager biomeMapManager) {
      this.seed = (int)seed + 124897;
      this.scale = scale;
      this.frequency = 1.0F / scale;
      Holder<Biome>[] holder = new Holder[]{biomeMapManager.getBiomes().getHolderOrThrow(ModBiomes.CAVE.key())};
      Holder<Biome>[] holder1 = biomeMapManager.getBiomes()
         .holders()
         .filter(b -> Biome.getBiomeCategory(b) == BiomeCategory.UNDERGROUND)
         .toArray(Holder[]::new);
      this.typeMap.put(CaveType.GLOBAL, create(holder));
      this.typeMap.put(CaveType.UNIQUE, create(holder1));
   }

   public CaveBiomeSampler(long seed, CaveBiomeSampler other) {
      this.seed = (int)seed + 124897;
      this.scale = other.scale;
      this.frequency = 1.0F / other.scale;
      this.typeMap.putAll(other.typeMap);
   }

   public Holder<Biome> getUnderGroundBiome(int seed, int x, int z, CaveType type) {
      float f = sample(seed + this.seed, x, z, this.frequency);
      return this.typeMap.get(type).getValue(f);
   }

   protected static float sample(int seed, int x, int z, float frequency) {
      float f = x * frequency;
      float f1 = z * frequency;
      float f2 = (1.0F + Noise.singleSimplex(f, f1, seed)) * 0.5F;
      return NoiseUtil.clamp(f2, 0.0F, 1.0F);
   }

   protected static WeightMap<Holder<Biome>> create(Holder<Biome>[] biomes) {
      float[] afloat = new float[biomes.length];
      Arrays.fill(afloat, 1.0F);
      return new WeightMap<>(biomes, afloat);
   }
}
