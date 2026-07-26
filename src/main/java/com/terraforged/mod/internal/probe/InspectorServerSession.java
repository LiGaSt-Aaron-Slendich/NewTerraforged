package com.terraforged.mod.internal.probe;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class InspectorServerSession {
    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> LAST_PROBE_MS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_OVERLAY_MS = new ConcurrentHashMap<>();
    private static final long PROBE_COOLDOWN_MS = 50L;
    private static final long OVERLAY_COOLDOWN_MS = 250L;

    private InspectorServerSession() {
    }

    public static boolean start(ServerPlayer player) {
        return ACTIVE.add(player.getUUID());
    }

    public static boolean stop(ServerPlayer player) {
        UUID id = player.getUUID();
        LAST_PROBE_MS.remove(id);
        LAST_OVERLAY_MS.remove(id);
        return ACTIVE.remove(id);
    }

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.contains(player.getUUID());
    }

    public static boolean allowProbe(ServerPlayer player) {
        return InspectorServerSession.allow(LAST_PROBE_MS, player.getUUID(), PROBE_COOLDOWN_MS);
    }

    public static boolean allowOverlay(ServerPlayer player) {
        return InspectorServerSession.allow(LAST_OVERLAY_MS, player.getUUID(), OVERLAY_COOLDOWN_MS);
    }

    private static boolean allow(Map<UUID, Long> map, UUID id, long cooldownMs) {
        if (!ACTIVE.contains(id)) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long last = map.get(id);
        if (last != null && now - last < cooldownMs) {
            return false;
        }
        map.put(id, now);
        return true;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            InspectorServerSession.stop(player);
        }
    }
}
