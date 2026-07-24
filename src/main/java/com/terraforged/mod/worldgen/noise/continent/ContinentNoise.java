package com.terraforged.mod.worldgen.noise.continent;

import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.engine.world.GeneratorContext;
import com.terraforged.engine.world.heightmap.ControlPoints;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.noise.IContinentNoise;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.mod.worldgen.noise.continent.island.IslandFeatureOverlay;
import com.terraforged.mod.worldgen.noise.continent.ocean.OceanLandscapeOverlay;
import com.terraforged.mod.worldgen.settings.ContinentRiverWiring;
import com.terraforged.mod.worldgen.settings.ContinentShapeWiring;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import com.terraforged.noise.Source;
import com.terraforged.noise.domain.Domain;
import com.terraforged.noise.source.Builder;
import com.terraforged.noise.util.Vec2f;

public class ContinentNoise implements IContinentNoise {
   protected final TerrainLevels levels;
   protected final GeneratorContext context;
   protected final ControlPoints controlPoints;
   protected final ContinentGenerator generator;
   protected final Domain warp;
   protected final Vec2f offset;
   protected final float frequency;
   protected final IslandFeatureOverlay islandOverlay;
   protected final OceanLandscapeOverlay oceanLandscape;
   protected final CoastalLiaOverlay coastalLia;
   protected final boolean shipwrecked;
   private boolean liaGraphBound;

   public ContinentNoise(TerrainLevels levels, GeneratorContext context) {
      long t0 = System.nanoTime();
      this.levels = levels;
      this.context = context;
      this.controlPoints = new ControlPoints(context.settings.world.controlPoints);
      ContinentConfig config = createConfig(context);
      this.generator = new ContinentGenerator(config, levels.noiseLevels, this.controlPoints);
      // Instant — no spiral search (was freezing create-world at 0%).
      this.offset = this.generator.getWorldOffset();
      this.islandOverlay = new IslandFeatureOverlay(config);
      // Corridor graph is lazy inside overlay — do not build here.
      this.oceanLandscape = new OceanLandscapeOverlay(config, this.generator);
      this.coastalLia = new CoastalLiaOverlay(config.shape.seed0);
      this.frequency = 1.0F / context.settings.world.continent.continentScale;
      this.shipwrecked = context.settings.world.properties != null
            && context.settings.world.properties.worldStyle == WorldSettings.WorldStyle.SHIPWRECKED;
      double d0 = 0.2;
      Builder builder = Source.builder().octaves(3).lacunarity(2.2).frequency(3.0).gain(0.3);
      this.warp = Domain.warp(builder.seed(context.seed.next()).perlin2(), builder.seed(context.seed.next()).perlin2(), Source.constant(d0));
      TerraForged.LOG.info("[ContinentNoise] init {} ms (offset={}, ol={})",
            (System.nanoTime() - t0) / 1_000_000L,
            this.offset,
            OceanLandscapeOverlay.isActive());
   }

   @Override
   public void sampleContinent(float x, float y, NoiseSample sample) {
      // Same frame as rivers / shape: scale → warp → worldOffset, then island contribution.
      x *= this.frequency;
      y *= this.frequency;
      float f = this.warp.getX(x, y);
      float f1 = this.warp.getY(x, y);
      f += this.offset.x;
      f1 += this.offset.y;

      if (this.shipwrecked) {
         sample.continentNoise = 0.0F;
         sample.baseNoise = 0.0F;
         sample.heightNoise = 0.0F;
         sample.oceanRelief = 0.0F;
         sample.terrainType = TerrainType.DEEP_OCEAN;
      } else {
         this.generator.shapeGenerator.sample(f, f1, sample);
         sample.terrainType = ContinentPoints.getTerrainType(sample.continentNoise);
      }

      // Block-equivalent coords that include warp+offset so continent shift moves islands.
      float noiseFreq = this.levels.noiseLevels.frequency;
      float invNoise = noiseFreq > 1.0E-6F ? 1.0F / noiseFreq : 1.0F;
      float islandX = f / this.frequency * invNoise;
      float islandZ = f1 / this.frequency * invNoise;
      if (OceanLandscapeOverlay.isActive()) {
         this.oceanLandscape.apply(islandX, islandZ, f, f1, sample);
         ensureLiaGraphBound();
      } else {
         this.islandOverlay.apply(islandX, islandZ, sample, this.levels.seaLevel);
      }
      // Little Ice Age coastal warp after islands so island paint stays intact.
      if (!this.shipwrecked) {
         this.coastalLia.applyContinent(islandX, islandZ, f, f1, sample);
      }
   }

   private void ensureLiaGraphBound() {
      if (this.liaGraphBound) {
         return;
      }
      this.coastalLia.bindCorridorGraph(
            this.generator,
            this.oceanLandscape.corridorGraph(),
            this.context.settings.world.continent.continentScale);
      this.liaGraphBound = true;
   }

   @Override
   public CoastalLiaOverlay getCoastalLia() {
      return this.coastalLia;
   }

   @Override
   public void sampleRiver(float x, float y, NoiseSample sample) {
      x *= this.frequency;
      y *= this.frequency;
      float f = this.warp.getX(x, y);
      float f1 = this.warp.getY(x, y);
      f += this.offset.x;
      f1 += this.offset.y;
      this.generator.riverGenerator.sample(f, f1, sample);
   }

   @Override
   public GeneratorContext getContext() {
      return this.context;
   }

   @Override
   public ControlPoints getControlPoints() {
      return this.controlPoints;
   }

   protected static ContinentConfig createConfig(GeneratorContext context) {
      ContinentConfig continentconfig = new ContinentConfig();
      continentconfig.shape.scale = context.settings.world.continent.continentScale;
      continentconfig.shape.seed0 = context.seed.next();
      continentconfig.shape.seed1 = context.seed.next();
      ContinentShapeWiring.apply(continentconfig, context.settings);
      ContinentRiverWiring.apply(continentconfig, context.settings);
      return continentconfig;
   }
}
