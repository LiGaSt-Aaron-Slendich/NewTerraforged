package com.terraforged.mod.internal.probe.client;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Hides vanilla HUD and first-person hands while the inspector is active. */
final class InspectorUiHider {
    private static boolean registered;

    private InspectorUiHider() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        MinecraftForge.EVENT_BUS.register(InspectorUiHider.class);
    }

    @SubscribeEvent
    static void onOverlayLayerPre(RenderGameOverlayEvent.PreLayer event) {
        if (!InspectorClient.isActive()) {
            return;
        }
        var overlay = event.getOverlay();
        if (overlay == ForgeIngameGui.HOTBAR_ELEMENT
                || overlay == ForgeIngameGui.PLAYER_HEALTH_ELEMENT
                || overlay == ForgeIngameGui.FOOD_LEVEL_ELEMENT
                || overlay == ForgeIngameGui.ARMOR_LEVEL_ELEMENT
                || overlay == ForgeIngameGui.EXPERIENCE_BAR_ELEMENT
                || overlay == ForgeIngameGui.AIR_LEVEL_ELEMENT
                || overlay == ForgeIngameGui.MOUNT_HEALTH_ELEMENT
                || overlay == ForgeIngameGui.JUMP_BAR_ELEMENT
                || overlay == ForgeIngameGui.POTION_ICONS_ELEMENT
                || overlay == ForgeIngameGui.VIGNETTE_ELEMENT
                || overlay == ForgeIngameGui.CROSSHAIR_ELEMENT
                || overlay == ForgeIngameGui.ITEM_NAME_ELEMENT
                || overlay == ForgeIngameGui.HELMET_ELEMENT) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        if (InspectorClient.isActive()) {
            event.setCanceled(true);
        }
    }
}
