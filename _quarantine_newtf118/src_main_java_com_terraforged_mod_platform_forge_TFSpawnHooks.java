package com.terraforged.mod.platform.forge;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.compat.TerraBlenderCompat;
import com.terraforged.mod.worldgen.Generator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * World-create hang fix: vanilla {@code setInitialSpawn} can force-generate up to 121
 * full chunks (height 1024 + Ocean Landscape) with no progress UI — looks like 0% forever.
 * We set spawn from noise height only and cancel the vanilla spiral.
 */
public final class TFSpawnHooks {
    private TFSpawnHooks() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        TerraForged.LOG.info("[TFSpawn] ServerAboutToStart — init TerraBlender + spawn hooks armed");
        TerraBlenderCompat.onGeneratorActive(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCreateSpawn(WorldEvent.CreateSpawnPosition event) {
        LevelAccessor world = event.getWorld();
        if (!(world instanceof ServerLevel level)) {
            return;
        }
        if (!(level.getChunkSource().getGenerator() instanceof Generator generator)) {
            return;
        }
        ServerLevelData data = event.getSettings();
        long t0 = System.nanoTime();
        // CONTINENT_CENTER offset already baked into noise — world (8,8) is near land.
        int x = 8;
        int z = 8;
        int y;
        try {
            y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level);
        } catch (Throwable t) {
            TerraForged.LOG.error("[TFSpawn] getBaseHeight failed; using seaLevel+1", t);
            y = generator.getSeaLevel() + 1;
        }
        int min = level.getMinBuildHeight() + 1;
        int max = level.getMaxBuildHeight() - 1;
        if (y < min) {
            y = Math.max(min, generator.getSeaLevel() + 1);
        }
        if (y > max) {
            y = max;
        }
        data.setSpawn(new BlockPos(x, y, z), 0.0F);
        event.setCanceled(true);
        TerraForged.LOG.info(
                "[TFSpawn] spawn set at {},{},{} in {} ms (skipped vanilla 11x11 chunk spiral)",
                x, y, z, (System.nanoTime() - t0) / 1_000_000L);
    }
}
