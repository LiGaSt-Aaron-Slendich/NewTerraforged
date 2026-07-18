package com.terraforged.mod.data;

import com.terraforged.mod.util.Init;

public class Content extends Init {
   public static final Content INSTANCE = new Content();

   @Override
   protected void doInit() {
      try {
         ModTerrainTypes.register();
         ModTerrains.register();
         ModBiomes.register();
         ModCaves.register();
         ModVegetations.register();
         ModClimates.register();
      } catch (Throwable throwable) {
         throwable.printStackTrace();
      }
   }
}
