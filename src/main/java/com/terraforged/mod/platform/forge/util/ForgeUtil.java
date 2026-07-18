package com.terraforged.mod.platform.forge.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.GameData;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ForgeUtil {
   public static <T extends IForgeRegistryEntry<T>> T withName(T t, String name) {
      if (t.getRegistryName() != null) {
         return t;
      } else {
         Logger logger = LogManager.getLogger(GameData.class);
         Level level = logger.getLevel();

         try {
            setLevel(logger, Level.OFF);
            t.setRegistryName(new ResourceLocation("minecraft", name));
         } finally {
            setLevel(logger, level);
         }

         return t;
      }
   }

   private static void setLevel(Logger logger, Level level) {
      if (logger instanceof org.apache.logging.log4j.core.Logger loggerx) {
         loggerx.setLevel(level);
      }
   }
}
