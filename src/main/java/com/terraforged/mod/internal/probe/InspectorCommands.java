package com.terraforged.mod.internal.probe;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.terraforged.mod.platform.forge.ProbeNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

public final class InspectorCommands {
    private InspectorCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!TfProbeConfig.enabled()) {
            return;
        }
        dispatcher.register((LiteralArgumentBuilder)Commands.literal("newtf").then(Commands.literal("ix").requires(s -> s.hasPermission(2)).then(Commands.literal("start").executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            if (!InspectorServerSession.start(player)) {
                player.sendMessage((Component)new TextComponent("Inspector already active — /newtf ix stop").withStyle(ChatFormatting.YELLOW), player.getUUID());
                return 0;
            }
            ProbeNetwork.sendSession(player, true);
            player.sendMessage((Component)new TextComponent("TF Inspector ON — free cam, LMB pick, 1/2/3 modes, Esc stop").withStyle(ChatFormatting.GREEN), player.getUUID());
            return 1;
        })).then(Commands.literal("stop").executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            InspectorServerSession.stop(player);
            ProbeNetwork.sendSession(player, false);
            player.sendMessage((Component)new TextComponent("TF Inspector OFF").withStyle(ChatFormatting.GRAY), player.getUUID());
            return 1;
        }))));
    }
}
