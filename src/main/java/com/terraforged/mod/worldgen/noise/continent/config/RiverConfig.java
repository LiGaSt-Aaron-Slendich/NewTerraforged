package com.terraforged.mod.worldgen.noise.continent.config;

public class RiverConfig {
   public float erosion = 0.075F;
   public final FloatRange bedWidth = new FloatRange(1.0F, 7.0F);
   public final FloatRange bankWidth = new FloatRange(3.0F, 30.0F);
   public final FloatRange valleyWidth = new FloatRange(80.0F, 200.0F);
   public final FloatRange bedDepth = new FloatRange(1.25F, 5.0F);
   public final FloatRange bankDepth = new FloatRange(1.25F, 3.0F);

   public RiverConfig copy(RiverConfig config) {
      this.erosion = config.erosion;
      this.bedDepth.copy(config.bedDepth);
      this.bankDepth.copy(config.bankDepth);
      this.bedWidth.copy(config.bedWidth);
      this.bankWidth.copy(config.bankWidth);
      this.valleyWidth.copy(config.valleyWidth);
      return this;
   }

   public RiverConfig scale(float frequency) {
      this.bedWidth.scale(frequency);
      this.bankWidth.scale(frequency);
      this.valleyWidth.scale(frequency);
      return this;
   }

   public static RiverConfig lake() {
      RiverConfig riverconfig = new RiverConfig();
      riverconfig.bankWidth.min = 30.0F;
      riverconfig.bankWidth.max = 45.0F;
      riverconfig.bankDepth.min = 1.0F;
      riverconfig.bankDepth.max = 1.5F;
      riverconfig.bedWidth.min = 8.0F;
      riverconfig.bedWidth.max = 15.0F;
      riverconfig.bedDepth.min = 2.0F;
      riverconfig.bedDepth.max = 8.0F;
      riverconfig.valleyWidth.min = 80.0F;
      riverconfig.valleyWidth.max = 120.0F;
      return riverconfig;
   }
}
