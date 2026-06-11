package com.terraforged.mod.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.terraforged.mod.command.CaveDebugSession;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.GeneratorPreset;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.cave.CaveBiomeClimateAffinity;
import com.terraforged.mod.worldgen.cave.CaveBiomeCategory;
import com.terraforged.mod.worldgen.cave.CaveBiomeEntry;
import com.terraforged.mod.worldgen.cave.CavePlacementType;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveBiomeRegistry;
import com.terraforged.mod.worldgen.cave.CaveCartography;
import com.terraforged.mod.worldgen.cave.CaveDebugInfo;
import com.terraforged.mod.worldgen.cave.CaveFeatureDiagnostics;
import com.terraforged.mod.worldgen.cave.CaveLayoutRegionGrid;
import com.terraforged.mod.worldgen.cave.CaveMegaGigaLayout;
import com.terraforged.mod.worldgen.cave.CaveSiteTags;
import com.terraforged.mod.worldgen.cave.CaveStatVector;
import com.terraforged.mod.worldgen.cave.CaveSubtype;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;
import com.terraforged.noise.util.NoiseUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;

public final class CaveDebugCommand {
    private CaveDebugCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"newtf").requires(source -> source.hasPermission(0))).then(Commands.literal((String)"debug").then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"cave").executes(ctx -> CaveDebugCommand.execute((CommandContext<CommandSourceStack>)ctx, Mode.FULL))).then(Commands.literal((String)"start").executes(ctx -> CaveDebugCommand.executeStart((CommandContext<CommandSourceStack>)ctx)))).then(Commands.literal((String)"stop").executes(ctx -> CaveDebugCommand.executeStop((CommandContext<CommandSourceStack>)ctx)))).then(Commands.literal((String)"map").executes(ctx -> CaveDebugCommand.executeMap((CommandContext<CommandSourceStack>)ctx)))).then(((LiteralArgumentBuilder)Commands.literal((String)"stats").then(Commands.literal((String)"local").executes(ctx -> CaveDebugCommand.execute((CommandContext<CommandSourceStack>)ctx, Mode.LOCAL)))).then(Commands.literal((String)"global").executes(ctx -> CaveDebugCommand.execute((CommandContext<CommandSourceStack>)ctx, Mode.GLOBAL))))));
    }

    private static int execute(CommandContext<CommandSourceStack> context, Mode mode) throws CommandSyntaxException {
        ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayerOrException();
        ServerLevel level = player.getLevel();
        Generator generator = GeneratorPreset.getGenerator(level);
        if (generator == null) {
            ((CommandSourceStack)context.getSource()).sendFailure((Component)new TextComponent("Not a NewTerraForged world").withStyle(ChatFormatting.RED));
            return 0;
        }
        BlockPos pos = player.blockPosition();
        for (String line : CaveDebugCommand.collectLines(generator, level, pos, mode)) {
            player.sendMessage((Component)new TextComponent(line).withStyle(ChatFormatting.GRAY), player.getUUID());
        }
        return 1;
    }

    private static int executeStart(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return CaveDebugSession.start(((CommandSourceStack)context.getSource()).getPlayerOrException());
    }

    private static int executeStop(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return CaveDebugSession.stop(((CommandSourceStack)context.getSource()).getPlayerOrException());
    }

    private static int executeMap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayerOrException();
        ServerLevel level = player.getLevel();
        Generator generator = GeneratorPreset.getGenerator(level);
        if (generator == null) {
            ((CommandSourceStack)context.getSource()).sendFailure((Component)new TextComponent("Not a NewTerraForged world").withStyle(ChatFormatting.RED));
            return 0;
        }
        BlockPos pos = player.blockPosition();
        String caveSystem = CaveDebugInfo.resolveCaveSystem(generator, pos.getX(), pos.getY(), pos.getZ());
        if (!"Mega".equals(caveSystem) && !"Giga".equals(caveSystem)) {
            player.sendMessage((Component)new TextComponent("Cartography requires Mega or Giga cave at your position.").withStyle(ChatFormatting.YELLOW), player.getUUID());
            return 0;
        }
        CaveType type = "Giga".equals(caveSystem) ? CaveType.GIGA : CaveType.MEGA;
        CaveCartography.Result result = CaveCartography.render(level, generator, pos.getX(), pos.getZ(), type);
        for (String line : result.chatLines()) {
            player.sendMessage((Component)new TextComponent(line).withStyle(ChatFormatting.GRAY), player.getUUID());
        }
        return 1;
    }

    static List<String> collectLines(Generator generator, ServerLevel level, BlockPos pos, Mode mode) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int seed = Seeds.get(generator.getSeed());
        Source source = generator.getBiomeSource();
        ClimateSample climateSample = source.getBiomeSampler().getSample(seed, x, z);
        NoiseSample terrainSample = generator.getTerrainSample(x, z);
        climateSample.terrainType = terrainSample.terrainType;
        climateSample.baseNoise = terrainSample.baseNoise;
        climateSample.heightNoise = terrainSample.heightNoise;
        int surfaceY = generator.getOceanFloorHeight(x, z);
        Holder<Biome> surfaceBiome = source.getNoiseBiome(x >> 2, 0, z >> 2, Source.NOOP_CLIMATE_SAMPLER);
        String caveSystem = CaveDebugInfo.resolveCaveSystem(generator, x, y, z);
        ArrayList<String> lines = new ArrayList<String>();
        lines.add("=== NewTerraForged Cave Debug ===");
        if (mode == Mode.FULL) {
            lines.add(String.format("Position: %d %d %d (surface Y=%d)", x, y, z, surfaceY));
            lines.add("");
            lines.add("[Cave]");
            lines.add("System: " + caveSystem);
            CaveDebugCommand.appendCaveSubtype(generator, seed, x, z, caveSystem, lines);
            CaveDebugCommand.appendRegionBiome(source, seed, x, y, z, surfaceY, caveSystem, lines);
            CaveDebugCommand.appendPaintedBiome(source, seed, x, y, z, surfaceBiome, surfaceY, caveSystem, lines);
            lines.add("");
            lines.add("[Surface]");
            lines.add("Terrain: " + climateSample.terrainType.getName());
            lines.add("Climate: " + climateSample.climateType.name());
            lines.add(String.format("Ocean proximity: %.3f", Float.valueOf(1.0f - climateSample.continentNoise)));
            lines.add(String.format("River proximity: %.3f (noise=%.3f)", Float.valueOf(1.0f - climateSample.riverNoise), Float.valueOf(climateSample.riverNoise)));
            lines.add("");
        }
        if ("Mega".equals(caveSystem) || "Giga".equals(caveSystem)) {
            CaveType type = "Giga".equals(caveSystem) ? CaveType.GIGA : CaveType.MEGA;
            CaveMegaGigaLayout layout = CaveDebugCommand.resolveLayout(source, seed, x, y, z, type, surfaceBiome, surfaceY);
            if (layout != null) {
                CaveDebugCommand.appendMegaGigaStats(layout, x, z, mode, lines);
                if (mode == Mode.FULL) {
                    CaveDebugCommand.appendLayoutDetails(layout, x, z, lines);
                }
            } else {
                lines.add("Layout: unavailable");
            }
        } else if (mode != Mode.FULL) {
            lines.add("Local/global stats apply only inside Mega or Giga caves.");
        } else {
            lines.add("[Stats]");
            lines.add("Mega/Giga stat pools are not active here (system: " + caveSystem + ").");
        }
        if (mode == Mode.FULL || mode == Mode.LOCAL || mode == Mode.GLOBAL) {
            lines.add("");
            CaveDebugCommand.appendStatEffects(lines);
        }
        if (mode == Mode.FULL) {
            lines.add("");
            CaveDebugCommand.appendRegistrySummary(source, lines);
            lines.add("");
            CaveFeatureDiagnostics.append(generator, (LevelReader)level, pos, lines);
        }
        return lines;
    }

    private static void appendCaveSubtype(Generator generator, int seed, int x, int z, String caveSystem, List<String> lines) {
        if (!"Mega".equals(caveSystem) && !"Giga".equals(caveSystem)) {
            return;
        }
        CaveType type = "Giga".equals(caveSystem) ? CaveType.GIGA : CaveType.MEGA;
        CaveSubtype subtype = CaveSiteTags.detectSubtype(generator, type, seed, x, z);
        if (subtype != CaveSubtype.ANY) {
            lines.add("Subtype: " + subtype.getName());
        }
    }

    private static void appendRegionBiome(Source source, int seed, int x, int y, int z, int surfaceY, String caveSystem, List<String> lines) {
        Holder<Biome> surfaceBiome;
        Holder<Biome> caveBiome;
        if ("Mega".equals(caveSystem) || "Giga".equals(caveSystem)) {
            CaveType type = "Giga".equals(caveSystem) ? CaveType.GIGA : CaveType.MEGA;
            ResourceLocation regionBiome = source.getCaveBiomeSampler().getPrimaryRegionBiomeId(seed, x, z, type);
            if (regionBiome != null) {
                lines.add("Region: " + CaveDebugInfo.formatRegionName(regionBiome));
                lines.add("Region biome: " + regionBiome);
            }
            return;
        }
        if (!"Surface".equals(caveSystem) && CaveBiomeIds.isUndergroundBiome(caveBiome = source.getUnderGroundBiome(seed, x, z, CaveType.GLOBAL, surfaceBiome = CaveDebugCommand.surfaceBiomeFor(source, x, z), y, surfaceY, x, z, 256))) {
            lines.add("Region: " + CaveDebugInfo.formatRegionName(caveBiome));
            caveBiome.unwrapKey().ifPresent(key -> lines.add("Cave biome: " + key.location()));
        }
    }

    private static Holder<Biome> surfaceBiomeFor(Source source, int x, int z) {
        return source.getNoiseBiome(x >> 2, 0, z >> 2, Source.NOOP_CLIMATE_SAMPLER);
    }

    private static void appendPaintedBiome(Source source, int seed, int x, int y, int z, Holder<Biome> surfaceBiome, int surfaceY, String caveSystem, List<String> lines) {
        Holder<Biome> caveBiome;
        if ("Mega".equals(caveSystem) || "Giga".equals(caveSystem)) {
            CaveBiomeEntry entry;
            CaveType type = "Giga".equals(caveSystem) ? CaveType.GIGA : CaveType.MEGA;
            int radius = type == CaveType.GIGA ? 400 : 250;
            int cx = Math.floorDiv(x, radius * 2) * radius * 2 + radius;
            int cz = Math.floorDiv(z, radius * 2) * radius * 2 + radius;
            CaveMegaGigaLayout layout = source.getCaveBiomeSampler().getMegaGigaLayout(seed, cx, cz, radius, type, surfaceBiome, y, surfaceY);
            if (layout != null && (entry = layout.getBiomeAt(x, z)) != null) {
                lines.add("Painted biome: " + entry.biome());
                lines.add("Category: " + CaveDebugCommand.formatLayoutCategory(entry));
            }
            return;
        }
        if (!"Surface".equals(caveSystem) && CaveBiomeIds.isUndergroundBiome(caveBiome = source.getUnderGroundBiome(seed, x, z, CaveType.GLOBAL, surfaceBiome, y, surfaceY, x, z, 256))) {
            caveBiome.unwrapKey().ifPresent(key -> lines.add("Painted biome: " + key.location()));
        }
    }

    private static CaveMegaGigaLayout resolveLayout(Source source, int seed, int x, int y, int z, CaveType type, Holder<Biome> surfaceBiome, int surfaceY) {
        int radius = type == CaveType.GIGA ? 400 : 250;
        int cx = Math.floorDiv(x, radius * 2) * radius * 2 + radius;
        int cz = Math.floorDiv(z, radius * 2) * radius * 2 + radius;
        return source.getCaveBiomeSampler().getMegaGigaLayout(seed, cx, cz, radius, type, surfaceBiome, y, surfaceY);
    }

    private static void appendMegaGigaStats(CaveMegaGigaLayout layout, int x, int z, Mode mode, List<String> lines) {
        CaveStatVector global = layout.globalPool();
        CaveStatVector local = layout.statsAt(x, z);
        float cropFactor = CaveDebugCommand.cropGrowthFactor(local);
        if (mode == Mode.GLOBAL || mode == Mode.FULL) {
            if (mode == Mode.FULL) {
                lines.add("[Mega/Giga - global pool]");
            } else {
                lines.add("[Global pool]");
            }
            lines.add(CaveDebugCommand.formatStatLine("Temperature", global.temperature()));
            lines.add(CaveDebugCommand.formatStatLine("Moisture", global.moisture()));
            lines.add(CaveDebugCommand.formatStatLine("Fertility", global.fertility()));
            lines.add("System climate: " + layout.climateType().name());
            if (mode == Mode.FULL) {
                lines.add("");
            }
        }
        if (mode == Mode.LOCAL || mode == Mode.FULL) {
            if (mode == Mode.FULL) {
                lines.add("[Mega/Giga - local stats @ position]");
            } else {
                lines.add("[Local stats @ position]");
            }
            lines.add(CaveDebugCommand.formatStatLine("Temperature", local.temperature()));
            lines.add(CaveDebugCommand.formatStatLine("Moisture", local.moisture()));
            lines.add(CaveDebugCommand.formatStatLine("Fertility", local.fertility()));
            lines.add(String.format(Locale.ROOT, "Crop growth factor: %.2fx", Float.valueOf(cropFactor)));
        }
    }

    private static void appendLayoutDetails(CaveMegaGigaLayout layout, int x, int z, List<String> lines) {
        List<CaveMegaGigaLayout.GeneratorNode> generators;
        CaveLayoutRegionGrid grid = layout.regionGrid();
        if (grid != null) {
            lines.add(String.format(Locale.ROOT, "Layout cell: ix=%d iz=%d center=(%d,%d) size=%d", grid.regionIndexX(x), grid.regionIndexZ(z), grid.snapX(x), grid.snapZ(z), grid.cellSize()));
        }
        if (!(generators = layout.generators()).isEmpty()) {
            lines.add("Generators:");
            for (CaveMegaGigaLayout.GeneratorNode node : generators) {
                lines.add(String.format(Locale.ROOT, "  - %s @ (%.0f, %.0f)%s", node.biome().biome(), Float.valueOf(node.x()), Float.valueOf(node.z()), CaveDebugCommand.generatorTags(node.biome().biome())));
            }
        }
    }

    private static String generatorTags(ResourceLocation id) {
        StringBuilder tags = new StringBuilder();
        if (CaveBiomeClimateAffinity.isHeatGenerator(id)) {
            tags.append(" [heat]");
        }
        if (CaveBiomeClimateAffinity.isSpringGenerator(id)) {
            tags.append(" [spring/local]");
        }
        if (CaveBiomeClimateAffinity.isColdGenerator(id)) {
            tags.append(" [cold]");
        }
        return tags.toString();
    }

    private static float cropGrowthFactor(CaveStatVector stats) {
        float moistureBoost = 1.0f + stats.moisture() * 0.04f;
        float fertilityBoost = 1.0f + stats.fertility() * 0.05f;
        return NoiseUtil.clamp(moistureBoost * fertilityBoost, 0.25f, 2.5f);
    }

    private static String formatStatLine(String label, float value) {
        return String.format(Locale.ROOT, "%s: %+.2f", label, Float.valueOf(value));
    }

    private static String formatLayoutCategory(CaveBiomeEntry entry) {
        if (entry.placementType() == CavePlacementType.FULL_REGION && (entry.category() == CaveBiomeCategory.TRANSITION || entry.category() == CaveBiomeCategory.COASTAL)) {
            return "PRIMARY (regional shell)";
        }
        return entry.category().name();
    }

    private static void appendStatEffects(List<String> lines) {
        lines.add("[Stat effects]");
        lines.add("Temperature: system climate (FROST/DRY/WET/NORMAL); biome affinity; warm oasis near heat/spring; frostfire cools.");
        lines.add("Moisture: WET classification; crop growth (+4% per point); humid biome selection.");
        lines.add("Fertility: crop growth (+5% per point); minimum pool thresholds for biome spawn in layout.");
        lines.add("Global pool: baseline from surface biome, depth, ocean/river; generator global contributions.");
        lines.add("Local stats: BFS from generators (-25% per hop, max 4 hops); spring uses local only; edge fade at cave boundary.");
    }

    private static void appendRegistrySummary(Source source, List<String> lines) {
        CaveBiomeRegistry registry = source.getCaveBiomeRegistry();
        lines.add("[Cave biome registry]");
        if (registry.isVanillaFallback()) {
            lines.add("Mode: vanilla fallback (no mod cave primaries resolved)");
            return;
        }
        lines.add(String.format(Locale.ROOT, "Loaded: %d primary, %d transition, %d special, %d coastal", registry.getPrimary().size(), registry.getTransition().size(), registry.getSpecial().size(), registry.getCoastal().size()));
        lines.add("Primary pool:");
        for (CaveBiomeEntry entry : registry.getPrimary()) {
            lines.add("  - " + entry.biome());
        }
        lines.add("Mega/giga shell pool: " + registry.getMegaGigaShellPool().size() + " entries");
    }

    public static enum Mode {
        FULL,
        LOCAL,
        GLOBAL;

    }
}
