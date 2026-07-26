package com.terraforged.mod.client;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.client.ingame.DimensionEffects;
import com.terraforged.mod.util.Init;

public class Client extends Init {
   public static final Client INSTANCE = new Client();

   Client() {
   }

   @Override
   protected void doInit() {
      TerraForged.LOG.info("Registering custom overworld effects");
      DimensionEffects.register(TerraForged.location("overworld"), new DimensionEffects());
   }
}
