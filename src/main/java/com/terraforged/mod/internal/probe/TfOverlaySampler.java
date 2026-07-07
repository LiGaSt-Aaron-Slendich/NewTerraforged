package com.terraforged.mod.internal.probe;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.engine.world.terrain.Terrain;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/** Server-side overlay sampling for the in-game inspector. */
public final class TfOverlaySampler {
    private static final int STRIDE = 4;
    private static final int MAX_COLUMNS = 512;
    private static final int BIOME_RADIUS_MAX = 96;
    private static final int BIOME_Y_RADIUS = 48;
    private static final int SURFACE_XZ_STRIDE = 2;
    private static final int MAX_BIOME_COLUMNS = 12000;

    private static int xzStride(int radius) {
        if (radius >= 64) {
            return 4;
        }
        if (radius >= 32) {
            return 2;
        }
        return 1;
    }

    private static int yStride(int radius) {
        return radius >= 64 ? 2 : 1;
    }

    private TfOverlaySampler() {
    }

    public static List<TfOverlayColumn> sample(ServerLevel level, Generator generator, InspectorOverlayMode mode, BlockPos center, int radius) {
        int r = switch (mode) {
            case BIOMES -> Math.min(Math.max(16, radius), BIOME_RADIUS_MAX);
            default -> Math.max(8, Math.min(48, radius));
        };
        ArrayList<TfOverlayColumn> out = new ArrayList<>();
        int cx = center.getX();
        int cz = center.getZ();
        int cy = center.getY();
        switch (mode) {
            case TERRAIN -> TfOverlaySampler.sampleTerrain(generator, cx, cz, r, out);
            case BIOMES -> {
                TfOverlaySampler.sampleCaveVolumes3D(level, generator, cx, cy, cz, r, out);
                TfOverlaySampler.sampleSurfaceCover2D(level, generator, cx, cy, cz, r, out);
            }
            case FEATURES -> TfOverlaySampler.sampleFeatureAnchors(level, generator, cx, cy, cz, r, out);
        }
        return out;
    }

    private static void sampleTerrain(Generator generator, int cx, int cz, int radius, List<TfOverlayColumn> out) {
        for (int dx = -radius; dx <= radius; dx += STRIDE) {
            for (int dz = -radius; dz <= radius; dz += STRIDE) {
                if (out.size() >= MAX_COLUMNS) {
                    return;
                }
                int x = cx + dx;
                int z = cz + dz;
                NoiseSample sample = generator.getTerrainSample(x, z);
                Terrain terrain = sample.terrainType;
                String name = terrain == null ? "none" : terrain.getName().toLowerCase(Locale.ROOT);
                int surfaceY = generator.getOceanFloorHeight(x, z);
                int rgb = TfOverlaySampler.colorHash(name);
                out.add(new TfOverlayColumn(x, z, surfaceY - 1, surfaceY + 1, rgb, name));
            }
        }
    }

    /** 3D underground cave biomes — painted air volumes only. */
    private static void sampleCaveVolumes3D(ServerLevel level, Generator generator, int cx, int cy, int cz, int radius, List<TfOverlayColumn> out) {
        int yMinScan = Math.max(level.getMinBuildHeight(), cy - BIOME_Y_RADIUS);
        int yMaxScan = Math.min(level.getMaxBuildHeight() - 1, cy + BIOME_Y_RADIUS);
        int stepXZ = TfOverlaySampler.xzStride(radius);
        int stepY = TfOverlaySampler.yStride(radius);
        ChunkAccessAccessor chunks = new ChunkAccessAccessor(level);
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx += stepXZ) {
            for (int dz = -radius; dz <= radius; dz += stepXZ) {
                if (out.size() >= MAX_BIOME_COLUMNS) {
                    return;
                }
                int x = cx + dx;
                int z = cz + dz;
                int surface = generator.getOceanFloorHeight(x, z);
                ChunkAccess chunk = chunks.getChunk(x >> 4, z >> 4);
                if (chunk == null) {
                    continue;
                }
                int lx = x & 15;
                int lz = z & 15;
                String lastLabel = null;
                int spanMin = Integer.MIN_VALUE;
                int spanMax = Integer.MIN_VALUE;
                int lastRgb = 0;
                for (int y = yMinScan; y <= yMaxScan; y += stepY) {
                    if (out.size() >= MAX_BIOME_COLUMNS) {
                        return;
                    }
                    probe.set(x, y, z);
                    BlockState state = level.getBlockState(probe);
                    boolean openAir = state.isAir() || !state.getFluidState().isEmpty();
                    if (!openAir || y > surface + 1) {
                        TfOverlaySampler.flushSpan(out, x, z, spanMin, spanMax, lastRgb, lastLabel, stepXZ);
                        lastLabel = null;
                        continue;
                    }
                    Holder<Biome> biome = CarverChunk.readPaintedBiomeAt(chunk, lx, y, lz);
                    if (biome == null || !CaveBiomeIds.isUndergroundBiome(biome) && !CaveBiomeIds.isModCaveBiome(biome)) {
                        TfOverlaySampler.flushSpan(out, x, z, spanMin, spanMax, lastRgb, lastLabel, stepXZ);
                        lastLabel = null;
                        continue;
                    }
                    ResourceLocation id = biome.unwrapKey().map(k -> k.location()).orElse(null);
                    if (id == null) {
                        continue;
                    }
                    String label = id.toString();
                    int rgb = TfOverlaySampler.colorHash(label);
                    if (label.equals(lastLabel)) {
                        spanMax = y + stepY - 1;
                    } else {
                        TfOverlaySampler.flushSpan(out, x, z, spanMin, spanMax, lastRgb, lastLabel, stepXZ);
                        lastLabel = label;
                        spanMin = y;
                        spanMax = y + stepY - 1;
                        lastRgb = rgb;
                    }
                }
                TfOverlaySampler.flushSpan(out, x, z, spanMin, spanMax, lastRgb, lastLabel, stepXZ);
            }
        }
    }

    /** 2D surface cover — top solid block column, overworld biomes only. */
    private static void sampleSurfaceCover2D(ServerLevel level, Generator generator, int cx, int cy, int cz, int radius, List<TfOverlayColumn> out) {
        Source source = generator.getBiomeSource();
        ChunkAccessAccessor chunks = new ChunkAccessAccessor(level);
        for (int dx = -radius; dx <= radius; dx += SURFACE_XZ_STRIDE) {
            for (int dz = -radius; dz <= radius; dz += SURFACE_XZ_STRIDE) {
                if (out.size() >= MAX_BIOME_COLUMNS) {
                    return;
                }
                int x = cx + dx;
                int z = cz + dz;
                ChunkAccess chunk = chunks.getChunk(x >> 4, z >> 4);
                if (chunk == null) {
                    continue;
                }
                int lx = x & 15;
                int lz = z & 15;
                int surfaceY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                if (cy + 24 < surfaceY) {
                    continue;
                }
                Holder<Biome> surfaceBiome = source.getNoiseBiome(x >> 2, surfaceY >> 2, z >> 2, Source.NOOP_CLIMATE_SAMPLER);
                if (CaveBiomeIds.isUndergroundBiome(surfaceBiome) || CaveBiomeIds.isModCaveBiome(surfaceBiome)) {
                    continue;
                }
                ResourceLocation id = surfaceBiome.unwrapKey().map(k -> k.location()).orElse(null);
                if (id == null) {
                    continue;
                }
                String label = "surface:" + id;
                int rgb = TfOverlaySampler.colorHash(label);
                out.add(new TfOverlayColumn(x, z, surfaceY, surfaceY, rgb, label, true, SURFACE_XZ_STRIDE));
            }
        }
    }

    private static void flushSpan(List<TfOverlayColumn> out, int x, int z, int yMin, int yMax, int rgb, String label, int xSize) {
        if (label == null || yMin == Integer.MIN_VALUE || yMax < yMin) {
            return;
        }
        out.add(new TfOverlayColumn(x, z, yMin, yMax, rgb, label, false, Math.max(1, xSize), Math.max(1, xSize)));
    }

    private static void sampleFeatureAnchors(ServerLevel level, Generator generator, int cx, int cy, int cz, int radius, List<TfOverlayColumn> out) {
        int grid = 4;
        int minY = Math.max(level.getMinBuildHeight(), cy - 24);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, cy + 24);
        for (int dx = -radius; dx <= radius; dx += grid) {
            for (int dz = -radius; dz <= radius; dz += grid) {
                if (out.size() >= MAX_COLUMNS) {
                    return;
                }
                int x = cx + dx;
                int z = cz + dz;
                int floorY = TfOverlaySampler.findFloorY(level, x, cy, z);
                if (floorY < minY || floorY > maxY) {
                    continue;
                }
                BlockPos pos = new BlockPos(x, floorY, z);
                BlockState floor = level.getBlockState(pos);
                if (!floor.isAir() && floor.getFluidState().isEmpty() && floor.getBlock() != Blocks.BEDROCK) {
                    int rgb = TfOverlaySampler.colorHash("anchor:" + (x / grid) + ":" + (z / grid));
                    out.add(new TfOverlayColumn(x, z, floorY, floorY, rgb, "anchor"));
                }
            }
        }
    }

    private static int findFloorY(ServerLevel level, int x, int hintY, int z) {
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        int start = Math.min(level.getMaxBuildHeight() - 2, hintY + 16);
        int end = Math.max(level.getMinBuildHeight() + 1, hintY - 32);
        for (int y = start; y >= end; --y) {
            probe.set(x, y, z);
            BlockState state = level.getBlockState(probe);
            BlockState above = level.getBlockState(probe.above());
            if (!state.isAir() && state.getFluidState().isEmpty() && (above.isAir() || !above.getFluidState().isEmpty())) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    static int colorHash(String key) {
        int hash = key.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = hash & 0x0000FF;
        r = 64 + (r * 191 / 255);
        g = 64 + (g * 191 / 255);
        b = 64 + (b * 191 / 255);
        return r << 16 | g << 8 | b;
    }

    /** Avoid repeated getChunk in tight loops. */
    private static final class ChunkAccessAccessor {
        private final ServerLevel level;
        private net.minecraft.world.level.chunk.ChunkAccess cached;
        private int cachedX;
        private int cachedZ;

        ChunkAccessAccessor(ServerLevel level) {
            this.level = level;
        }

        ChunkAccess getChunk(int chunkX, int chunkZ) {
            if (this.cached != null && this.cachedX == chunkX && this.cachedZ == chunkZ) {
                return this.cached;
            }
            this.cached = this.level.getChunk(chunkX, chunkZ);
            this.cachedX = chunkX;
            this.cachedZ = chunkZ;
            return this.cached;
        }
    }
}
