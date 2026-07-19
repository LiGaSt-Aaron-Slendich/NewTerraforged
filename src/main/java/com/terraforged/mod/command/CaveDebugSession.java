package com.terraforged.mod.command;

import com.terraforged.mod.command.CaveDebugCommand;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.GeneratorPreset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class CaveDebugSession {
    private static final int REFRESH_INTERVAL_TICKS = 40;
    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> TICKS = new ConcurrentHashMap<UUID, Integer>();

    private CaveDebugSession() {
    }

    public static int start(ServerPlayer player) {
        if (!ACTIVE.add(player.getUUID())) {
            player.sendMessage((Component)new TextComponent("Cave debug already running \u0432\u0402\u201d use /newtf debug cave stop").withStyle(ChatFormatting.YELLOW), player.getUUID());
            return 0;
        }
        TICKS.put(player.getUUID(), 0);
        player.sendMessage((Component)new TextComponent("Cave debug live \u0432\u0402\u201d /newtf debug cave stop to end").withStyle(ChatFormatting.GREEN), player.getUUID());
        CaveDebugSession.sendOnce(player);
        return 1;
    }

    public static int stop(ServerPlayer player) {
        if (!ACTIVE.remove(player.getUUID())) {
            player.sendMessage((Component)new TextComponent("Cave debug was not running").withStyle(ChatFormatting.YELLOW), player.getUUID());
            return 0;
        }
        TICKS.remove(player.getUUID());
        player.sendMessage((Component)new TextComponent("Cave debug stopped").withStyle(ChatFormatting.GRAY), player.getUUID());
        return 1;
    }

    public static void sendOnce(ServerPlayer player) {
        ServerLevel level = player.getLevel();
        Generator generator = GeneratorPreset.getGenerator(level);
        if (generator == null) {
            player.sendMessage((Component)new TextComponent("Not a NewTerraForged world").withStyle(ChatFormatting.RED), player.getUUID());
            return;
        }
        for (String line : CaveDebugCommand.collectLines(generator, level, player.blockPosition(), CaveDebugCommand.Mode.FULL)) {
            player.sendMessage((Component)new TextComponent(line).withStyle(ChatFormatting.GRAY), player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (UUID id : ACTIVE) {
            int tick = TICKS.merge(id, 1, Integer::sum);
            if (tick < 40) continue;
            TICKS.put(id, 0);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null || player.hasDisconnected()) {
                ACTIVE.remove(id);
                TICKS.remove(id);
                continue;
            }
            CaveDebugSession.sendOnce(player);
        }
    }
}
