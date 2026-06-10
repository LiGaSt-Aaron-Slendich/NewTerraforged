package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import com.terraforged.mod.worldgen.biome.decorator.FeaturePlacement;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveColumnScan;
import com.terraforged.mod.worldgen.cave.CaveOpenAirCheck;
import com.terraforged.mod.worldgen.cave.CaveSurfaceBiomeRestorer;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class CaveEntranceSurfaceDecorator {
    private static final int GRID = 2;

    private CaveEntranceSurfaceDecorator() {
    }

    public static void decorate(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator) {
        Source source = generator.getBiomeSource();
        int climateSeed = Seeds.get((int)generator.getSeed());
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        WorldgenRandom random = new WorldgenRandom((RandomSource)new LegacyRandomSource(region.getSeed()));
        for (int lx = 0; lx < 16; lx += 2) {
            for (int lz = 0; lz < 16; lz += 2) {
                Holder<Biome> surfaceBiome;
                if (!carver.isEntranceColumn(lx, lz)) continue;
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                int floorY = CaveEntranceSurfaceDecorator.findEntranceFloor(chunk, lx, lz, minY, Math.min(maxY, surface + 4));
                if (floorY < 0 || (surfaceBiome = CaveSurfaceBiomeRestorer.resolveSurfaceBiome(source, climateSeed, wx, wz, surface)) == null) continue;
                int groundY = floorY - 1;
                if (!CaveOpenAirCheck.isSunFloor(chunk, lx, floorY, lz)) continue;
                CaveEntranceSurfaceDecorator.restoreSurfaceCover(chunk, lx, groundY, lz, surfaceBiome);
                CaveEntranceSurfaceDecorator.paintSurfaceBiome(chunk, lx, groundY, lz, surfaceBiome);
                CaveEntranceSurfaceDecorator.placeSurfaceFeatures(chunk, region, generator, random, wx, floorY, wz, surfaceBiome);
            }
        }
    }

    private static int findEntranceFloor(ChunkAccess chunk, int lx, int lz, int minY, int maxY) {
        return CaveColumnScan.findLowestFloor(chunk, lx, lz, minY, maxY, y -> {
            BlockPos pos = new BlockPos(lx, y, lz);
            return chunk.getBlockState(pos).isAir() && !chunk.getBlockState(pos.below()).isAir();
        });
    }

    private static void restoreSurfaceCover(ChunkAccess chunk, int lx, int groundY, int lz, Holder<Biome> biome) {
        BlockPos pos = new BlockPos(lx, groundY, lz);
        BlockState state = chunk.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return;
        }
        if (state.is(BlockTags.LEAVES) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL)) {
            return;
        }
        BlockState top = CaveEntranceSurfaceDecorator.pickTopBlock(biome, state);
        chunk.setBlockState(pos, top, false);
        if (top.is(Blocks.GRASS_BLOCK) || top.is(Blocks.PODZOL) || top.is(Blocks.MYCELIUM)) {
            BlockState below;
            for (int dy = 1; dy <= 3 && !(below = chunk.getBlockState(pos.below(dy))).isAir() && below.getFluidState().isEmpty() && (below.is(BlockTags.DIRT) || below.is(Blocks.STONE) || below.is(Blocks.GRAVEL)); ++dy) {
                chunk.setBlockState(pos.below(dy), Blocks.DIRT.defaultBlockState(), false);
            }
        }
    }

    private static BlockState pickTopBlock(Holder<Biome> biome, BlockState current) {
        String path = biome.unwrapKey().map(key -> key.location().getPath().toLowerCase()).orElse("");
        if (path.contains("river") || path.contains("beach") || path.contains("gravel")) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        if (path.contains("snow") || path.contains("tundra") || path.contains("frozen") || path.contains("ice")) {
            return Blocks.SNOW_BLOCK.defaultBlockState();
        }
        if (path.contains("desert") || path.contains("badlands") || path.contains("dune")) {
            return Blocks.SAND.defaultBlockState();
        }
        if (path.contains("mycelium") || path.contains("mushroom")) {
            return Blocks.MYCELIUM.defaultBlockState();
        }
        if (path.contains("podzol") || path.contains("taiga") || path.contains("giant_tree_taiga")) {
            return Blocks.PODZOL.defaultBlockState();
        }
        if (current.is(Blocks.GRAVEL)) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        return Blocks.GRASS_BLOCK.defaultBlockState();
    }

    private static void paintSurfaceBiome(ChunkAccess chunk, int lx, int groundY, int lz, Holder<Biome> biome) {
        int biomeX = lx >> 2;
        int biomeZ = lz >> 2;
        int biomeY = (groundY & 0xF) >> 2;
        int sectionIndex = chunk.getSectionIndex(groundY);
        LevelChunkSection section = chunk.getSection(sectionIndex);
        PalettedContainer container = section.getBiomes();
        container.set(biomeX, biomeY, biomeZ, biome);
    }

    private static void placeSurfaceFeatures(ChunkAccess chunk, WorldGenLevel region, Generator generator, WorldgenRandom random, int wx, int floorY, int wz, Holder<Biome> biome) {
        int vegetal;
        BiomeGenerationSettings settings = ((Biome)biome.value()).getGenerationSettings();
        List stages = settings.features();
        long seed = random.setDecorationSeed(region.getSeed(), wx, wz);
        BlockPos pos = new BlockPos(wx, floorY, wz);
        int lakes = GenerationStep.Decoration.LAKES.ordinal();
        if (lakes < stages.size()) {
            CaveEntranceSurfaceDecorator.placeStage((HolderSet<PlacedFeature>)((HolderSet)stages.get(lakes)), region, generator, random, seed, pos, 0);
        }
        if ((vegetal = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal()) < stages.size()) {
            CaveEntranceSurfaceDecorator.placeStage((HolderSet<PlacedFeature>)((HolderSet)stages.get(vegetal)), region, generator, random, seed, pos, 1);
        }
    }

    private static void placeStage(HolderSet<PlacedFeature> features, WorldGenLevel region, Generator generator, WorldgenRandom random, long seed, BlockPos pos, int salt) {
        int index = 0;
        for (Holder holder : features) {
            if (!CaveEntranceSurfaceDecorator.isEntranceSurfaceFeature((Holder<PlacedFeature>)holder)) continue;
            random.setFeatureSeed(seed, index, salt);
            FeaturePlacement.place((Holder<PlacedFeature>)holder, region, (ChunkGenerator)generator, (Random)random, pos, true);
            ++index;
        }
    }

    private static boolean isEntranceSurfaceFeature(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if (path.contains("cave") || path.contains("dripstone") || path.contains("spore_blossom") || path.contains("geode") || path.contains("amethyst") || path.contains("crystal") || path.contains("mushroom") || path.contains("fungal") || path.contains("lichen") || path.contains("ore_") || path.contains("monster_room") || path.contains("fossil") || path.contains("spring_lava") || path.contains("lake_lava") || path.contains("patch_grass") || path.contains("patch_tall_grass") || path.contains("patch_fern") || path.contains("flower_") || path.contains("tall_grass") || path.contains("bush") || path.contains("bamboo") || path.contains("sugar_cane")) {
            return false;
        }
        if (FeatureMassClassifier.isTree(path)) {
            return true;
        }
        return path.contains("river") || path.contains("lake_water");
    }
}
