package com.terraforged.mod.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.terraforged.mod.platform.forge.CaveDebugNetwork;
import com.terraforged.mod.util.map.WeightMap;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.GeneratorPreset;
import com.terraforged.mod.worldgen.biome.BiomeSampler;
import com.terraforged.mod.worldgen.biome.IBiomeSampler;
import com.terraforged.mod.worldgen.biome.util.BiomeTerrainIntegration;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * {@code /newtf debug terrain [menu]} — surface terrain / subterrain / biome pick diagnostics.
 */
public final class TerrainDebugCommand {
    private TerrainDebugCommand() {
    }

    /** Branch attached under {@code /newtf debug}. */
    public static LiteralArgumentBuilder<CommandSourceStack> branch() {
        return Commands.literal("terrain")
                .executes(TerrainDebugCommand::executeChat)
                .then(Commands.literal("menu").executes(TerrainDebugCommand::executeMenu));
    }

    private static int executeChat(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<String> lines = collectLines(context.getSource());
        for (String line : lines) {
            player.sendMessage(new TextComponent(line).withStyle(ChatFormatting.GRAY), player.getUUID());
        }
        return 1;
    }

    private static int executeMenu(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CaveDebugNetwork.openMenu(player, collectLines(context.getSource()), "NewTerraForged Terrain Debug");
        return 1;
    }

    private static List<String> collectLines(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.getLevel();
        BlockPos pos = player.blockPosition();
        List<String> lines = new ArrayList<>();
        lines.add("=== NewTerraForged Terrain Debug ===");
        lines.add(String.format("pos %d %d %d", pos.getX(), pos.getY(), pos.getZ()));

        Generator generator = GeneratorPreset.getGenerator(level);
        if (generator == null) {
            lines.add("Not a NewTerraForged generator");
            return lines;
        }

        IBiomeSampler sampler = generator.getBiomeSource().getBiomeSampler();
        ClimateSample sample = sampler.getSample(pos.getX(), pos.getZ());
        Holder<Biome> worldBiome = level.getBiome(pos);
        lines.add("world biome: " + worldBiome.unwrapKey().map(k -> k.location().toString()).orElse("?"));

        if (!(sampler instanceof BiomeSampler biomeSampler)) {
            lines.add("sampler is not BiomeSampler — limited dump");
            lines.add("terrain=" + (sample.terrainType != null ? sample.terrainType.getName() : "null"));
            return lines;
        }

        WeightMap<Holder<Biome>> pool = biomeSampler.getBiomeMapManager().getBiomeMap().get(sample.climateType);
        Holder<Biome> fallback = biomeSampler.getBiomeMapManager().getBiomes().getHolderOrThrow(Biomes.PLAINS);
        lines.addAll(BiomeTerrainIntegration.explainPick(
                sample.biomeNoise,
                sample,
                pool,
                fallback,
                generator.getNoiseGenerator(),
                pos.getX(),
                pos.getZ()));
        return lines;
    }
}
