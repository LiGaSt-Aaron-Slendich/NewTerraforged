package com.terraforged.mod.worldgen.biome;

import com.mojang.serialization.Codec;
import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.mod.platform.forge.TFCaveBiomeConfig;
import com.terraforged.mod.util.map.LongCache;
import com.terraforged.mod.util.map.LossyCache;
import com.terraforged.mod.worldgen.biome.util.BiomeMapManager;
import com.terraforged.mod.worldgen.cave.CaveBiomeRegistry;
import com.terraforged.mod.worldgen.cave.CaveBiomeRegistryLoader;
import com.terraforged.mod.worldgen.cave.CaveSystemConfig;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.Sampler;

public class Source extends BiomeSource {
   public static final Codec<Source> CODEC = new SourceCodec();
   public static final Sampler NOOP_CLIMATE_SAMPLER = Climate.empty();
   protected final long seed;
   protected final RegistryAccess registries;
   protected final Set<Holder<Biome>> possibleBiomes;
   protected final BiomeSampler biomeSampler;
   protected final BiomeMapManager biomeMapManager;
   protected final CaveBiomeSampler caveBiomeSampler;
   protected final LongCache<Holder<Biome>> cache = LossyCache.concurrent(2048, Holder[]::new);

   public Source(long seed, INoiseGenerator noise, Source other) {
      super(List.of());
      this.seed = seed;
      this.registries = other.registries;
      this.biomeMapManager = other.biomeMapManager;
      this.possibleBiomes = new ObjectLinkedOpenHashSet(other.possibleBiomes);
      this.biomeSampler = new BiomeSampler(noise, other.biomeMapManager);
      this.caveBiomeSampler = new CaveBiomeSampler(seed, other.caveBiomeSampler);
   }

   public Source(long seed, INoiseGenerator noise, RegistryAccess access) {
      super(List.of());
      this.seed = seed;
      this.registries = access;
      this.biomeMapManager = new BiomeMapManager(access);
      this.possibleBiomes = new ObjectLinkedOpenHashSet(this.biomeMapManager.getOverworldBiomes());
      this.biomeSampler = new BiomeSampler(noise, this.biomeMapManager);
      CaveBiomeRegistry registry = null;
      if (TFCaveBiomeConfig.INSTANCE != null) {
         registry = CaveBiomeRegistryLoader.build(this.biomeMapManager.getBiomes(), TFCaveBiomeConfig.INSTANCE);
      }
      this.caveBiomeSampler = new CaveBiomeSampler(seed, 800, this.biomeMapManager, registry, CaveSystemConfig.DEFAULT);
   }

   public Set<Holder<Biome>> possibleBiomes() {
      return this.possibleBiomes;
   }

   protected Codec<? extends BiomeSource> codec() {
      return CODEC;
   }

   public Source withSeed(long l) {
      return this;
   }

   public Holder<Biome> getNoiseBiome(int x, int y, int z, Sampler sampler) {
      return this.cache.computeIfAbsent(PosUtil.pack(x, z), this::compute);
   }

   public RegistryAccess getRegistries() {
      return this.registries;
   }

   public BiomeSampler getBiomeSampler() {
      return this.biomeSampler;
   }

   public CaveBiomeSampler getCaveBiomeSampler() {
      return this.caveBiomeSampler;
   }

   public CaveBiomeRegistry getCaveBiomeRegistry() {
      return this.caveBiomeSampler.getRegistry();
   }

   public Holder<Biome> getUnderGroundBiome(int seed, int x, int z, CaveType type) {
      return this.caveBiomeSampler.getUnderGroundBiome(seed, x, z, type);
   }

   public Holder<Biome> getUnderGroundBiome(
      int seed,
      int x,
      int z,
      CaveType type,
      Holder<Biome> surfaceBiome,
      int blockY,
      int surfaceY,
      int caveCenterX,
      int caveCenterZ,
      int caveRadius
   ) {
      return this.caveBiomeSampler.getUnderGroundBiome(
         seed, x, z, type, surfaceBiome, blockY, surfaceY, caveCenterX, caveCenterZ, caveRadius
      );
   }

   public Registry<Biome> getRegistry() {
      return this.biomeMapManager.getBiomes();
   }

   protected Holder<Biome> compute(long index) {
      int i = PosUtil.unpackLeft(index) << 2;
      int j = PosUtil.unpackRight(index) << 2;
      return this.biomeSampler.sampleBiome(i, j);
   }
}
