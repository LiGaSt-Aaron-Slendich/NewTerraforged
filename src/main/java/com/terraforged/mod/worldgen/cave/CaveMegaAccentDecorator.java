package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.biome.decorator.FeaturePlacement;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveColumnScan;
import com.terraforged.mod.worldgen.cave.CaveFeaturePlacement;
import com.terraforged.mod.worldgen.cave.CaveFeatureRules;
import com.terraforged.mod.worldgen.cave.CaveModifiers;
import com.terraforged.mod.worldgen.cave.CaveNoise;
import com.terraforged.mod.worldgen.cave.CaveUndergroundGuard;
import com.terraforged.mod.worldgen.cave.MegaCaveStructureFilter;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class CaveMegaAccentDecorator {
    private static final int FLOOR_GRID = 7;
    private static final int CRYSTAL_FLOOR_GRID = 6;
    private static final int CEILING_GRID = 6;
    private static final int MIN_WALL_SPAN = 8;
    private static final ResourceLocation LARGE_DRIPSTONE = new ResourceLocation("minecraft", "large_dripstone");
    private static final ResourceLocation DRIPSTONE_CLUSTER = new ResourceLocation("minecraft", "dripstone_cluster");
    private static final ResourceLocation POINTED_DRIPSTONE = new ResourceLocation("minecraft", "pointed_dripstone");
    private static final ResourceLocation GLOW_LICHEN = new ResourceLocation("minecraft", "glow_lichen");
    private static final ResourceLocation CLASSIC_VINES = new ResourceLocation("minecraft", "classic_vines_cave_feature");
    private static final ResourceLocation FROSTFIRE_COLUMNS = new ResourceLocation("terralith", "cave/frostfire/columns");
    private static final ResourceLocation ICE_CRYSTAL = new ResourceLocation("terralith", "cave/ice/crystal_inside");
    private static final ResourceLocation[] TERRALITH_ICE_CEILING = new ResourceLocation[]{new ResourceLocation("terralith", "cave/ice/icicle"), new ResourceLocation("terralith", "cave/ice/crystal_inside")};
    private static final ResourceLocation[] TERRALITH_CRYSTAL_ACCENTS = new ResourceLocation[]{new ResourceLocation("terralith", "cave/crystal/large_crystal"), new ResourceLocation("terralith", "cave/crystal/crystal_growth"), new ResourceLocation("terralith", "cave/crystal/amethyst/cluster"), new ResourceLocation("terralith", "cave/crystal/amethyst/bud"), new ResourceLocation("terralith", "cave/crystal/small_crystal"), new ResourceLocation("terralith", "cave/crystal/amethyst/crystal_up"), new ResourceLocation("terralith", "cave/crystal/amethyst/crystal_down")};
    private static final ResourceLocation[] PRISMA_ACCENTS = new ResourceLocation[]{new ResourceLocation("regions_unexplored", "prismarite_cluster"), new ResourceLocation("regions_unexplored", "large_prismarite_cluster"), new ResourceLocation("regions_unexplored", "prismoss"), new ResourceLocation("regions_unexplored", "prismoss_vegetation"), new ResourceLocation("regions_unexplored", "cave_hyssop"), new ResourceLocation("terralith", "cave/crystal/small_crystal"), new ResourceLocation("terralith", "cave/crystal/large_crystal")};
    private static final ResourceLocation[] PRISMA_CEILING = new ResourceLocation[]{new ResourceLocation("regions_unexplored", "hanging_prismarite"), new ResourceLocation("regions_unexplored", "large_prismarite_cluster"), new ResourceLocation("regions_unexplored", "prismarite_cluster"), new ResourceLocation("regions_unexplored", "prismoss"), new ResourceLocation("regions_unexplored", "cave_hyssop"), new ResourceLocation("terralith", "cave/crystal/amethyst/crystal_down")};
    private static final ResourceLocation[] MANTLE_ACCENTS = new ResourceLocation[]{new ResourceLocation("terralith", "cave/mantle/lava_drip"), new ResourceLocation("terralith", "cave/mantle/magma_strip"), new ResourceLocation("terralith", "cave/mantle/basalt_strip")};
    private static final ResourceLocation[] VOLCANIC_VENT_ACCENTS = new ResourceLocation[]{new ResourceLocation("regions_unexplored", "ash_vent"), new ResourceLocation("regions_unexplored", "scorching_ash_vent"), new ResourceLocation("regions_unexplored", "scorching/ash_vent"), new ResourceLocation("terralith", "yellowstone/vents"), new ResourceLocation("terralith", "yellowstone/vents_small")};

    private CaveMegaAccentDecorator() {
    }

    public static void decorate(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator) {
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        if (!MegaCaveStructureFilter.isInMegaOrGigaCave(generator, chunkX + 8, chunkZ + 8)) {
            return;
        }
        Holder<Biome> biome;
        int floorY;
        int wz;
        int wx;
        int lz;
        int lx;
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        WorldgenRandom random = new WorldgenRandom((RandomSource)new LegacyRandomSource(region.getSeed()));
        Registry registry = region.registryAccess().registryOrThrow(Registry.PLACED_FEATURE_REGISTRY);
        Holder<PlacedFeature> large = CaveMegaAccentDecorator.resolve((Registry<PlacedFeature>)registry, LARGE_DRIPSTONE);
        Holder<PlacedFeature> dripCluster = CaveMegaAccentDecorator.resolve((Registry<PlacedFeature>)registry, DRIPSTONE_CLUSTER);
        Holder<PlacedFeature> pointed = CaveMegaAccentDecorator.resolve((Registry<PlacedFeature>)registry, POINTED_DRIPSTONE);
        Holder<PlacedFeature> lichen = CaveMegaAccentDecorator.resolve((Registry<PlacedFeature>)registry, GLOW_LICHEN);
        Holder<PlacedFeature> vines = CaveMegaAccentDecorator.resolve((Registry<PlacedFeature>)registry, CLASSIC_VINES);
        Holder<PlacedFeature> frostColumns = CaveMegaAccentDecorator.resolve((Registry<PlacedFeature>)registry, FROSTFIRE_COLUMNS);
        Holder<PlacedFeature> iceCrystal = CaveMegaAccentDecorator.resolve((Registry<PlacedFeature>)registry, ICE_CRYSTAL);
        for (lx = 1; lx < 16; lx += 7) {
            for (lz = 1; lz < 16; lz += 6) {
                int midY;
                int vineY;
                int ceilY;
                wx = chunkX + lx;
                wz = chunkZ + lz;
                floorY = CaveMegaAccentDecorator.findFloor(chunk, lx, lz, minY, maxY);
                if (floorY < 0 || !CaveMegaAccentDecorator.mayPlace(chunk, carver, lx, floorY, lz, generator, wx, wz, biome = carver.resolveBiome(chunk, lx, floorY, lz)) || !CaveBiomeIds.isUndergroundBiome(biome)) continue;
                boolean crystalBiome = CaveBiomeIds.isCrystalCaveBiome(biome);
                boolean prismaBiome = CaveBiomeIds.isPrismachasmBiome(biome);
                boolean scorchingBiome = CaveBiomeIds.isScorchingCaveBiome(biome);
                if (!MegaCaveStructureFilter.isInMegaOrGigaCaveAt(generator, wx, floorY, wz) && !crystalBiome && !prismaBiome && !scorchingBiome) continue;
                boolean volcanic = CaveBiomeIds.isVolcanicCaveBiome(biome);
                boolean mantle = CaveMegaAccentDecorator.isMantleBiome(biome);
                boolean brimstone = CaveMegaAccentDecorator.isBrimstoneBiome(biome);
                boolean cold = CaveMegaAccentDecorator.isColdCaveBiome(biome);
                boolean crystal = CaveBiomeIds.isCrystalCaveBiome(biome);
                boolean ice = CaveMegaAccentDecorator.isIceCaveBiome(biome);
                boolean prisma = CaveBiomeIds.isPrismachasmBiome(biome);
                boolean nearCore = CaveMegaAccentDecorator.isNearMegaGigaCore(generator, wx, wz);
                long seed = random.setDecorationSeed(region.getSeed(), wx, wz);
                boolean bend = CaveMegaAccentDecorator.isBendWall(chunk, lx, floorY, lz, minY, maxY);
                if (prisma) {
                    CaveMegaAccentDecorator.placeFirstMatch((Registry<PlacedFeature>)registry, PRISMA_ACCENTS, region, generator, random, wx, floorY, wz, seed, 20, 4);
                    ceilY = CaveMegaAccentDecorator.findCeiling(chunk, lx, lz, floorY + 4, maxY);
                    if (ceilY > floorY + 5 && CaveMegaAccentDecorator.mayPlace(chunk, carver, lx, ceilY, lz, generator, wx, wz, biome)) {
                        random.setFeatureSeed(seed, 21, 1);
                        CaveMegaAccentDecorator.placeFirstMatch((Registry<PlacedFeature>)registry, PRISMA_CEILING, region, generator, random, wx, ceilY, wz, seed, 22, 5);
                    }
                }
                if (crystal && random.nextFloat() < 0.42f) {
                    CaveMegaAccentDecorator.placeCrystalCluster((Registry<PlacedFeature>)registry, region, generator, random, chunk, carver, biome, wx, floorY, wz, seed, 40);
                }
                boolean scorching = scorchingBiome;
                if (volcanic || scorching) {
                    if (mantle && !brimstone && !scorching && random.nextFloat() < 0.14f) {
                        CaveMegaAccentDecorator.placeFirstMatch((Registry<PlacedFeature>)registry, MANTLE_ACCENTS, region, generator, random, wx, floorY, wz, seed, 30, 5);
                    } else if ((scorching || brimstone || volcanic) && CaveMegaAccentDecorator.isValidVentFloor(chunk, lx, floorY, lz) && random.nextFloat() < (scorching ? 0.62f : 0.42f)) {
                        random.setFeatureSeed(seed, 30, 5);
                        CaveMegaAccentDecorator.placeVentCluster((Registry<PlacedFeature>)registry, region, generator, random, chunk, lx, floorY, lz, wx, wz, seed, scorching ? 2 : 1);
                    }
                } else if (large != null && CaveMegaAccentDecorator.allowsDripstoneAccent(biome) && (bend || random.nextFloat() < 0.38f)) {
                    random.setFeatureSeed(seed, 0, 0);
                    FeaturePlacement.place(large, region, (ChunkGenerator)generator, (Random)random, new BlockPos(wx, floorY, wz), true);
                }
                if (cold && frostColumns != null && !ice && random.nextFloat() < 0.32f) {
                    random.setFeatureSeed(seed, 10, 0);
                    FeaturePlacement.place(frostColumns, region, (ChunkGenerator)generator, (Random)random, new BlockPos(wx, floorY, wz), true);
                }
                if ((ceilY = CaveMegaAccentDecorator.findCeiling(chunk, lx, lz, floorY + 4, maxY)) <= floorY + 5 || !CaveMegaAccentDecorator.mayPlace(chunk, carver, lx, ceilY, lz, generator, wx, wz, biome)) continue;
                if (ice && random.nextFloat() < 0.55f) {
                    random.setFeatureSeed(seed, 12, 1);
                    CaveMegaAccentDecorator.placeFirstMatch((Registry<PlacedFeature>)registry, TERRALITH_ICE_CEILING, region, generator, random, wx, ceilY, wz, seed, 50, 5);
                } else if (crystal && random.nextFloat() < 0.35f) {
                    random.setFeatureSeed(seed, 13, 1);
                    CaveMegaAccentDecorator.placeCrystalCluster((Registry<PlacedFeature>)registry, region, generator, random, chunk, carver, biome, wx, ceilY, wz, seed, 51);
                } else if (cold && iceCrystal != null && random.nextFloat() < 0.38f) {
                    random.setFeatureSeed(seed, 11, 1);
                    FeaturePlacement.place(iceCrystal, region, (ChunkGenerator)generator, (Random)random, new BlockPos(wx, ceilY, wz), true);
                } else if (large != null && CaveMegaAccentDecorator.allowsDripstoneAccent(biome) && random.nextFloat() < 0.44f) {
                    random.setFeatureSeed(seed, 1, 1);
                    FeaturePlacement.place(large, region, (ChunkGenerator)generator, (Random)random, new BlockPos(wx, ceilY, wz), true);
                } else if (dripCluster != null && CaveMegaAccentDecorator.allowsDripstoneAccent(biome) && random.nextFloat() < 0.28f) {
                    random.setFeatureSeed(seed, 14, 1);
                    FeaturePlacement.place(dripCluster, region, (ChunkGenerator)generator, (Random)random, new BlockPos(wx, ceilY, wz), true);
                }
                if (vines != null && random.nextFloat() < 0.28f && CaveMegaAccentDecorator.mayPlace(chunk, carver, lx, vineY = ceilY - random.nextInt(3), lz, generator, wx, wz, biome)) {
                    random.setFeatureSeed(seed, 3, 2);
                    FeaturePlacement.place(vines, region, (ChunkGenerator)generator, (Random)random, new BlockPos(wx, vineY, wz), true);
                }
                if (lichen == null || CaveBiomeIds.isModCaveBiome(biome) || !(random.nextFloat() < 0.1f) || !CaveMegaAccentDecorator.mayPlace(chunk, carver, lx, midY = (floorY + ceilY) / 2, lz, generator, wx, wz, biome)) continue;
                random.setFeatureSeed(seed, 2, 2);
                FeaturePlacement.place(lichen, region, (ChunkGenerator)generator, (Random)random, new BlockPos(wx, midY, wz), true);
            }
        }
        for (lx = 1; lx < 16; lx += 4) {
            for (lz = 1; lz < 16; lz += 4) {
                wx = chunkX + lx;
                wz = chunkZ + lz;
                floorY = CaveMegaAccentDecorator.findFloor(chunk, lx, lz, minY, maxY);
                if (floorY < 0 || !CaveBiomeIds.isCrystalCaveBiome(biome = carver.resolveBiome(chunk, lx, floorY, lz)) || CaveBiomeIds.isPrismachasmBiome(biome) || !CaveMegaAccentDecorator.mayPlace(chunk, carver, lx, floorY, lz, generator, wx, wz, biome)) continue;
                long seed = random.setDecorationSeed(region.getSeed(), wx, wz);
                CaveMegaAccentDecorator.placeCrystalCluster((Registry<PlacedFeature>)registry, region, generator, random, chunk, carver, biome, wx, floorY, wz, seed, 60);
                int ceilY = CaveMegaAccentDecorator.findCeiling(chunk, lx, lz, floorY + 4, maxY);
                if (ceilY <= floorY + 5 || !CaveMegaAccentDecorator.mayPlace(chunk, carver, lx, ceilY, lz, generator, wx, wz, biome)) continue;
                random.setFeatureSeed(seed, 61, 1);
                CaveMegaAccentDecorator.placeCrystalCluster((Registry<PlacedFeature>)registry, region, generator, random, chunk, carver, biome, wx, ceilY, wz, seed, 62);
            }
        }
        for (lx = 3; lx < 16; lx += 5) {
            for (lz = 3; lz < 16; lz += 5) {
                wx = chunkX + lx;
                wz = chunkZ + lz;
                floorY = CaveMegaAccentDecorator.findFloor(chunk, lx, lz, minY, maxY);
                if (floorY < 0 || !CaveBiomeIds.isPrismachasmBiome(biome = carver.resolveBiome(chunk, lx, floorY, lz))) continue;
                int ceilY = CaveMegaAccentDecorator.findCeiling(chunk, lx, lz, floorY + 4, maxY);
                if (ceilY <= floorY + 5 || !CaveMegaAccentDecorator.mayPlace(chunk, carver, lx, ceilY, lz, generator, wx, wz, biome) || random.nextFloat() > 0.72f) continue;
                long seed = random.setDecorationSeed(region.getSeed(), wx, wz);
                random.setFeatureSeed(seed, 70, 1);
                CaveMegaAccentDecorator.placeFirstMatch((Registry<PlacedFeature>)registry, PRISMA_CEILING, region, generator, random, wx, ceilY, wz, seed, 71, 5);
            }
        }
        for (lx = 2; lx < 16; lx += 2) {
            for (lz = 2; lz < 16; lz += 2) {
                wx = chunkX + lx;
                wz = chunkZ + lz;
                floorY = CaveMegaAccentDecorator.findFloor(chunk, lx, lz, minY, maxY);
                if (floorY < 0 || !CaveMegaAccentDecorator.mayPlace(chunk, carver, lx, floorY, lz, generator, wx, wz, biome = carver.resolveBiome(chunk, lx, floorY, lz))) continue;
                if (!CaveBiomeIds.isScorchingCaveBiome(biome) && !CaveBiomeIds.isVolcanicCaveBiome(biome) || !CaveMegaAccentDecorator.isValidVentFloor(chunk, lx, floorY, lz)) continue;
                long seed = random.setDecorationSeed(region.getSeed(), wx, wz);
                random.setFeatureSeed(seed, 80, 5);
                CaveMegaAccentDecorator.placeVentCluster((Registry<PlacedFeature>)registry, region, generator, random, chunk, lx, floorY, lz, wx, wz, seed, CaveBiomeIds.isScorchingCaveBiome(biome) ? 2 : 1);
            }
        }
        CaveMegaAccentDecorator.decorateUniversalDripstoneColumns(chunk, carver, region, generator, random, large, dripCluster, minY, maxY, chunkX, chunkZ);
    }

    /** Sparse pass for stone caves missed by the main accent grid — large columns only, no pointed dripstone. */
    private static void decorateUniversalDripstoneColumns(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, WorldgenRandom random, Holder<PlacedFeature> large, Holder<PlacedFeature> dripCluster, int minY, int maxY, int chunkX, int chunkZ) {
        if (large == null && dripCluster == null) {
            return;
        }
        for (int lx = 3; lx < 16; lx += 6) {
            for (int lz = 3; lz < 16; lz += 6) {
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                int floorY = CaveMegaAccentDecorator.findFloor(chunk, lx, lz, minY, maxY);
                if (floorY < 0) continue;
                Holder<Biome> biome = carver.resolveBiome(chunk, lx, floorY, lz);
                if (!CaveBiomeIds.isUndergroundBiome(biome) || !CaveMegaAccentDecorator.allowsDripstoneAccent(biome) || CaveBiomeIds.isScorchingCaveBiome(biome) || CaveBiomeIds.isVolcanicCaveBiome(biome) || CaveBiomeIds.isCrystalCaveBiome(biome) || CaveBiomeIds.isPrismachasmBiome(biome) || CaveMegaAccentDecorator.isIceCaveBiome(biome)) continue;
                int ceilY = CaveMegaAccentDecorator.findCeiling(chunk, lx, lz, floorY + 4, maxY);
                if (ceilY <= floorY + 5) continue;
                long seed = random.setDecorationSeed(region.getSeed(), wx, wz);
                if (large != null && CaveMegaAccentDecorator.mayPlace(chunk, carver, lx, floorY, lz, generator, wx, wz, biome) && random.nextFloat() < 0.32f) {
                    random.setFeatureSeed(seed, 90, 1);
                    FeaturePlacement.place(large, region, (ChunkGenerator)generator, (Random)random, new BlockPos(wx, floorY, wz), true);
                }
                if (large != null && CaveMegaAccentDecorator.mayPlace(chunk, carver, lx, ceilY, lz, generator, wx, wz, biome) && random.nextFloat() < 0.36f) {
                    random.setFeatureSeed(seed, 91, 1);
                    FeaturePlacement.place(large, region, (ChunkGenerator)generator, (Random)random, new BlockPos(wx, ceilY, wz), true);
                } else if (dripCluster != null && CaveMegaAccentDecorator.mayPlace(chunk, carver, lx, ceilY, lz, generator, wx, wz, biome) && random.nextFloat() < 0.24f) {
                    random.setFeatureSeed(seed, 93, 1);
                    FeaturePlacement.place(dripCluster, region, (ChunkGenerator)generator, (Random)random, new BlockPos(wx, ceilY, wz), true);
                }
            }
        }
    }

    private static void placeCrystalCluster(Registry<PlacedFeature> registry, WorldGenLevel region, Generator generator, WorldgenRandom random, ChunkAccess chunk, CarverChunk carver, Holder<Biome> biome, int wx, int y, int wz, long seed, int seedBase) {
        int passes = 1 + random.nextInt(2);
        for (int pass = 0; pass < passes; ++pass) {
            int ox = pass == 0 ? 0 : -2 + random.nextInt(5);
            int oz = pass == 0 ? 0 : -2 + random.nextInt(5);
            int px = wx + ox;
            int pz = wz + oz;
            int lx = px & 0xF;
            int lz = pz & 0xF;
            if (!CaveMegaAccentDecorator.mayPlace(chunk, carver, lx, y, lz, generator, px, pz, biome)) continue;
            random.setFeatureSeed(seed, seedBase + pass, 4 + pass);
            CaveMegaAccentDecorator.placeFirstMatch(registry, TERRALITH_CRYSTAL_ACCENTS, region, generator, random, px, y, pz, seed, seedBase + pass * 3, 4 + pass);
        }
    }

    private static void placeVentCluster(Registry<PlacedFeature> registry, WorldGenLevel region, Generator generator, WorldgenRandom random, ChunkAccess chunk, int lx, int floorY, int lz, int wx, int wz, long seed, int count) {
        for (int i = 0; i < count; ++i) {
            int ox = i == 0 ? 0 : -2 + random.nextInt(5);
            int oz = i == 0 ? 0 : -2 + random.nextInt(5);
            int px = wx + ox;
            int pz = wz + oz;
            int plx = px & 0xF;
            int plz = pz & 0xF;
            int y = CaveMegaAccentDecorator.snapVentFloorY(chunk, plx, floorY, plz);
            if (y < 0) continue;
            random.setFeatureSeed(seed, 32 + i, 5);
            BlockPos placePos = CaveFeaturePlacement.resolveWorldPos(new BlockPos(px, y, pz), CaveFeatureRules.Anchor.FLOOR, false);
            CaveMegaAccentDecorator.placeFirstMatch(registry, VOLCANIC_VENT_ACCENTS, region, generator, random, placePos.getX(), placePos.getY(), placePos.getZ(), seed, 33 + i, 5);
        }
    }

    private static void placeVentOnFloor(Registry<PlacedFeature> registry, WorldGenLevel region, Generator generator, WorldgenRandom random, ChunkAccess chunk, int lx, int floorY, int lz, int wx, int wz, long seed) {
        int y = CaveMegaAccentDecorator.snapVentFloorY(chunk, lx, floorY, lz);
        if (y < 0) {
            return;
        }
        BlockPos placePos = CaveFeaturePlacement.resolveWorldPos(new BlockPos(wx, y, wz), CaveFeatureRules.Anchor.FLOOR, false);
        CaveMegaAccentDecorator.placeFirstMatch(registry, VOLCANIC_VENT_ACCENTS, region, generator, random, placePos.getX(), placePos.getY(), placePos.getZ(), seed, 32, 5);
    }

    private static int snapVentFloorY(ChunkAccess chunk, int lx, int startY, int lz) {
        for (int dy = 0; dy >= -8; --dy) {
            int y = startY + dy;
            if (y <= chunk.getMinBuildHeight() || !CaveMegaAccentDecorator.isValidVentFloor(chunk, lx, y, lz)) continue;
            return y;
        }
        return -1;
    }

    private static boolean isValidVentFloor(ChunkAccess chunk, int lx, int floorY, int lz) {
        if (!FeaturePlacement.hasStableGround((BlockGetter)chunk, lx, floorY, lz, 2)) {
            return false;
        }
        for (int dy = 1; dy <= 1; ++dy) {
            if (!chunk.getBlockState(new BlockPos(lx, floorY + dy, lz)).isAir()) {
                return false;
            }
        }
        return true;
    }

    private static void placeFirstMatch(Registry<PlacedFeature> registry, ResourceLocation[] ids, WorldGenLevel region, Generator generator, WorldgenRandom random, int wx, int y, int wz, long seed, int seedBase, int stage) {
        for (int i = 0; i < ids.length; ++i) {
            Holder<PlacedFeature> feature = CaveMegaAccentDecorator.resolve(registry, ids[i]);
            if (feature == null) continue;
            random.setFeatureSeed(seed, seedBase + i, stage);
            if (!FeaturePlacement.place(feature, region, (ChunkGenerator)generator, (Random)random, new BlockPos(wx, y, wz), true)) continue;
            return;
        }
    }

    private static boolean isNearMegaGigaCore(Generator generator, int wx, int wz) {
        int s = Seeds.get(generator.getSeed());
        float giga = CaveNoise.sample(CaveModifiers.giga(), s, wx, wz);
        if (giga > 0.06f) {
            return true;
        }
        float mega = CaveNoise.sample(CaveModifiers.mega(), s, wx, wz);
        return mega > 0.18f;
    }

    private static boolean allowsDripstoneAccent(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> {
            String path = key.location().getPath().toLowerCase();
            return CaveBiomeIds.isEmptyStoneCave(key.location()) || path.contains("dripstone") || path.contains("grotto") || path.contains("tuff") || path.contains("andesite") || path.contains("granite") || path.contains("diorite") || "minecraft".equals(key.location().getNamespace()) && path.contains("cave");
        }).orElse(false);
    }

    private static boolean isIceCaveBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> {
            String path = key.location().getPath().toLowerCase();
            return path.contains("ice_caves") || path.contains("icicle_caves");
        }).orElse(false);
    }

    private static boolean mayPlace(ChunkAccess chunk, CarverChunk carver, int lx, int y, int lz, Generator generator, int wx, int wz, Holder<Biome> biome) {
        boolean mega = MegaCaveStructureFilter.isInMegaOrGigaCaveAt(generator, wx, y, wz);
        return CaveUndergroundGuard.mayPlaceAnchorForBiome(chunk, carver, lx, y, lz, biome, mega, carver.isEntranceColumn(lx, lz));
    }

    private static boolean isMantleBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> key.location().getPath().toLowerCase().contains("mantle")).orElse(false);
    }

    private static boolean isBrimstoneBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> key.location().getPath().toLowerCase().contains("brimstone")).orElse(false);
    }

    private static boolean isColdCaveBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> {
            String path = key.location().getPath().toLowerCase();
            return path.contains("frost") || path.contains("ice") || path.contains("icicle") || path.contains("subzero") || path.contains("glacier") || path.contains("shattered_glacier");
        }).orElse(false);
    }

    private static Holder<PlacedFeature> resolve(Registry<PlacedFeature> registry, ResourceLocation id) {
        return registry.getHolder(ResourceKey.create(Registry.PLACED_FEATURE_REGISTRY, (ResourceLocation)id)).orElse(null);
    }

    private static int findFloor(ChunkAccess chunk, int lx, int lz, int minY, int maxY) {
        return CaveColumnScan.findTopValidFloor(chunk, lx, lz, minY, maxY, y -> true);
    }

    private static int findCeiling(ChunkAccess chunk, int lx, int lz, int floorY, int maxY) {
        return CaveColumnScan.findCeilingAboveFloor(chunk, lx, lz, floorY, maxY);
    }

    private static boolean isBendWall(ChunkAccess chunk, int lx, int y, int lz, int minY, int maxY) {
        int[][] dirs;
        for (int[] dir : dirs = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            int above;
            int below;
            BlockState wall = chunk.getBlockState(new BlockPos(lx + dir[0], y, lz + dir[1]));
            if (wall.isAir() || !wall.isSolidRender((BlockGetter)chunk, new BlockPos(lx + dir[0], y, lz + dir[1])) || (below = CaveMegaAccentDecorator.countAirRun(chunk, lx, lz, y, -1, minY, 14)) + (above = CaveMegaAccentDecorator.countAirRun(chunk, lx, lz, y, 1, maxY, 14)) < 8) continue;
            return true;
        }
        return false;
    }

    private static int countAirRun(ChunkAccess chunk, int lx, int lz, int startY, int step, int limitY, int maxRun) {
        int count = 0;
        for (int y = startY + step; count < maxRun && y >= chunk.getMinBuildHeight() && y <= limitY && chunk.getBlockState(new BlockPos(lx, y, lz)).isAir(); ++count, y += step) {
        }
        return count;
    }
}
