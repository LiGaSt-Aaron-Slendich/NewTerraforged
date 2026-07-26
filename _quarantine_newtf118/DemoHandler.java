package com.terraforged.mod.util;

import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.Environment;
import com.terraforged.mod.worldgen.Generator;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;

public class DemoHandler {
   private static final String WARNING = "This version of TerraForged has been provided for demo purposes only and may not be suitable for survival play. Please do NOT report bugs, compatibility issues, or feedback regarding this version of the mod.";

   public static void renderOverlay(PoseStack stack) {
      if (!Environment.DEV_ENV) {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.isWindowActive()) {
            LocalPlayer localplayer = minecraft.player;
            if (localplayer != null) {
               if (isTFWorld(localplayer.level)) {
                  Window window = minecraft.getWindow();
                  Font font = minecraft.font;
                  int i = window.getGuiScaledWidth();
                  int j = window.getGuiScaledHeight();
                  String s = "TerraForged Demo";
                  int k = i - font.width(s) - 5;
                  int l = j - 9 - 5;
                  font.drawShadow(stack, s, k, l, -65536);
               }
            }
         }
      }
   }

   public static void warn(Player player) {
      if (player != null && !Environment.DEV_ENV) {
         if (player.level.getChunkSource() instanceof ServerChunkCache serverchunkcache && isTFGenerator(serverchunkcache.getGenerator())) {
            player.sendMessage(
               Component.literal(
                     "This version of TerraForged has been provided for demo purposes only and may not be suitable for survival play. Please do NOT report bugs, compatibility issues, or feedback regarding this version of the mod."
                  )
                  .withStyle(ChatFormatting.RED),
               Util.NIL_UUID
            );
         }
      }
   }

   private static boolean isTFWorld(Level level) {
      return level.dimensionType().effectsLocation().getNamespace().equals("terraforged");
   }

   private static boolean isTFGenerator(ChunkGenerator generator) {
      return generator instanceof GeneratorProfiler || generator instanceof Generator;
   }
}
