package com.terraforged.mod.hooks;

import com.mojang.serialization.Dynamic;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.compat.TerraBlenderCompat;
import com.terraforged.mod.hooks.DatapackHook;
import com.terraforged.mod.hooks.PrimaryLevelDataMarker;
import com.terraforged.mod.hooks.WorldGenHook;
import com.terraforged.mod.hooks.WorldTypeMarker;
import com.terraforged.mod.mixin.common.ChunkMapAccess;
import com.terraforged.mod.mixin.common.PrimaryLevelDataAccess;
import com.terraforged.mod.mixin.common.ServerChunkCacheAccess;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.GeneratorPreset;
import java.nio.file.Path;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;

public final class WorldGeneratorRestorer {
    private WorldGeneratorRestorer() {
    }

    public static void patchSettingsBeforeLevels(MinecraftServer server) {
        WorldData data = server.getWorldData();
        if (!(data instanceof PrimaryLevelData)) {
            return;
        }
        PrimaryLevelData levelData = (PrimaryLevelData)data;
        if (!WorldGeneratorRestorer.shouldRestore(server, levelData)) {
            return;
        }
        try {
            WorldGenSettings fixed = WorldGenHook.applyGenerator((RegistryAccess)server.registryAccess(), data.worldGenSettings());
            ((PrimaryLevelDataAccess)levelData).newtf$setWorldGenSettings(fixed);
            WorldGeneratorRestorer.markWorld(server, levelData);
            DatapackHook.consumeServerApplyPending();
            TerraForged.LOG.info("Restored NewTerraForged world settings before level creation");
        }
        catch (Throwable t) {
            TerraForged.LOG.error("Failed to restore NewTerraForged settings before level creation", t);
        }
    }

    public static void ensureOverworldGenerator(MinecraftServer server) {
        PrimaryLevelData levelData;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        ChunkGenerator current = overworld.getChunkSource().getGenerator();
        if (current instanceof Generator) {
            PrimaryLevelData p;
            WorldData worldData = server.getWorldData();
            WorldGeneratorRestorer.markWorld(server, worldData instanceof PrimaryLevelData ? (p = (PrimaryLevelData)worldData) : null);
            TerraBlenderCompat.onGeneratorActive();
            return;
        }
        WorldData data = server.getWorldData();
        if (!(data instanceof PrimaryLevelData) || !WorldGeneratorRestorer.shouldRestore(server, levelData = (PrimaryLevelData)data)) {
            TerraBlenderCompat.warnVanillaOverworld(current.getClass().getSimpleName());
            return;
        }
        try {
            WorldGenSettings fixed = WorldGenHook.applyGenerator((RegistryAccess)server.registryAccess(), data.worldGenSettings());
            ChunkGenerator newGen = ((LevelStem)fixed.dimensions().getOrThrow(LevelStem.OVERWORLD)).generator();
            if (!(newGen instanceof Generator)) {
                TerraForged.LOG.error("NewTerraForged generator build failed after world load");
                TerraBlenderCompat.warnVanillaOverworld(current.getClass().getSimpleName());
                return;
            }
            Generator tfGen = (Generator)newGen;
            ((PrimaryLevelDataAccess)levelData).newtf$setWorldGenSettings(fixed);
            WorldGeneratorRestorer.hotSwapGenerator(overworld.getChunkSource(), tfGen);
            WorldGeneratorRestorer.markWorld(server, levelData);
            TerraForged.LOG.info("Hot-swapped overworld chunk generator to NewTerraForged after reload");
            TerraBlenderCompat.onGeneratorActive();
        }
        catch (Throwable t) {
            TerraForged.LOG.error("Failed to hot-swap NewTerraForged generator after reload", t);
            TerraBlenderCompat.warnVanillaOverworld(current.getClass().getSimpleName());
        }
    }

    public static boolean shouldRestore(MinecraftServer server, PrimaryLevelData levelData) {
        PrimaryLevelDataMarker marker;
        if (GeneratorPreset.isTerraForgedWorld(levelData.worldGenSettings())) {
            return true;
        }
        if (DatapackHook.shouldPatchServerGenerator()) {
            return true;
        }
        if (levelData instanceof PrimaryLevelDataMarker && (marker = (PrimaryLevelDataMarker)levelData).newtf$isTerraforgedWorld()) {
            return true;
        }
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        if (WorldTypeMarker.hasMarkerFile(worldRoot)) {
            return true;
        }
        if (WorldTypeMarker.hasWorldDatapack(worldRoot)) {
            return true;
        }
        return WorldTypeMarker.hasGeneratorInLevelDat(worldRoot);
    }

    public static void markWorld(MinecraftServer server, PrimaryLevelData levelData) {
        if (levelData instanceof PrimaryLevelDataMarker) {
            PrimaryLevelDataMarker marker = (PrimaryLevelDataMarker)levelData;
            marker.newtf$setTerraforgedWorld(true);
        }
        WorldTypeMarker.writeMarkerFile(server.getWorldPath(LevelResource.ROOT));
    }

    public static boolean readMarkedFromSave(Dynamic<?> dynamic, CompoundTag tag) {
        return WorldTypeMarker.isMarked(tag);
    }

    private static void hotSwapGenerator(ServerChunkCache cache, ChunkGenerator generator) {
        ChunkMap map = ((ServerChunkCacheAccess)cache).newtf$getChunkMap();
        ((ChunkMapAccess)map).newtf$setGenerator(generator);
    }
}
