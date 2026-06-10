package com.terraforged.mod.worldgen.biome.util;

import com.terraforged.mod.worldgen.biome.util.DelegateBiomeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;

public class BufferedBiomeManager
extends DelegateBiomeManager {
    private static final ThreadLocal<BufferedBiomeManager> LOCAL_BIOME_MANAGER = ThreadLocal.withInitial(BufferedBiomeManager::new);
    protected int misses;
    protected int requests;
    protected ChunkPos chunkPos;
    protected final Holder<Biome>[] buffer = new Holder[256];
    protected final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    void set(ChunkPos chunkPos, BiomeManager biomeManager) {
        this.misses = 0;
        this.requests = 0;
        this.chunkPos = chunkPos;
        this.setDelegate(biomeManager);
        int x = chunkPos.getMinBlockX();
        int z = chunkPos.getMinBlockZ();
        for (int dz = 0; dz < 16; ++dz) {
            for (int dx = 0; dx < 16; ++dx) {
                Holder biome;
                this.pos.set(x + dx, 64, z + dz);
                this.buffer[BufferedBiomeManager.index((int)dx, (int)dz)] = biome = biomeManager.getBiome((BlockPos)this.pos);
            }
        }
    }

    @Override
    public Holder<Biome> getBiome(BlockPos pos) {
        ++this.requests;
        int x = pos.getX() >> 4;
        int z = pos.getZ() >> 4;
        if (x == this.chunkPos.x && z == this.chunkPos.z) {
            int dx = pos.getX() - this.chunkPos.getMinBlockX();
            int dz = pos.getZ() - this.chunkPos.getMinBlockZ();
            return this.buffer[BufferedBiomeManager.index(dx, dz)];
        }
        ++this.misses;
        return this.delegate.getBiome(pos);
    }

    public void report() {
    }

    private static int index(int dx, int dz) {
        return dz << 4 | dx;
    }

    public static BufferedBiomeManager assign(ChunkPos chunkPos, BiomeManager biomeManager) {
        BufferedBiomeManager manager = LOCAL_BIOME_MANAGER.get();
        manager.set(chunkPos, biomeManager);
        return manager;
    }
}
