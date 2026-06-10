package com.terraforged.mod.worldgen.noise.continent;

import com.terraforged.engine.world.GeneratorContext;
import com.terraforged.engine.world.heightmap.ControlPoints;
import com.terraforged.mod.worldgen.noise.IContinentNoise;
import com.terraforged.mod.worldgen.noise.NoiseLevels;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.ContinentGenerator;
import com.terraforged.mod.worldgen.noise.continent.ContinentPoints;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import com.terraforged.noise.Source;
import com.terraforged.noise.domain.Domain;
import com.terraforged.noise.source.Builder;
import com.terraforged.noise.util.Vec2f;

public class ContinentNoise
implements IContinentNoise {
    protected final TerrainLevels levels;
    protected final GeneratorContext context;
    protected final ControlPoints controlPoints;
    protected final ContinentGenerator generator;
    protected final Domain warp;
    protected final float frequency;

    public ContinentNoise(TerrainLevels levels, GeneratorContext context) {
        this.levels = levels;
        this.context = context;
        this.controlPoints = new ControlPoints(context.settings.world.controlPoints);
        this.generator = ContinentNoise.createContinent(context, this.controlPoints, levels.noiseLevels);
        this.frequency = 1.0f / (float)context.settings.world.continent.continentScale;
        double strength = 0.2;
        Builder builder = Source.builder().octaves(3).lacunarity(2.2).frequency(3.0).gain(0.3);
        this.warp = Domain.warp(builder.seed(context.seed.next()).perlin2(), builder.seed(context.seed.next()).perlin2(), Source.constant(strength));
    }

    @Override
    public void sampleContinent(int seed, float x, float y, NoiseSample sample) {
        float px = this.warp.getX(x *= this.frequency, y *= this.frequency);
        float py = this.warp.getY(x, y);
        Vec2f offset = this.generator.getWorldOffset(seed);
        this.generator.shapeGenerator.sample(seed, px += offset.x, py += offset.y, sample);
        sample.terrainType = ContinentPoints.getTerrainType(sample.continentNoise);
    }

    @Override
    public void sampleRiver(int seed, float x, float y, NoiseSample sample) {
        float px = this.warp.getX(x *= this.frequency, y *= this.frequency);
        float py = this.warp.getY(x, y);
        Vec2f offset = this.generator.getWorldOffset(seed);
        this.generator.riverGenerator.sample(seed, px += offset.x, py += offset.y, sample);
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
        ContinentConfig config = new ContinentConfig();
        config.shape.scale = context.settings.world.continent.continentScale;
        config.shape.seed0 = context.seed.next();
        config.shape.seed1 = context.seed.next();
        return new ContinentGenerator(config, levels, controlPoints);
    }
}
