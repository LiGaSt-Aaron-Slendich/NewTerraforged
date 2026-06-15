package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveColumnScan;
import com.terraforged.mod.worldgen.cave.CaveOceanFilter;
import com.terraforged.mod.worldgen.cave.CaveOpenAirCheck;
import com.terraforged.mod.worldgen.cave.CaveSurfaceBiomeRestorer;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
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

/**
 * Entrance decoration modelled on real cave mouths:
 * <ul>
 *   <li>Sinkhole / doline — rim vegetation and surface cover restored at the breach.</li>
 *   <li>Hillside adit — surface biome features via vanilla pass (trees, grass, water), not underground scatter.</li>
 *   <li>Coastal grotto — dripstone teeth and ceiling accents at the sea-level opening.</li>
 * </ul>
 * Uses {@code placeWithBiomeCheck} with a blacklist (skip underground-only features), not a fragile whitelist.
 */
public final class CaveEntranceVanillaDecorator {
    private static final int GRID = 2;
    private static final ResourceLocation DRIPSTONE_CLUSTER = new ResourceLocation("minecraft", "dripstone_cluster");
    private static final ResourceLocation POINTED_DRIPSTONE = new ResourceLocation("minecraft", "pointed_dripstone");
    private static final ResourceLocation LARGE_DRIPSTONE = new ResourceLocation("minecraft", "large_dripstone");

    private CaveEntranceVanillaDecorator() {
    }

    public static void decorate(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator) {
        if (!carver.hasAnyEntranceColumn()) {
            return;
        }
        Source source = generator.getBiomeSource();
        int climateSeed = Seeds.get((int)generator.getSeed());
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        int sea = generator.getSeaLevel();
        WorldgenRandom random = new WorldgenRandom((RandomSource)new LegacyRandomSource(region.getSeed()));
        Registry<PlacedFeature> featureRegistry = region.registryAccess().registryOrThrow(Registry.PLACED_FEATURE_REGISTRY);
        Holder<PlacedFeature> dripstoneCluster = CaveEntranceVanillaDecorator.resolve(featureRegistry, DRIPSTONE_CLUSTER);
        Holder<PlacedFeature> pointedDripstone = CaveEntranceVanillaDecorator.resolve(featureRegistry, POINTED_DRIPSTONE);
        Holder<PlacedFeature> largeDripstone = CaveEntranceVanillaDecorator.resolve(featureRegistry, LARGE_DRIPSTONE);
        for (int lx = 0; lx < 16; lx += GRID) {
            for (int lz = 0; lz < 16; lz += GRID) {
                if (!carver.isEntranceColumn(lx, lz)) {
                    continue;
                }
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                int floorY = CaveEntranceVanillaDecorator.findEntranceFloor(chunk, lx, lz, minY, Math.min(maxY, surface + 6));
                if (floorY < 0 || !CaveOpenAirCheck.isSunFloor(chunk, lx, floorY, lz)) {
                    continue;
                }
                Holder<Biome> surfaceBiome = CaveSurfaceBiomeRestorer.resolveSurfaceBiome(source, climateSeed, wx, wz, surface);
                if (surfaceBiome == null) {
                    continue;
                }
                int groundY = floorY - 1;
                CaveEntranceVanillaDecorator.restoreRimCover(chunk, lx, groundY, lz, surfaceBiome);
                CaveEntranceVanillaDecorator.paintSurfaceBiome(chunk, lx, groundY, lz, surfaceBiome);
                long seed = random.setDecorationSeed(region.getSeed(), wx, wz);
                BlockPos floorPos = new BlockPos(wx, floorY, wz);
                CaveEntranceVanillaDecorator.placeSurfaceVanillaPass(chunk, region, generator, random, seed, floorPos, surfaceBiome);
                if (carver.isCoastalEntranceColumn(lx, lz) && CaveOceanFilter.isNearSea(generator, wx, wz)) {
                    CaveEntranceVanillaDecorator.decorateCoastalGrotto(chunk, region, generator, random, seed, wx, wz, lx, lz, floorY, surface, sea, maxY, dripstoneCluster, pointedDripstone, largeDripstone);
                }
            }
        }
    }

    private static void placeSurfaceVanillaPass(ChunkAccess chunk, WorldGenLevel region, Generator generator, WorldgenRandom random, long seed, BlockPos origin, Holder<Biome> surfaceBiome) {
        BiomeGenerationSettings settings = ((Biome)surfaceBiome.value()).getGenerationSettings();
        List<?> stages = settings.features();
        int lakes = GenerationStep.Decoration.LAKES.ordinal();
        int vegetal = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        int topLayer = GenerationStep.Decoration.TOP_LAYER_MODIFICATION.ordinal();
        if (lakes < stages.size()) {
            CaveEntranceVanillaDecorator.placeStage((HolderSet<PlacedFeature>)stages.get(lakes), region, generator, random, seed, origin, 0);
        }
        if (vegetal < stages.size()) {
            CaveEntranceVanillaDecorator.placeStage((HolderSet<PlacedFeature>)stages.get(vegetal), region, generator, random, seed, origin, 1);
        }
        if (topLayer < stages.size()) {
            CaveEntranceVanillaDecorator.placeStage((HolderSet<PlacedFeature>)stages.get(topLayer), region, generator, random, seed, origin, 2);
        }
    }

    private static void placeStage(HolderSet<PlacedFeature> features, WorldGenLevel region, Generator generator, WorldgenRandom random, long seed, BlockPos pos, int salt) {
        int index = 0;
        for (Holder<PlacedFeature> holder : features) {
            ResourceLocation featureId = FeatureMassClassifier.featurePath(holder);
            if (CaveEntranceVanillaDecorator.isUndergroundOnlyFeature(holder) || featureId != null && CaveFeatureFilters.isDeadWoodFeature(featureId.getPath().toLowerCase())) {
                ++index;
                continue;
            }
            random.setFeatureSeed(seed, index, salt);
            try {
                ((PlacedFeature)holder.value()).placeWithBiomeCheck(region, (ChunkGenerator)generator, (Random)random, pos);
            }
            catch (RuntimeException ignored) {
            }
            ++index;
        }
    }

    private static void decorateCoastalGrotto(ChunkAccess chunk, WorldGenLevel region, Generator generator, WorldgenRandom random, long seed, int wx, int wz, int lx, int lz, int floorY, int surface, int sea, int maxY, Holder<PlacedFeature> cluster, Holder<PlacedFeature> pointed, Holder<PlacedFeature> large) {
        int ceilY = CaveColumnScan.findCeilingAboveFloor(chunk, lx, lz, floorY + 3, Math.min(maxY, surface - 1));
        if (ceilY <= floorY + 2) {
            return;
        }
        float humidity = 0.5f + Math.min(1.0f, (float)(surface - sea) * 0.02f);
        if (pointed != null && random.nextFloat() < humidity * 0.85f) {
            random.setFeatureSeed(seed, 11, 0);
            CaveEntranceVanillaDecorator.tryPlace(pointed, region, generator, random, new BlockPos(wx, ceilY, wz));
        }
        if (cluster != null && random.nextFloat() < humidity * 0.55f) {
            random.setFeatureSeed(seed, 12, 0);
            CaveEntranceVanillaDecorator.tryPlace(cluster, region, generator, random, new BlockPos(wx, ceilY - 1, wz));
        }
        int midY = floorY + (ceilY - floorY) / 2;
        if (large != null && random.nextFloat() < humidity * 0.35f) {
            random.setFeatureSeed(seed, 13, 0);
            CaveEntranceVanillaDecorator.tryPlace(large, region, generator, random, new BlockPos(wx, midY, wz));
        }
        if (pointed != null && floorY > sea + 2 && random.nextFloat() < 0.4f) {
            random.setFeatureSeed(seed, 14, 0);
            CaveEntranceVanillaDecorator.tryPlace(pointed, region, generator, random, new BlockPos(wx, floorY + 1, wz));
        }
    }

    private static void tryPlace(Holder<PlacedFeature> feature, WorldGenLevel region, Generator generator, WorldgenRandom random, BlockPos pos) {
        try {
            ((PlacedFeature)feature.value()).placeWithBiomeCheck(region, (ChunkGenerator)generator, (Random)random, pos);
        }
        catch (RuntimeException ignored) {
        }
    }

    /** Skip features that belong underground — everything else may decorate the entrance rim. */
    private static boolean isUndergroundOnlyFeature(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if (path.contains("monster_room") || path.contains("fossil") || path.contains("spring_lava") || path.contains("lake_lava")) {
            return true;
        }
        if (path.startsWith("cave/") || path.contains("/cave/")) {
            return true;
        }
        if (path.contains("ore_") || path.contains("geode") || path.contains("amethyst") || path.contains("sculk")) {
            return true;
        }
        if (path.contains("dripstone") && !id.getNamespace().equals("minecraft")) {
            return true;
        }
        return path.contains("fungal") || path.contains("mycotoxic") || path.contains("thermal") && path.contains("cave");
    }

    private static int findEntranceFloor(ChunkAccess chunk, int lx, int lz, int minY, int maxY) {
        return CaveColumnScan.findLowestFloor(chunk, lx, lz, minY, maxY, y -> {
            BlockPos pos = new BlockPos(lx, y, lz);
            return chunk.getBlockState(pos).isAir() && !chunk.getBlockState(pos.below()).isAir();
        });
    }

    private static void restoreRimCover(ChunkAccess chunk, int lx, int groundY, int lz, Holder<Biome> biome) {
        BlockPos pos = new BlockPos(lx, groundY, lz);
        BlockState state = chunk.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return;
        }
        if (state.is(BlockTags.LEAVES)) {
            return;
        }
        BlockState top = CaveEntranceVanillaDecorator.pickTopBlock(biome, state);
        chunk.setBlockState(pos, top, false);
        if (top.is(Blocks.GRASS_BLOCK) || top.is(Blocks.PODZOL) || top.is(Blocks.MYCELIUM)) {
            for (int dy = 1; dy <= 3; ++dy) {
                BlockState below = chunk.getBlockState(pos.below(dy));
                if (below.isAir() || !below.getFluidState().isEmpty()) {
                    break;
                }
                if (!below.is(BlockTags.DIRT) && !below.is(Blocks.STONE) && !below.is(Blocks.GRAVEL)) {
                    break;
                }
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
        if (path.contains("mycelium") || path.contains("mushroom") || path.contains("fungal")) {
            return Blocks.MYCELIUM.defaultBlockState();
        }
        if (path.contains("podzol") || path.contains("taiga")) {
            return Blocks.PODZOL.defaultBlockState();
        }
        if (current.is(Blocks.GRAVEL)) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        return Blocks.GRASS_BLOCK.defaultBlockState();
    }

    private static void paintSurfaceBiome(ChunkAccess chunk, int lx, int groundY, int lz, Holder<Biome> biome) {
        int sectionIndex = chunk.getSectionIndex(groundY);
        LevelChunkSection section = chunk.getSection(sectionIndex);
        PalettedContainer<Holder<Biome>> container = section.getBiomes();
        container.set(lx >> 2, (groundY & 0xF) >> 2, lz >> 2, biome);
    }

    private static Holder<PlacedFeature> resolve(Registry<PlacedFeature> registry, ResourceLocation id) {
        return registry.getHolder(ResourceKey.create(Registry.PLACED_FEATURE_REGISTRY, id)).orElse(null);
    }
}
