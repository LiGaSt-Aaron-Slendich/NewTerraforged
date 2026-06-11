package com.terraforged.mod.worldgen.cave;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CaveEntranceClaims {
    private final Set<Long> claimed = ConcurrentHashMap.newKeySet();
    private final Set<Long> exitClaimed = ConcurrentHashMap.newKeySet();
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

    public record TunnelAxis(int mouthX, int mouthZ, int exitX, int exitZ) {
    }
}
