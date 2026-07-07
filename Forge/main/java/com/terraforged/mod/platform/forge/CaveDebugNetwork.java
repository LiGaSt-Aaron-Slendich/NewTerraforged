package com.terraforged.mod.platform.forge;

import com.terraforged.mod.client.CaveDebugScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.server.level.ServerPlayer;

public final class CaveDebugNetwork {
    private static final String PROTOCOL = "1";
    private static final ResourceLocation CHANNEL_ID = new ResourceLocation("newterraforged", "cave_debug");
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(CHANNEL_ID, () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static boolean registered;

    private CaveDebugNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.registerMessage(0, Payload.class, Payload::encode, Payload::decode, Payload::handle);
    }

    public static void openMenu(ServerPlayer player, List<String> lines) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Payload(lines));
    }

    public static final class Payload {
        private final List<String> lines;

        public Payload(List<String> lines) {
            this.lines = lines;
        }

        private static void encode(Payload msg, FriendlyByteBuf buf) {
            buf.writeVarInt(msg.lines.size());
            for (String line : msg.lines) {
                buf.writeUtf(line, 4096);
            }
        }

        private static Payload decode(FriendlyByteBuf buf) {
            int count = buf.readVarInt();
            ArrayList<String> lines = new ArrayList<>(count);
            for (int i = 0; i < count; ++i) {
                lines.add(buf.readUtf(4096));
            }
            return new Payload(lines);
        }

        private static void handle(Payload msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null) {
                    minecraft.setScreen(new CaveDebugScreen(msg.lines));
                }
            }));
            ctx.get().setPacketHandled(true);
        }
    }
}
