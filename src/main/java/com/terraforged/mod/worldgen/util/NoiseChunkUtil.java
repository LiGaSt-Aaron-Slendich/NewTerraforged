package com.terraforged.mod.worldgen.util;

import com.terraforged.mod.util.ReflectionUtil;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.VanillaGen;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.blending.Blender;

/**
 * Restored to official TerraForged 1.18.2-0.3.1-alpha-2 behaviour:
 * - NoiseChunk built with {@link NoopNoise#ROUTER} (not vanilla router)
 * - surface-cache halo filled with chunk min height
 */
public class NoiseChunkUtil {
    private static final MethodHandle SURFACE_CACHE = ReflectionUtil.field(NoiseChunk.class, Long2IntMap.class);

    public static void initChunk(ChunkAccess chunk, Generator generator) {
        getNoiseChunk(chunk, generator);
    }

    public static NoiseChunk getNoiseChunk(ChunkAccess chunk, Generator generator) {
        VanillaGen vanilla = generator.getVanillaGen();
        Aquifer.FluidPicker fluidPicker = vanilla.getGlobalFluidPicker();
        NoiseGeneratorSettings settings = vanilla.getSettings().value();
        CompletableFuture<TerrainData> terrainData = generator.getChunkDataAsync(chunk.getPos());
        NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(
                NoopNoise.ROUTER,
                NoopNoise.BEARDIFIER,
                settings,
                fluidPicker,
                Blender.empty()
        );
        initChunk(chunk, noiseChunk, terrainData);
        return noiseChunk;
    }

    private static void initChunk(ChunkAccess chunk, NoiseChunk noiseChunk, CompletableFuture<TerrainData> terrainData) {
        Long2IntMap cache = getCache(noiseChunk);
        if (!cache.isEmpty()) {
            return;
        }
        initSurfaceCache(chunk, cache, terrainData);
    }

    private static void initSurfaceCache(ChunkAccess chunk, Long2IntMap cache, CompletableFuture<TerrainData> terrainData) {
        ChunkPos chunkPos = chunk.getPos();
        TerrainData data = terrainData.join();
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();
        int min = Integer.MAX_VALUE;
        cache.clear();

        // Official TF 1.18 samples every column for min, then pads halo with that min.
        for (int dz = 0; dz < 16; ++dz) {
            for (int dx = 0; dx < 16; ++dx) {
                int height = data.getHeight(dx, dz);
                int qx = QuartPos.toBlock(QuartPos.fromBlock(startX + dx));
                int qz = QuartPos.toBlock(QuartPos.fromBlock(startZ + dz));
                cache.put(ColumnPos.asLong(qx, qz), height);
                min = Math.min(min, height);
            }
        }

        for (int dz = -16; dz < 32; ++dz) {
            for (int dx = -16; dx < 32; ++dx) {
                if ((dx & 0xF) == dx && (dz & 0xF) == dz) {
                    continue;
                }
                int qx = QuartPos.toBlock(QuartPos.fromBlock(startX + dx));
                int qz = QuartPos.toBlock(QuartPos.fromBlock(startZ + dz));
                cache.put(ColumnPos.asLong(qx, qz), min);
            }
        }
    }

    private static Long2IntMap getCache(NoiseChunk noiseChunk) {
        try {
            return (Long2IntMap) SURFACE_CACHE.invokeExact(noiseChunk);
        } catch (Throwable e) {
            throw new Error(e);
        }
    }
}
