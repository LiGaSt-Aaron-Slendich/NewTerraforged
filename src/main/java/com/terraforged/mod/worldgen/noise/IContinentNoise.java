package com.terraforged.mod.worldgen.noise;

import com.terraforged.engine.world.GeneratorContext;
import com.terraforged.engine.world.heightmap.ControlPoints;

public interface IContinentNoise {
   GeneratorContext getContext();

   ControlPoints getControlPoints();

   void sampleContinent(float var1, float var2, NoiseSample var3);

   void sampleRiver(float var1, float var2, NoiseSample var3);
}
