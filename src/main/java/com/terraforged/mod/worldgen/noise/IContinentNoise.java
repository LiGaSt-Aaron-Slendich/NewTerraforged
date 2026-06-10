package com.terraforged.mod.worldgen.noise;

import com.terraforged.engine.world.GeneratorContext;
import com.terraforged.engine.world.heightmap.ControlPoints;
import com.terraforged.mod.worldgen.noise.NoiseSample;

public interface IContinentNoise {
    public GeneratorContext getContext();

    public ControlPoints getControlPoints();

    public void sampleContinent(int var1, float var2, float var3, NoiseSample var4);

    public void sampleRiver(int var1, float var2, float var3, NoiseSample var4);
}
