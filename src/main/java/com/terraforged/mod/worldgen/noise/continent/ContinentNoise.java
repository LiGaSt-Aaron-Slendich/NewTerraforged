package com.terraforged.mod.worldgen.noise.continent;

import com.terraforged.engine.world.GeneratorContext;
import com.terraforged.engine.world.heightmap.ControlPoints;
import com.terraforged.mod.worldgen.noise.IContinentNoise;
import com.terraforged.mod.worldgen.noise.NoiseLevels;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.mod.worldgen.noise.continent.island.IslandFeatureOverlay;
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

   public ContinentNoise(TerrainLevels levels, GeneratorContext context) {
      this.levels = levels;
      this.context = context;
      this.controlPoints = new ControlPoints(context.settings.world.controlPoints);
      ContinentConfig config = createConfig(context);
      this.generator = new ContinentGenerator(config, levels.noiseLevels, this.controlPoints);
      this.islandOverlay = new IslandFeatureOverlay(config);
      this.offset = this.generator.getWorldOffset();
      this.frequency = 1.0F / context.settings.world.continent.continentScale;
      double d0 = 0.2;
      Builder builder = Source.builder().octaves(3).lacunarity(2.2).frequency(3.0).gain(0.3);
      this.warp = Domain.warp(builder.seed(context.seed.next()).perlin2(), builder.seed(context.seed.next()).perlin2(), Source.constant(d0));
   }

   @Override
   public void sampleContinent(float x, float y, NoiseSample sample) {
      // x/y are noise coords (block * levels.frequency); overlay needs world blocks.
      float freq = this.levels.noiseLevels.frequency;
      float worldX = freq > 1.0E-6F ? x / freq : x;
      float worldZ = freq > 1.0E-6F ? y / freq : y;
      if (this.context.settings.world.properties != null
              && this.context.settings.world.properties.worldStyle
              == com.terraforged.engine.settings.WorldSettings.WorldStyle.SHIPWRECKED) {
         // Islands-only: no mainland from shape; overlay places every landmass.
         sample.continentNoise = 0.0F;
         sample.terrainType = com.terraforged.engine.world.terrain.TerrainType.DEEP_OCEAN;
         this.islandOverlay.apply(worldX, worldZ, sample, this.levels.seaLevel);
         return;
      }
      x *= this.frequency;
      y *= this.frequency;
      float f = this.warp.getX(x, y);
      float f1 = this.warp.getY(x, y);
      f += this.offset.x;
      f1 += this.offset.y;
      this.generator.shapeGenerator.sample(f, f1, sample);
      sample.terrainType = ContinentPoints.getTerrainType(sample.continentNoise);
      this.islandOverlay.apply(worldX, worldZ, sample, this.levels.seaLevel);
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
