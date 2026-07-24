package com.terraforged.mod.worldgen.noise;

import com.terraforged.engine.world.GeneratorContext;
import com.terraforged.engine.world.heightmap.ControlPoints;
import com.terraforged.mod.worldgen.noise.continent.CoastalLiaOverlay;

public interface IContinentNoise {
   GeneratorContext getContext();

   ControlPoints getControlPoints();

   void sampleContinent(float var1, float var2, NoiseSample var3);

   void sampleRiver(float var1, float var2, NoiseSample var3);

   /** Coastal Little Ice Age overlay; never null on production continent noise. */
   default CoastalLiaOverlay getCoastalLia() {
      return null;
   }
}
