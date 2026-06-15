/*
 * MIT License
 *
 * Copyright (c) 2021 TerraForged
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.biome.decorator.FeaturePlacement;
import com.terraforged.mod.worldgen.biome.util.BiomeList;
import com.terraforged.mod.worldgen.util.ChunkScopedWorldGenLevel;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Original TerraForged 0.3.x cave decorator (github.com/TerraForged/TerraForged).
 * One decoration origin per cave config per chunk; runs biome features from LOCAL_MODIFICATIONS upward.
 */
public final class TerraForgedOfficialCaveDecorator {
    private static final int FIRST_STAGE = GenerationStep.Decoration.LOCAL_MODIFICATIONS.ordinal();
    /** Minimum blocks below local surface for decoration origin (mega/giga caves). */
    private static final int MEGA_GIGA_ORIGIN_DEPTH = 16;
    /** Minimum blocks below local surface for decoration origin (other caves). */
    private static final int DEFAULT_ORIGIN_DEPTH = 14;

    private TerraForgedOfficialCaveDecorator() {
    }

    public static void decorate(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, NoiseCave config) {
        BiomeList biomes = carver.getBiomes(config);
        if (biomes == null || biomes.size() == 0) {
            return;
        }
        int seed = Seeds.get(region);
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        int startY = config.getHeight(seed, startX, startZ);
        BlockPos pos = TerraForgedOfficialCaveDecorator.clampOrigin(chunk, carver, startX, startY, startZ);
        if (pos == null) {
            return;
        }
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(region.getSeed()));
        WorldGenLevel guarded = ChunkScopedWorldGenLevel.wrapWithUndergroundGuard(region, chunk, carver);
        for (int i = 0; i < biomes.size(); ++i) {
            Holder<Biome> biome = biomes.get(i);
            TerraForgedOfficialCaveDecorator.decorate(pos, guarded, generator, ((Biome)biome.value()).getGenerationSettings(), random);
        }
    }

    public static void decorateBiome(List<BlockPos> origins, ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, Holder<Biome> biome) {
        if (origins == null || origins.isEmpty()) {
            return;
        }
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(region.getSeed()));
        WorldGenLevel guarded = ChunkScopedWorldGenLevel.wrapWithUndergroundGuard(region, chunk, carver);
        BiomeGenerationSettings settings = ((Biome)biome.value()).getGenerationSettings();
        for (BlockPos pos : origins) {
            BlockPos clamped = TerraForgedOfficialCaveDecorator.clampOrigin(chunk, carver, pos.getX(), pos.getY(), pos.getZ());
            if (clamped == null) {
                continue;
            }
            random.setDecorationSeed(region.getSeed(), clamped.getX(), clamped.getZ());
            TerraForgedOfficialCaveDecorator.decorate(clamped, guarded, generator, settings, random);
        }
    }

    private static BlockPos clampOrigin(ChunkAccess chunk, CarverChunk carver, int x, int y, int z) {
        int lx = x & 0xF;
        int lz = z & 0xF;
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        boolean megaGiga = carver != null && carver.isColumnCacheReady() && carver.columnCache().anyMegaGiga();
        boolean entrance = carver != null && carver.isEntranceColumn(lx, lz);
        int minDepth = entrance ? CaveUndergroundGuard.ENTRANCE_ANCHOR_DEPTH + 2 : (megaGiga ? MEGA_GIGA_ORIGIN_DEPTH : DEFAULT_ORIGIN_DEPTH);
        int maxY = surface - minDepth;
        if (y > maxY) {
            y = maxY;
        }
        if (y < chunk.getMinBuildHeight() + 4 || y >= surface - 2) {
            return null;
        }
        return new BlockPos(x, y, z);
    }

    private static void decorate(BlockPos pos, WorldGenLevel region, Generator generator, BiomeGenerationSettings settings, WorldgenRandom random) {
        var features = settings.features();
        if (features.isEmpty()) {
            return;
        }
        long baseSeed = random.setDecorationSeed(region.getSeed(), pos.getX(), pos.getZ());
        int lastStage = Math.min(features.size() - 1, GenerationStep.Decoration.FLUID_SPRINGS.ordinal());
        for (int stageIndex = FIRST_STAGE; stageIndex <= lastStage; ++stageIndex) {
            HolderSet<PlacedFeature> stage = features.get(stageIndex);
            if (stage == null || stage.size() == 0) {
                continue;
            }
            for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
                Holder<PlacedFeature> placed = stage.get(featureIndex);
                if (TerraForgedOfficialCaveDecorator.shouldSkipFeature(placed)) {
                    continue;
                }
                random.setFeatureSeed(baseSeed, featureIndex, stageIndex);
                FeaturePlacement.place(placed, region, (ChunkGenerator)generator, (Random)random, pos, true);
            }
        }
    }

    /** Skip full geode shells and other features that pierce the surface from a single origin. */
    private static boolean shouldSkipFeature(Holder<PlacedFeature> placed) {
        return placed.unwrapKey().map(key -> TerraForgedOfficialCaveDecorator.isBlockedFeaturePath(key.location().getPath())).orElse(false);
    }

    private static boolean isBlockedFeaturePath(String path) {
        String lower = path.toLowerCase();
        return lower.contains("geode") || lower.contains("mega_geode") || lower.contains("crystal_geode")
                || lower.contains("monster_room") || lower.contains("fossil")
                || lower.startsWith("ore_") || lower.contains("/ore_")
                || lower.contains("lake_lava") || lower.contains("lake_water")
                || lower.contains("spring_lava") || lower.contains("spring_water");
    }
}
