package com.terraforged.mod.worldgen.noise.continent;

import com.terraforged.engine.world.GeneratorContext;
import com.terraforged.engine.world.heightmap.ControlPoints;
import com.terraforged.mod.worldgen.noise.IContinentNoise;
import com.terraforged.mod.worldgen.noise.NoiseLevels;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
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

   public ContinentNoise(TerrainLevels levels, GeneratorContext context) {
      this.levels = levels;
      this.context = context;
      this.controlPoints = new ControlPoints(context.settings.world.controlPoints);
      this.generator = createContinent(context, this.controlPoints, levels.noiseLevels);
      this.offset = this.generator.getWorldOffset();
      this.frequency = 1.0F / context.settings.world.continent.continentScale;
      double d0 = 0.2;
      Builder builder = Source.builder().octaves(3).lacunarity(2.2).frequency(3.0).gain(0.3);
      this.warp = Domain.warp(builder.seed(context.seed.next()).perlin2(), builder.seed(context.seed.next()).perlin2(), Source.constant(d0));
   }

   @Override
   public void sampleContinent(float x, float y, NoiseSample sample) {
      x *= this.frequency;
      y *= this.frequency;
      float f = this.warp.getX(x, y);
      float f1 = this.warp.getY(x, y);
      f += this.offset.x;
      f1 += this.offset.y;
      this.generator.shapeGenerator.sample(f, f1, sample);
      sample.terrainType = ContinentPoints.getTerrainType(sample.continentNoise);
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

   protected static ContinentGenerator createContinent(GeneratorContext context, ControlPoints controlPoints, NoiseLevels levels) {
      ContinentConfig continentconfig = new ContinentConfig();
      continentconfig.shape.scale = context.settings.world.continent.continentScale;
      continentconfig.shape.seed0 = context.seed.next();
      continentconfig.shape.seed1 = context.seed.next();
      return new ContinentGenerator(continentconfig, levels, controlPoints);
   }
}
