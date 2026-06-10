package com.terraforged.mod.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.TerraForged;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

public class Arg {
    public static RequiredArgumentBuilder<CommandSourceStack, String> terrainType() {
        return Commands.argument((String)"terrain", (ArgumentType)StringArgumentType.string()).suggests((context, builder) -> {
            RegistryAccess.Frozen registries = ((CommandSourceStack)context.getSource()).getServer().registryAccess();
            Optional terrainTypes = registries.ownedRegistry(TerraForged.TERRAIN_TYPES.get());
            if (terrainTypes.isEmpty()) {
                TerrainType.forEach(type -> builder.suggest(type.getName()));
            } else {
                ((Registry<com.terraforged.mod.worldgen.asset.TerrainType>) terrainTypes.get()).forEach(type -> builder.suggest(type.getName()));
            }
            return builder.buildFuture();
        });
    }
}
