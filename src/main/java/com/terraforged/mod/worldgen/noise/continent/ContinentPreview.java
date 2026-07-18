package com.terraforged.mod.worldgen.noise.continent;

import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.mod.data.ModTerrains;
import com.terraforged.mod.util.ColorUtil;
import com.terraforged.mod.util.ui.Previewer;
import com.terraforged.mod.worldgen.noise.NoiseGenerator;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.climate.ClimateNoise;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import java.util.concurrent.ThreadLocalRandom;

public class ContinentPreview {
   public static int SEED = ThreadLocalRandom.current().nextInt();

   public static void main(String[] args) {
      Previewer.launch(() -> {
         ContinentPreview.Noise continentpreview$noise = create();
         return (x, y) -> {
            ClimateSample climatesample = continentpreview$noise.getSample(x, y);
            return ColorUtil.getBiomeColor(climatesample, 0.2F);
         };
      });
   }

   private static ContinentPreview.Noise create() {
      WorldSettings.ControlPoints worldsettings$controlpoints = new WorldSettings.ControlPoints();
      worldsettings$controlpoints.deepOcean = 0.05F;
      worldsettings$controlpoints.shallowOcean = 0.3F;
      worldsettings$controlpoints.beach = 0.45F;
      worldsettings$controlpoints.coast = 0.75F;
      worldsettings$controlpoints.inland = 0.8F;
      ContinentConfig continentconfig = new ContinentConfig();
      continentconfig.shape.seed0 = SEED;
      continentconfig.shape.seed1 = SEED + 39674;
      continentconfig.shape.threshold = 0.525F;
      TerrainLevels terrainlevels = new TerrainLevels();
      NoiseGenerator noisegenerator = new NoiseGenerator(SEED, terrainlevels, ModTerrains.Factory.getDefault(null));
      return new ContinentPreview.Noise(noisegenerator, new ClimateNoise(noisegenerator.getContinent().getContext()));
   }

   public record Noise(NoiseGenerator generator, ClimateNoise climate) {
      public ClimateSample getSample(float x, float y) {
         ClimateSample climatesample = this.climate.getSample(x, y);
         this.sampleContinent(x, y, climatesample);
         this.sampleRivers(x, y, climatesample);
         return climatesample;
      }

      public void sampleContinent(float x, float y, NoiseSample sample) {
         this.generator.sampleContinentNoise((int)x, (int)y, sample);
      }

      public void sampleRivers(float x, float y, NoiseSample sample) {
         this.generator.sampleRiverNoise((int)x, (int)y, sample);
      }
   }
}
