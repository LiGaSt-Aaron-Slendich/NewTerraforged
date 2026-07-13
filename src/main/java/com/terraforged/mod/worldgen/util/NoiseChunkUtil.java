package com.terraforged.mod.worldgen.util;

import com.terraforged.mod.util.ReflectionUtil;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.VanillaGen;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.util.NoopNoise;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.blending.Blender;

public class NoiseChunkUtil {
    private static final Field SURFACE_CACHE = ReflectionUtil.getField(NoiseChunk.class, Long2IntMap.class, f -> true);
    private static final Field NOISE_ROUTER = ReflectionUtil.getField(NoiseBasedChunkGenerator.class, NoiseRouter.class, f -> true);

    public static NoiseChunk getNoiseChunk(ChunkAccess chunk, Generator generator) {
        CompletableFuture<TerrainData> terrainData = generator.getChunkDataAsync(chunk.getPos());
        VanillaGen vanillaGen = generator.getVanillaGen();
        NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(vanillaGen.getNoiseRouter(), () -> NoopNoise.BEARDIFIER, (NoiseGeneratorSettings)vanillaGen.getSettings().value(), vanillaGen.getGlobalFluidPicker(), Blender.empty());
        NoiseChunkUtil.initChunk(chunk, noiseChunk, terrainData);
        return noiseChunk;
    }

    public static NoiseRouter resolveRouter(NoiseBasedChunkGenerator generator) {
        try {
            return (NoiseRouter)NOISE_ROUTER.get(generator);
        }
        catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }

    private static void initChunk(ChunkAccess chunk, NoiseChunk noiseChunk, CompletableFuture<TerrainData> terrainData) {
        Long2IntMap cache = NoiseChunkUtil.getCache(noiseChunk);
        if (!cache.isEmpty()) {
            return;
        }
        NoiseChunkUtil.initSurfaceCache(chunk, cache, terrainData);
    }

    private static void initSurfaceCache(ChunkAccess chunk, Long2IntMap cache, CompletableFuture<TerrainData> terrainData) {
        ChunkPos chunkPos = chunk.getPos();
        TerrainData data = terrainData.join();
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();
        cache.clear();
        for (int qz = 0; qz < 4; ++qz) {
            for (int qx = 0; qx < 4; ++qx) {
                int bx = Math.min(15, qx * 4 + 2);
                int bz = Math.min(15, qz * 4 + 2);
                int height = data.getHeight(bx, bz);
                int qbx = QuartPos.toBlock(QuartPos.fromBlock(startX + bx));
                int qbz = QuartPos.toBlock(QuartPos.fromBlock(startZ + bz));
                cache.put(ColumnPos.asLong(qbx, qbz), height);
            }
        }
        for (int dz = -16; dz < 32; dz += 4) {
            for (int dx = -16; dx < 32; dx += 4) {
                if ((dx & 0xF) == dx && (dz & 0xF) == dz) {
                    continue;
                }
                int bx = Math.max(0, Math.min(15, dx));
                int bz = Math.max(0, Math.min(15, dz));
                int height = data.getHeight(bx, bz);
                int qbx = QuartPos.toBlock(QuartPos.fromBlock(startX + dx));
                int qbz = QuartPos.toBlock(QuartPos.fromBlock(startZ + dz));
                cache.put(ColumnPos.asLong(qbx, qbz), height);
            }
        }
    }

    private static Long2IntMap getCache(NoiseChunk noiseChunk) {
        try {
            return (Long2IntMap)SURFACE_CACHE.get(noiseChunk);
        }
        catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }
}
