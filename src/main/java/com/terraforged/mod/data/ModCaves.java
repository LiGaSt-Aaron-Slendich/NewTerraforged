package com.terraforged.mod.data;

import com.terraforged.mod.Environment;
import com.terraforged.mod.registry.ModRegistries;
import com.terraforged.mod.registry.ModRegistry;
import com.terraforged.mod.util.seed.RandSeed;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

interface ModCaves extends ModRegistry {
   static void register() {
      RandSeed randseed = new RandSeed(901246L, 500000);
      ModRegistries.register(CAVE, "synapse_high", ModCaves.Factory.synapse(randseed.next(), 0.75F, 96, 384));
      ModRegistries.register(CAVE, "synapse_mid", ModCaves.Factory.synapse(randseed.next(), 1.0F, 0, 256));
      ModRegistries.register(CAVE, "synapse_low", ModCaves.Factory.synapse(randseed.next(), 1.2F, -32, 128));
      ModRegistries.register(CAVE, "mega", ModCaves.Factory.mega(randseed.next(), 1.0F, -16, 64));
      ModRegistries.register(CAVE, "mega_deep", ModCaves.Factory.mega(randseed.next(), 1.2F, -32, 48));
   }

   static NoiseCave[] getCaves(RegistryAccess access) {
      return access != null && !Environment.DEV_ENV
         ? ModRegistry.entries(access, (ResourceKey<Registry<NoiseCave>>)CAVE.get(), NoiseCave[]::new)
         : ModCaves.Factory.getDefaults();
   }

   public static class Factory {
      static NoiseCave mega(int seed, float scale, int minY, int maxY) {
         int i = NoiseUtil.floor(200.0F * scale);
         int j = NoiseUtil.floor(250.0F * scale);
         int k = NoiseUtil.floor(50.0F * scale);
         int l = NoiseUtil.floor(30.0F * scale);
         Module module = Source.simplex(++seed, i, 2).map(0.3, 0.7);
         Module module1 = Source.simplex(++seed, j, 3).bias(-0.5).abs().scale(2.0).invert().clamp(0.75, 1.0).map(0.0, 1.0);
         Module module2 = Source.simplex(++seed, k, 2).clamp(0.0, 0.3).map(0.0, 1.0);
         return new NoiseCave(seed, CaveType.UNIQUE, module, module1, module2, l, minY, maxY);
      }

      static NoiseCave synapse(int seed, float scale, int minY, int maxY) {
         int i = NoiseUtil.floor(350.0F * scale);
         int j = NoiseUtil.floor(180.0F * scale);
         int k = NoiseUtil.floor(20.0F * scale);
         int l = k / 2;
         int i1 = NoiseUtil.floor(30.0F * scale);
         int j1 = NoiseUtil.floor(15.0F * scale);
         Module module = Source.simplex(++seed, i, 3).map(0.1, 0.9);
         Module module1 = Source.simplexRidge(++seed, j, 3).warp(++seed, k, 1, l).clamp(0.35, 0.75).map(0.0, 1.0);
         Module module2 = Source.simplex(++seed, i1, 2).clamp(0.0, 0.15).map(0.0, 1.0);
         return new NoiseCave(seed, CaveType.GLOBAL, module, module1, module2, j1, minY, maxY);
      }

      static NoiseCave[] getDefaults() {
         RandSeed randseed = new RandSeed(901246L, 500000);
         return new NoiseCave[]{
            synapse(randseed.next(), 0.75F, 96, 384),
            synapse(randseed.next(), 1.0F, 0, 256),
            synapse(randseed.next(), 1.2F, -32, 128),
            mega(randseed.next(), 1.0F, -16, 64),
            mega(randseed.next(), 1.2F, -32, 48)
         };
      }
   }
}
