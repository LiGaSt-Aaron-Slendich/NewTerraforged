package com.terraforged.mod.platform.forge.client;

import com.terraforged.mod.platform.forge.util.ForgeUtil;
import com.terraforged.mod.worldgen.GeneratorPreset;
import com.terraforged.mod.worldgen.datapack.DataPackExporter;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import java.nio.file.Files;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.world.ForgeWorldPreset;
import net.minecraftforge.common.world.ForgeWorldPreset.IBasicChunkGeneratorFactory;

public class TFPreset implements IBasicChunkGeneratorFactory {
   /** Registered world-type instance (client uses this for Customize editor). */
   public static ForgeWorldPreset INSTANCE;

   public ChunkGenerator createChunkGenerator(RegistryAccess registryAccess, long seed) {
      return GeneratorPreset.build(seed, TerrainLevels.DEFAULT.get(), registryAccess);
   }

   public static ForgeWorldPreset create() {
      INSTANCE = ForgeUtil.withName(new ForgeWorldPreset(new TFPreset()) {
         public String getTranslationKey() {
            return GeneratorPreset.TRANSLATION_KEY;
         }

         public Component getDisplayName() {
            return new TranslatableComponent(this.getTranslationKey()).withStyle(s -> s.withColor(ChatFormatting.GREEN));
         }
      }, "newterraforged");
      return INSTANCE;
   }

   public static void makeDefault() {
      if (!Files.exists(DataPackExporter.CONFIG_DIR)) {
         String s = (String)ForgeConfig.COMMON.defaultWorldType.get();
         if (s.isEmpty() || s.equals("default") || s.equals("terraforged")) {
            ConfigValue configvalue = ForgeConfig.COMMON.defaultWorldType;
            configvalue.set("newterraforged");
         }
      }
   }
}
