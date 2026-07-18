package com.terraforged.mod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.GeneratorPreset;
import com.terraforged.mod.worldgen.Regenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

public class TFCommands {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(getLocateTerrainCommand());
      dispatcher.register(getTFCommand());
   }

   private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
      return (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal(name).requires(s -> s.hasPermission(2));
   }

   private static LiteralArgumentBuilder<CommandSourceStack> getLocateTerrainCommand() {
      return (LiteralArgumentBuilder<CommandSourceStack>)root("locateterrain")
         .then(
            ((RequiredArgumentBuilder)Arg.terrainType().then(Commands.argument("radius", IntegerArgumentType.integer(1)).executes(c -> locate(c, true))))
               .executes(c -> locate(c, false))
         );
   }

   private static LiteralArgumentBuilder<CommandSourceStack> getTFCommand() {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)root("tf")
            .then(Commands.literal("export").then(Commands.literal("structures").executes(TFCommands::export))))
         .then(Commands.literal("regen").then(Commands.argument("radius", IntegerArgumentType.integer(1)).executes(TFCommands::regen)));
   }

   private static int regen(CommandContext<CommandSourceStack> context) {
      try {
         int i = IntegerArgumentType.getInteger(context, "radius");
         Vec3 vec3 = ((CommandSourceStack)context.getSource()).getPosition();
         ChunkPos chunkpos = new ChunkPos((int)vec3.x >> 4, (int)vec3.z >> 4);
         Regenerator.regenerateChunks(chunkpos, i, ((CommandSourceStack)context.getSource()).getLevel(), (CommandSourceStack)context.getSource());
         return 1;
      } catch (Throwable throwable) {
         throwable.printStackTrace();
         return 1;
      }
   }

   private static int export(CommandContext<CommandSourceStack> context) {
      RegistryAccess registryaccess = ((CommandSourceStack)context.getSource()).registryAccess();
      MutableComponent mutablecomponent = new TextComponent("Exported structure settings").withStyle(s -> s.withColor(ChatFormatting.GREEN));
      ((CommandSourceStack)context.getSource()).sendSuccess(mutablecomponent, false);
      return 1;
   }

   private static int locate(CommandContext<CommandSourceStack> context, boolean withRadius) throws CommandSyntaxException {
      Generator generator = GeneratorPreset.getGenerator(((CommandSourceStack)context.getSource()).getLevel());
      if (generator == null) {
         return 1;
      } else {
         String s = StringArgumentType.getString(context, "terrain");
         Terrain terrain = TerrainType.get(s);
         int i = withRadius ? IntegerArgumentType.getInteger(context, "radius") : 1;
         ServerPlayer serverplayer = ((CommandSourceStack)context.getSource()).getPlayerOrException();
         BlockPos blockpos = serverplayer.blockPosition();
         Component component;
         if (terrain == null) {
            component = new TextComponent("Invalid terrain: " + s).withStyle(ChatFormatting.RED);
         } else {
            int j = Math.min(100, i + 50);
            long k = generator.getNoiseGenerator().find(blockpos.getX(), blockpos.getZ(), i, j, terrain);
            if (k == 0L) {
               component = new TextComponent("Unable to locate terrain: " + s).withStyle(ChatFormatting.RED);
            } else {
               int l = PosUtil.unpackLeft(k);
               int i1 = PosUtil.unpackRight(k);
               int j1 = generator.getFirstFreeHeight(l, i1, Types.MOTION_BLOCKING, serverplayer.level);
               component = createTerrainTeleportMessage(blockpos, l, j1, i1, terrain);
            }
         }

         serverplayer.sendMessage(component, ChatType.SYSTEM, Util.NIL_UUID);
         return 1;
      }
   }

   private static Component createTerrainTeleportMessage(BlockPos pos, int x, int y, int z, Terrain terrain) {
      double d0 = Math.sqrt(pos.distToCenterSqr(x, y, z));
      String s = String.format("/tp %s %s %s", x, y, z);
      String s1 = String.format("%.1f", d0);
      String s2 = String.format("%s;%s;%s", x, y, z);
      return new TextComponent("Found terrain: ")
         .withStyle(ChatFormatting.GREEN)
         .append(new TextComponent(terrain.getName()).withStyle(ChatFormatting.YELLOW))
         .append(new TextComponent(" Distance: ").withStyle(ChatFormatting.GREEN))
         .append(new TextComponent(s1).withStyle(ChatFormatting.YELLOW))
         .append(new TextComponent(". ").withStyle(ChatFormatting.GREEN))
         .append(
            new TextComponent("Teleport")
               .withStyle(new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.UNDERLINE})
               .withStyle(
                  style -> style.withClickEvent(new ClickEvent(Action.RUN_COMMAND, s))
                     .withHoverEvent(
                        new HoverEvent(
                           net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                           new TextComponent("Location: ").withStyle(ChatFormatting.GREEN).append(new TextComponent(s2).withStyle(ChatFormatting.YELLOW))
                        )
                     )
               )
         );
   }
}
