package com.terraforged.mod.worldgen.util;

import com.terraforged.mod.util.ReflectionUtil;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.VanillaGen;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.util.NoopNoise;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import java.lang.invoke.MethodHandle;
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
    private static final MethodHandle SURFACE_CACHE = ReflectionUtil.field(NoiseChunk.class, Long2IntMap.class, new String[0]);
    private static final MethodHandle NOISE_ROUTER = ReflectionUtil.field(NoiseBasedChunkGenerator.class, NoiseRouter.class, new String[0]);

    public static NoiseChunk getNoiseChunk(ChunkAccess chunk, Generator generator) {
        CompletableFuture<TerrainData> terrainData = generator.getChunkDataAsync(chunk.getPos());
        VanillaGen vanillaGen = generator.getVanillaGen();
        NoiseBasedChunkGenerator vanilla = vanillaGen.getVanillaGenerator();
        NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(NoiseChunkUtil.getRouter(vanilla), () -> NoopNoise.BEARDIFIER, (NoiseGeneratorSettings)vanillaGen.getSettings().value(), vanillaGen.getGlobalFluidPicker(), Blender.empty());
        NoiseChunkUtil.initChunk(chunk, noiseChunk, terrainData);
        return noiseChunk;
    }

    private static NoiseRouter getRouter(NoiseBasedChunkGenerator generator) {
        try {
            return (NoiseRouter) NOISE_ROUTER.invoke(generator);
        }
        catch (Throwable e) {
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
        int qz;
        int qx;
        ChunkPos chunkPos = chunk.getPos();
        TerrainData data = terrainData.join();
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();
        cache.clear();
        for (int dz = 0; dz < 16; dz += 4) {
            for (int dx = 0; dx < 16; dx += 4) {
                int height = data.getHeight(dx, dz);
                qx = QuartPos.toBlock((int)QuartPos.fromBlock((int)(startX + dx)));
                qz = QuartPos.toBlock((int)QuartPos.fromBlock((int)(startZ + dz)));
                cache.put(ColumnPos.asLong((int)qx, (int)qz), height);
            }
        }
        int min = cache.values().intStream().min().orElse(64);
        for (int dz = -16; dz < 32; dz += 4) {
            for (int dx = -16; dx < 32; dx += 4) {
                if ((dx & 0xF) == dx && (dz & 0xF) == dz) continue;
                qx = QuartPos.toBlock((int)QuartPos.fromBlock((int)(startX + dx)));
                qz = QuartPos.toBlock((int)QuartPos.fromBlock((int)(startZ + dz)));
                cache.put(ColumnPos.asLong((int)qx, (int)qz), min);
            }
        }
    }

    private static Long2IntMap getCache(NoiseChunk noiseChunk) {
        try {
            return (Long2IntMap) SURFACE_CACHE.invoke(noiseChunk);
        }
        catch (Throwable e) {
            throw new Error(e);
        }
    }
}
