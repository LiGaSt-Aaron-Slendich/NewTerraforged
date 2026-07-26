package com.terraforged.mod.worldgen.cave;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CaveEntranceClaims {
    private final Set<Long> claimed = ConcurrentHashMap.newKeySet();
    private final Set<Long> exitClaimed = ConcurrentHashMap.newKeySet();
    private final Set<Long> grottoCells = ConcurrentHashMap.newKeySet();
    private final Map<Long, TunnelAxis> tunnelAxes = new ConcurrentHashMap<Long, TunnelAxis>();

    public boolean isClaimed(long systemKey) {
        return this.claimed.contains(systemKey);
    }

    public boolean hasExit(long systemKey) {
        return this.exitClaimed.contains(systemKey);
    }

    public boolean tryClaim(long systemKey) {
        return this.claimed.add(systemKey);
    }

    public boolean tryClaimExit(long systemKey) {
        return this.exitClaimed.add(systemKey);
    }

    public void registerTunnel(long systemKey, TunnelAxis axis) {
        this.tunnelAxes.put(systemKey, axis);
    }

    public void registerTunnelIfAbsent(long systemKey, TunnelAxis axis) {
        this.tunnelAxes.putIfAbsent(systemKey, axis);
    }

    public TunnelAxis tunnelAxis(long systemKey) {
        return this.tunnelAxes.get(systemKey);
    }

    public boolean tryClaimGrotto(int worldX, int worldZ) {
        return this.grottoCells.add(CaveEntranceClaims.grottoCellKey(worldX, worldZ));
    }

    public boolean isGrottoClaimed(int worldX, int worldZ, int radiusBlocks) {
        int cell = Math.max(16, radiusBlocks);
        int cx = Math.floorDiv(worldX, cell);
        int cz = Math.floorDiv(worldZ, cell);
        for (int ox = -1; ox <= 1; ++ox) {
            for (int oz = -1; oz <= 1; ++oz) {
                if (this.grottoCells.contains(CaveEntranceClaims.pack(cx + ox, cz + oz))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static long grottoCellKey(int worldX, int worldZ) {
        return CaveEntranceClaims.pack(Math.floorDiv(worldX, 48), Math.floorDiv(worldZ, 48));
    }

    private static long pack(int x, int z) {
        return (long)x << 32 | (long)z & 0xFFFFFFFFL;
    }

    public record TunnelAxis(int mouthX, int mouthZ, int exitX, int exitZ) {
    }
}
