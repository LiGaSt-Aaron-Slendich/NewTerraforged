package com.terraforged.mod.platform.forge;

import com.terraforged.mod.platform.forge.TFConfigWarnings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="newterraforged", bus=Mod.EventBusSubscriber.Bus.FORGE, value={Dist.CLIENT})
public final class TFConfigClientNotifier {
    private TFConfigClientNotifier() {
    }

    @SubscribeEvent
    static void onClientLogin(ClientPlayerNetworkEvent.LoggedInEvent event) {
        try {
            for (String message : TFConfigWarnings.drain()) {
                event.getPlayer().displayClientMessage((Component)new TextComponent(message).withStyle(ChatFormatting.RED), false);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }
}
