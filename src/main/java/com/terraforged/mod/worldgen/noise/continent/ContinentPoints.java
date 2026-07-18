package com.terraforged.mod.worldgen.noise.continent;

import com.terraforged.engine.world.heightmap.ControlPoints;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.worldgen.noise.continent.shape.FalloffPoint;

public interface ContinentPoints {
   float DEEP_OCEAN = 0.1F;
   float SHALLOW_OCEAN = 0.25F;
   float BEACH = 0.5F;
   float COAST = 0.55F;
   float INLAND = 0.6F;

   static Terrain getTerrainType(float continentNoise) {
      if (continentNoise < 0.25F) {
         return TerrainType.DEEP_OCEAN;
      } else if (continentNoise < 0.5F) {
         return TerrainType.SHALLOW_OCEAN;
      } else {
         return continentNoise < 0.55F ? TerrainType.COAST : TerrainType.NONE;
      }
   }

   static FalloffPoint[] getFalloff(ControlPoints controlPoints) {
      return new FalloffPoint[]{
         new FalloffPoint(controlPoints.inland, 1.0F, 1.0F),
         new FalloffPoint(controlPoints.coast, 0.55F, 1.0F),
         new FalloffPoint(controlPoints.beach, 0.5F, 0.55F),
         new FalloffPoint(controlPoints.shallowOcean, 0.25F, 0.5F),
         new FalloffPoint(controlPoints.deepOcean, 0.1F, 0.25F)
      };
   }

   static float getFalloff(float continentNoise, FalloffPoint[] falloffCurve) {
      float f = 1.0F;

      for (FalloffPoint falloffpoint : falloffCurve) {
         if (continentNoise >= falloffpoint.controlPoint()) {
            return MathUtil.map(continentNoise, falloffpoint.controlPoint(), f, falloffpoint.min(), falloffpoint.max());
         }

         f = falloffpoint.controlPoint();
      }

      return MathUtil.map(continentNoise, 0.0F, f, 0.0F, 0.1F);
   }
}
