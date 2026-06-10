package com.terraforged.mod.worldgen.biome.decorator;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.decorator.FeatureDecorator;
import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import com.terraforged.mod.worldgen.biome.decorator.FeaturePlacement;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CavePlacementFilter;
import com.terraforged.mod.worldgen.cave.MegaCaveStructureFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public class VanillaDecorator {
    private static final ResourceLocation END_SPIKE = new ResourceLocation("minecraft", "end_spike");

    public static void decorate(long seed, int from, int to, BlockPos origin, Holder<Biome> biome, ChunkAccess chunk, WorldGenLevel level, Generator generator, WorldgenRandom random, StructureFeatureManager structureManager, FeatureDecorator decorator) {
        if (CaveBiomeIds.isUndergroundBiome(biome)) {
            return;
        }
        for (int stage = from; stage <= to; ++stage) {
            List<Holder<ConfiguredStructureFeature<?, ?>>> structures = decorator.getStageStructures(stage);
            HolderSet<PlacedFeature> features = decorator.getStageFeatures(stage, (Biome)biome.value());
            if (features == null) continue;
            VanillaDecorator.placeStructures(seed, stage, chunk, level, generator, random, structureManager, structures);
            VanillaDecorator.placeFeatures(seed, structures.size(), stage, origin, chunk, level, generator, random, features);
        }
    }

    private static void placeStructures(long seed, int stage, ChunkAccess chunk, WorldGenLevel level, Generator generator, WorldgenRandom random, StructureFeatureManager structureManager, List<Holder<ConfiguredStructureFeature<?, ?>>> structures) {
        ChunkPos chunkPos = chunk.getPos();
        SectionPos sectionPos = SectionPos.of((ChunkPos)chunkPos, (int)level.getMinSection());
        for (int structureIndex = 0; structureIndex < structures.size(); ++structureIndex) {
            random.setFeatureSeed(seed, structureIndex, stage);
            Holder<ConfiguredStructureFeature<?, ?>> structure = structures.get(structureIndex);
            if (VanillaDecorator.shouldSkipStructure(structure, level) || MegaCaveStructureFilter.shouldSkip(generator, chunk.getPos().getWorldPosition(), structure)) continue;
            List starts = structureManager.startsForFeature(sectionPos, (ConfiguredStructureFeature)structure.value());
            for (int startIndex = 0; startIndex < starts.size(); ++startIndex) {
                StructureStart start = (StructureStart)starts.get(startIndex);
                start.placeInChunk(level, structureManager, (ChunkGenerator)generator, (Random)random, VanillaDecorator.getWritableArea(chunk), chunkPos);
            }
        }
    }

    private static void placeFeatures(long seed, int offset, int stage, BlockPos origin, ChunkAccess chunk, WorldGenLevel level, Generator generator, WorldgenRandom random, HolderSet<PlacedFeature> features) {
        int wx = origin.getX();
        int wz = origin.getZ();
        boolean megaCave = MegaCaveStructureFilter.isInMegaOrGigaCave(generator, wx, wz);
        for (int i = 0; i < features.size(); ++i) {
            Holder feature = features.get(i);
            if (megaCave && VanillaDecorator.isBlockedMegaGigaFeature((Holder<PlacedFeature>)feature) || FeatureMassClassifier.isTree((Holder<PlacedFeature>)feature) && (megaCave || CavePlacementFilter.shouldSkipTree(generator, chunk, wx, origin.getY(), wz) || !FeaturePlacement.hasStableGround((BlockGetter)level, origin, 2))) continue;
            random.setFeatureSeed(seed, offset + i, stage);
            ((PlacedFeature)feature.value()).placeWithBiomeCheck(level, (ChunkGenerator)generator, (Random)random, origin);
        }
    }

    private static boolean isBlockedMegaGigaFeature(Holder<PlacedFeature> feature) {
        return feature.unwrapKey().map(key -> {
            String path = key.location().getPath().toLowerCase();
            return path.contains("geode") || path.contains("amethyst");
        }).orElse(false);
    }

    private static boolean shouldSkipStructure(Holder<ConfiguredStructureFeature<?, ?>> structure, WorldGenLevel level) {
        if (level.getLevel().dimension() == Level.END) {
            return false;
        }
        return structure.unwrapKey().map(key -> {
            String path = key.location().getPath().toLowerCase();
            if (key.location().equals(END_SPIKE)) {
                return true;
            }
            return path.contains("end_city") || path.contains("end_gateway") || path.contains("end_spike");
        }).orElse(false);
    }

    public static Map<GenerationStep.Decoration, List<Holder<ConfiguredStructureFeature<?, ?>>>> buildStructureMap(RegistryAccess access) {
        final var map = new EnumMap<GenerationStep.Decoration, List<Holder<ConfiguredStructureFeature<?, ?>>>>(GenerationStep.Decoration.class);
        final var registry = access.registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
        for (var entry : registry.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            map.computeIfAbsent(value.feature.step(), s -> new ArrayList<>()).add(registry.getHolderOrThrow(key));
        }
        for (var stage : FeatureDecorator.STAGES) {
            if (!map.containsKey(stage)) {
                map.put(stage, Collections.emptyList());
            }
        }
        return map;
    }

    private static BoundingBox getWritableArea(ChunkAccess chunkAccess) {
        ChunkPos chunkPos = chunkAccess.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        LevelHeightAccessor levelHeightAccessor = chunkAccess.getHeightAccessorForGeneration();
        int minY = levelHeightAccessor.getMinBuildHeight() + 1;
        int maxY = levelHeightAccessor.getMaxBuildHeight() - 1;
        return new BoundingBox(minX, minY, minZ, minX + 15, maxY, minZ + 15);
    }
}
