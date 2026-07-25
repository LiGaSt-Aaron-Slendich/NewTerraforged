package com.terraforged.mod.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.terraforged.mod.platform.forge.CaveDebugNetwork;
import com.terraforged.mod.platform.forge.TFCaveBiomeConfig;
import com.terraforged.mod.platform.forge.TFCaveSystemConfig;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.GeneratorPreset;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.cave.CaveBiomeCategory;
import com.terraforged.mod.worldgen.cave.CaveBiomeClimateAffinity;
import com.terraforged.mod.worldgen.cave.CaveBiomeEntry;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveBiomeRegistry;
import com.terraforged.mod.worldgen.cave.CaveBiomeRegistryLoader;
import com.terraforged.mod.worldgen.cave.CaveCartography;
import com.terraforged.mod.worldgen.cave.CaveDebugInfo;
import com.terraforged.mod.worldgen.cave.CaveDebugMaps;
import com.terraforged.mod.worldgen.cave.CaveDebugReport;
import com.terraforged.mod.worldgen.cave.CaveLayoutRegionGrid;
import com.terraforged.mod.worldgen.cave.CaveMegaGigaLayout;
import com.terraforged.mod.worldgen.cave.CavePlacementType;
import com.terraforged.mod.worldgen.cave.CaveStatVector;
import com.terraforged.mod.worldgen.cave.CaveSystemConfig;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;

/**
 * TF118-portable {@code /newtf debug cave}. Feature/carve diagnostics and cartography
 * extras that need excluded KEEP (CaveFeature*, CarveDecisionDiagnostics, MegaGigaZoneProbe)
 * are soft-deferred with clear messages.
 */
public final class CaveDebugCommand {
    private CaveDebugCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("newtf")
            .requires(source -> source.hasPermission(0))
            .then(
                Commands.literal("debug")
                    .then(
                        Commands.literal("cave")
                            .executes(ctx -> CaveDebugCommand.execute(ctx, Mode.FULL))
                            .then(Commands.literal("carve").executes(CaveDebugCommand::executeCarve))
                            .then(Commands.literal("start").executes(CaveDebugCommand::executeStart))
                            .then(Commands.literal("stop").executes(CaveDebugCommand::executeStop))
                            .then(Commands.literal("map").executes(CaveDebugCommand::executeMap))
                            .then(Commands.literal("save").executes(CaveDebugCommand::executeSave))
                            .then(Commands.literal("menu").executes(CaveDebugCommand::executeMenu))
                            .then(
                                Commands.literal("stats")
                                    .then(Commands.literal("local").executes(ctx -> CaveDebugCommand.execute(ctx, Mode.LOCAL)))
                                    .then(Commands.literal("global").executes(ctx -> CaveDebugCommand.execute(ctx, Mode.GLOBAL)))
                            )
                    )
                    .then(TerrainDebugCommand.branch())
            );
    }

    private static int execute(CommandContext<CommandSourceStack> context, Mode mode) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.getLevel();
        Generator generator = GeneratorPreset.getGenerator(level);
        if (generator == null) {
            context.getSource().sendFailure(new TextComponent("Not a NewTerraForged world").withStyle(ChatFormatting.RED));
            return 0;
        }
        BlockPos pos = player.blockPosition();
        try {
            for (String line : CaveDebugCommand.collectLines(generator, level, pos, mode)) {
                player.sendMessage(new TextComponent(line).withStyle(ChatFormatting.GRAY), player.getUUID());
            }
        } catch (Throwable t) {
            context.getSource()
                .sendFailure(
                    new TextComponent("Cave debug failed: " + t.getClass().getSimpleName() + ": " + t.getMessage())
                        .withStyle(ChatFormatting.RED)
                );
            return 0;
        }
        return 1;
    }

    private static int executeStart(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return CaveDebugSession.start(context.getSource().getPlayerOrException());
    }

    private static int executeStop(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return CaveDebugSession.stop(context.getSource().getPlayerOrException());
    }

    private static int executeCarve(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.getLevel();
        Generator generator = GeneratorPreset.getGenerator(level);
        if (generator == null) {
            context.getSource().sendFailure(new TextComponent("Not a NewTerraForged world").withStyle(ChatFormatting.RED));
            return 0;
        }
        BlockPos pos = player.blockPosition();
        try {
            CaveDebugReport report = new CaveDebugReport();
            report.add("=== Carve decision replay ===");
            report.add(String.format(Locale.ROOT, "Position: %d %d %d", pos.getX(), pos.getY(), pos.getZ()));
            report.add("Deferred: CarveDecisionDiagnostics needs CarverColumnCache / main CarverChunk hooks.");
            CaveDebugCommand.appendRiverMaskProbe(generator, pos, report);
            for (String line : report.lines()) {
                player.sendMessage(new TextComponent(line).withStyle(ChatFormatting.GRAY), player.getUUID());
            }
        } catch (Throwable t) {
            context.getSource()
                .sendFailure(
                    new TextComponent("Carve debug failed: " + t.getClass().getSimpleName() + ": " + t.getMessage())
                        .withStyle(ChatFormatting.RED)
                );
            return 0;
        }
        return 1;
    }

    private static int executeMap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.getLevel();
        Generator generator = GeneratorPreset.getGenerator(level);
        if (generator == null) {
            context.getSource().sendFailure(new TextComponent("Not a NewTerraForged world").withStyle(ChatFormatting.RED));
            return 0;
        }
        BlockPos pos = player.blockPosition();
        String caveSystem = CaveDebugInfo.resolveCaveSystem(generator, pos.getX(), pos.getY(), pos.getZ());
        if (!"Mega".equals(caveSystem) && !"Giga".equals(caveSystem)) {
            player.sendMessage(
                new TextComponent("Cartography requires Mega or Giga cave at your position.").withStyle(ChatFormatting.YELLOW),
                player.getUUID()
            );
            return 0;
        }
        CaveType type = "Giga".equals(caveSystem) ? CaveType.GIGA : CaveType.MEGA;
        try {
            CaveCartography.Result result = CaveCartography.render(level, generator, pos.getX(), pos.getZ(), type);
            for (String line : result.chatLines()) {
                player.sendMessage(new TextComponent(line).withStyle(ChatFormatting.GRAY), player.getUUID());
            }
        } catch (Throwable t) {
            context.getSource()
                .sendFailure(
                    new TextComponent("Cartography failed: " + t.getClass().getSimpleName() + ": " + t.getMessage())
                        .withStyle(ChatFormatting.RED)
                );
            return 0;
        }
        return 1;
    }

    private static int executeSave(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.getLevel();
        Generator generator = GeneratorPreset.getGenerator(level);
        if (generator == null) {
            context.getSource().sendFailure(new TextComponent("Not a NewTerraForged world").withStyle(ChatFormatting.RED));
            return 0;
        }
        BlockPos pos = player.blockPosition();
        try {
            CaveDebugReport report = CaveDebugCommand.collectReport(generator, level, pos, Mode.FULL);
            Path path = CaveDebugReport.save(level, pos, report);
            player.sendMessage(new TextComponent("Cave debug saved: " + path).withStyle(ChatFormatting.GREEN), player.getUUID());
            player.sendMessage(
                new TextComponent("HTML copy: " + path.getParent().resolve(path.getFileName().toString().replace(".txt", ".html")))
                    .withStyle(ChatFormatting.GRAY),
                player.getUUID()
            );
        } catch (Throwable t) {
            String message = t instanceof IOException
                ? "Save failed: " + t.getMessage()
                : "Save failed: " + t.getClass().getSimpleName() + ": " + t.getMessage();
            context.getSource().sendFailure(new TextComponent(message).withStyle(ChatFormatting.RED));
            return 0;
        }
        return 1;
    }

    private static int executeMenu(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.getLevel();
        Generator generator = GeneratorPreset.getGenerator(level);
        if (generator == null) {
            context.getSource().sendFailure(new TextComponent("Not a NewTerraForged world").withStyle(ChatFormatting.RED));
            return 0;
        }
        BlockPos pos = player.blockPosition();
        try {
            CaveDebugReport report = CaveDebugCommand.collectReport(generator, level, pos, Mode.FULL);
            CaveDebugNetwork.openMenu(player, report.lines());
            player.sendMessage(
                new TextComponent("Opened cave debug menu (scroll with mouse wheel)").withStyle(ChatFormatting.GREEN),
                player.getUUID()
            );
        } catch (Throwable t) {
            context.getSource()
                .sendFailure(
                    new TextComponent("Menu failed: " + t.getClass().getSimpleName() + ": " + t.getMessage())
                        .withStyle(ChatFormatting.RED)
                );
            return 0;
        }
        return 1;
    }

    public static CaveDebugReport collectReport(Generator generator, ServerLevel level, BlockPos pos, Mode mode) {
        CaveDebugReport report = new CaveDebugReport();
        CaveDebugCommand.appendReportHeader(generator, level, pos, mode, report);
        if (mode == Mode.FULL) {
            report.add("");
            report.add("Tip: feature diagnostics deferred until CaveFeature* KEEP compiles.");
            report.add("Tip: carve replay deferred until CarverColumnCache hooks land.");
            CaveDebugCommand.appendRiverMaskProbe(generator, pos, report);
        }
        report.appendFeatureTable();
        return report;
    }

    static List<String> collectLines(Generator generator, ServerLevel level, BlockPos pos, Mode mode) {
        return CaveDebugCommand.collectReport(generator, level, pos, mode).lines();
    }

    private static void appendRiverMaskProbe(Generator generator, BlockPos pos, CaveDebugReport report) {
        int x = pos.getX();
        int z = pos.getZ();
        TerrainData terrain = generator.getChunkData(new ChunkPos(x >> 4, z >> 4));
        float river = terrain.getRiver().get(x & 15, z & 15);
        report.add("");
        report.add("[TF118 river mask]");
        report.add(String.format(Locale.ROOT, "River factor @ chunk local: %.4f", river));
        report.add("Formula: getCarvingMask = 1 - maskNoise * river (unchanged).");
        // CarverChunk.debug* available when a live carver chunk is bound; terrain river alone is always safe.
        CarverChunk probe = new CarverChunk(1);
        probe.terrainData = terrain;
        report.add(
            String.format(
                Locale.ROOT,
                "debugRiverNoise(local)=%.4f (mask noise needs active carver.mask)",
                probe.debugRiverNoise(x & 15, z & 15)
            )
        );
    }

    private static void appendReportHeader(Generator generator, ServerLevel level, BlockPos pos, Mode mode, CaveDebugReport report) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int seed = Seeds.get(generator.getSeed());
        Source source = generator.getBiomeSource();
        ClimateSample climateSample = source.getBiomeSampler().getSample(x, z);
        int surfaceY = CaveDebugInfo.oceanFloorHeight(generator, x, z);
        Holder<Biome> surfaceBiome = source.getNoiseBiome(x >> 2, 0, z >> 2, Source.NOOP_CLIMATE_SAMPLER);
        String caveSystem = CaveDebugInfo.resolveCaveSystem(generator, x, y, z);
        ArrayList<String> lineBuffer = new ArrayList<>();
        lineBuffer.add("=== NewTerraForged Cave Debug ===");
        if (mode == Mode.FULL) {
            lineBuffer.add(String.format("Position: %d %d %d (surface Y=%d)", x, y, z, surfaceY));
            lineBuffer.add("");
            lineBuffer.add("[Cave]");
            lineBuffer.add("System: " + caveSystem);
            CaveDebugCommand.appendRegionBiome(source, seed, x, y, z, surfaceY, caveSystem, lineBuffer);
            CaveDebugCommand.appendPaintedBiome(generator, seed, x, y, z, surfaceBiome, surfaceY, caveSystem, lineBuffer);
            lineBuffer.add("");
            lineBuffer.add("[Surface]");
            lineBuffer.add("Terrain: " + climateSample.terrainType.getName());
            lineBuffer.add("Climate: " + climateSample.climateType.name());
            lineBuffer.add(String.format("Ocean proximity: %.3f", Float.valueOf(1.0f - climateSample.continentNoise)));
            lineBuffer.add(
                String.format(
                    "River proximity: %.3f (noise=%.3f)",
                    Float.valueOf(1.0f - climateSample.riverNoise),
                    Float.valueOf(climateSample.riverNoise)
                )
            );
            CaveDebugCommand.appendWaterBiomeDiagnostics(source, x, z, climateSample, lineBuffer);
            lineBuffer.add("");
        }
        if ("Mega".equals(caveSystem) || "Giga".equals(caveSystem)) {
            CaveType type = "Giga".equals(caveSystem) ? CaveType.GIGA : CaveType.MEGA;
            CaveMegaGigaLayout layout = CaveDebugInfo.resolveLayout(generator, seed, x, y, z, type);
            if (layout != null) {
                CaveDebugCommand.appendMegaGigaStats(layout, x, z, mode, lineBuffer);
                if (mode == Mode.FULL) {
                    CaveDebugCommand.appendLayoutDetails(layout, x, z, lineBuffer);
                }
            } else {
                lineBuffer.add("Layout: unavailable");
            }
        } else if (mode != Mode.FULL) {
            lineBuffer.add("Local/global stats apply only inside Mega or Giga caves.");
        } else {
            lineBuffer.add("[Stats]");
            lineBuffer.add("Mega/Giga stat pools are not active here (system: " + caveSystem + ").");
        }
        if (mode == Mode.FULL || mode == Mode.LOCAL || mode == Mode.GLOBAL) {
            lineBuffer.add("");
            CaveDebugCommand.appendStatEffects(lineBuffer);
        }
        if (mode == Mode.FULL) {
            lineBuffer.add("");
            CaveDebugCommand.appendRegistrySummary(source, lineBuffer);
        }
        for (String line : lineBuffer) {
            report.add(line);
        }
    }

    private static void appendWaterBiomeDiagnostics(Source source, int x, int z, ClimateSample climateSample, List<String> lines) {
        if (!climateSample.terrainType.isRiver() && !climateSample.terrainType.isLake()) {
            return;
        }
        Holder<Biome> sampled = source.getBiomeSampler().sampleBiome(x, z);
        Holder<Biome> noiseBiome = source.getNoiseBiome(x >> 2, 0, z >> 2, Source.NOOP_CLIMATE_SAMPLER);
        lines.add("[Water biome check]");
        sampled.unwrapKey().ifPresent(key -> lines.add("Sampler biome @ block: " + key.location()));
        noiseBiome.unwrapKey().ifPresent(key -> lines.add("Climate noise biome (F3 surface): " + key.location()));
        if (climateSample.riverNoise != 0.0f) {
            lines.add(
                String.format(
                    Locale.ROOT,
                    "River noise != 0 (%.4f) — vanilla RIVER override in BiomeSampler may NOT apply; climate biome bleeds to shores",
                    climateSample.riverNoise
                )
            );
        } else {
            lines.add("River noise == 0 — BiomeSampler should force minecraft:river / frozen_river here");
        }
    }

    private static void appendRegionBiome(
        Source source,
        int seed,
        int x,
        int y,
        int z,
        int surfaceY,
        String caveSystem,
        List<String> lines
    ) {
        if ("Mega".equals(caveSystem) || "Giga".equals(caveSystem)) {
            return;
        }
        if (!"Surface".equals(caveSystem)) {
            Holder<Biome> caveBiome = source.getUnderGroundBiome(seed, x, z, CaveType.GLOBAL);
            if (CaveBiomeIds.isUndergroundBiome(caveBiome)) {
                lines.add("Region: " + CaveDebugInfo.formatRegionName(caveBiome));
                caveBiome.unwrapKey().ifPresent(key -> lines.add("Cave biome: " + key.location()));
            }
        }
    }

    private static void appendPaintedBiome(
        Generator generator,
        int seed,
        int x,
        int y,
        int z,
        Holder<Biome> surfaceBiome,
        int surfaceY,
        String caveSystem,
        List<String> lines
    ) {
        if ("Mega".equals(caveSystem) || "Giga".equals(caveSystem)) {
            CaveType type = "Giga".equals(caveSystem) ? CaveType.GIGA : CaveType.MEGA;
            CaveMegaGigaLayout layout = CaveDebugInfo.resolveLayout(generator, seed, x, y, z, type);
            CaveBiomeEntry entry;
            if (layout != null && (entry = layout.getBiomeAt(x, z)) != null) {
                lines.add("Painted biome: " + entry.biome());
                lines.add("Category: " + CaveDebugCommand.formatLayoutCategory(entry));
            }
            return;
        }
        if (!"Surface".equals(caveSystem)) {
            Holder<Biome> caveBiome = generator.getBiomeSource().getUnderGroundBiome(seed, x, z, CaveType.GLOBAL);
            if (CaveBiomeIds.isUndergroundBiome(caveBiome)) {
                caveBiome.unwrapKey().ifPresent(key -> lines.add("Painted biome: " + key.location()));
            }
        }
    }

    private static void appendMegaGigaStats(CaveMegaGigaLayout layout, int x, int z, Mode mode, List<String> lines) {
        CaveStatVector global = layout.globalPool();
        CaveStatVector local = layout.statsAt(x, z);
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
            lines.add("Crop/feature scaling: disabled (stats layout-only)");
        }
    }

    private static void appendLayoutDetails(CaveMegaGigaLayout layout, int x, int z, List<String> lines) {
        List<CaveMegaGigaLayout.GeneratorNode> generators;
        CaveLayoutRegionGrid grid = layout.regionGrid();
        if (grid != null) {
            lines.add(
                String.format(
                    Locale.ROOT,
                    "Layout cell: ix=%d iz=%d center=(%d,%d) size=%d",
                    grid.regionIndexX(x),
                    grid.regionIndexZ(z),
                    grid.snapX(x),
                    grid.snapZ(z),
                    grid.cellSize()
                )
            );
        }
        if (!(generators = layout.generators()).isEmpty()) {
            lines.add("Generators:");
            for (CaveMegaGigaLayout.GeneratorNode node : generators) {
                lines.add(
                    String.format(
                        Locale.ROOT,
                        "  - %s @ (%.0f, %.0f)%s",
                        node.biome().biome(),
                        Float.valueOf(node.x()),
                        Float.valueOf(node.z()),
                        CaveDebugCommand.generatorTags(node.biome().biome())
                    )
                );
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

    private static String formatStatLine(String label, float value) {
        return String.format(Locale.ROOT, "%s: %+.2f", label, Float.valueOf(value));
    }

    private static String formatLayoutCategory(CaveBiomeEntry entry) {
        if (entry.placementType() == CavePlacementType.FULL_REGION
            && (entry.category() == CaveBiomeCategory.TRANSITION || entry.category() == CaveBiomeCategory.COASTAL)) {
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
        lines.add("[Cave biome registry]");
        TFCaveBiomeConfig cfg = TFCaveBiomeConfig.INSTANCE;
        if (cfg == null) {
            lines.add("Mode: config not loaded");
            return;
        }
        Registry<Biome> biomes = source.getRegistries().registryOrThrow(Registry.BIOME_REGISTRY);
        CaveBiomeRegistry registry = CaveBiomeRegistryLoader.build(biomes, cfg);
        if (registry.isVanillaFallback()) {
            lines.add("Mode: vanilla fallback (no mod cave primaries resolved)");
            return;
        }
        lines.add(
            String.format(
                Locale.ROOT,
                "Loaded: %d primary, %d transition, %d special, %d coastal",
                registry.getPrimary().size(),
                registry.getTransition().size(),
                registry.getSpecial().size(),
                registry.getCoastal().size()
            )
        );
        lines.add("Primary pool:");
        for (CaveBiomeEntry entry : registry.getPrimary()) {
            lines.add("  - " + entry.biome());
        }
        lines.add("Mega/giga shell pool: " + registry.getMegaGigaShellPool().size() + " entries");
        if (TFCaveSystemConfig.INSTANCE != null) {
            CaveSystemConfig sys = TFCaveSystemConfig.INSTANCE.toSystemConfig();
            lines.add(
                String.format(
                    Locale.ROOT,
                    "System config: mega regions %d-%d, giga %d-%d",
                    sys.megaRegionCountMin(),
                    sys.megaRegionCountMax(),
                    sys.gigaRegionCountMin(),
                    sys.gigaRegionCountMax()
                )
            );
        }
    }

    public enum Mode {
        FULL,
        LOCAL,
        GLOBAL
    }
}
