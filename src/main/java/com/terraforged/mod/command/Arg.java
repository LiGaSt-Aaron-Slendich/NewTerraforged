package com.terraforged.mod.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.registry.ModRegistry;
import com.terraforged.mod.worldgen.asset.TerrainType;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess.Frozen;

public class Arg {
   public static RequiredArgumentBuilder<CommandSourceStack, String> terrainType() {
      return Commands.argument("terrain", StringArgumentType.string()).suggests((context, builder) -> {
         Frozen frozen = ((CommandSourceStack)context.getSource()).getServer().registryAccess();
         Optional<Registry<TerrainType>> optional = frozen.ownedRegistry(ModRegistry.TERRAIN_TYPE.get());
         if (optional.isEmpty()) {
            com.terraforged.engine.world.terrain.TerrainType.forEach(type -> builder.suggest(type.getName()));
         } else {
            optional.get().forEach(type -> builder.suggest(type.getName()));
         }

         return builder.buildFuture();
      });
   }
}
