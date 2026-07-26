package com.terraforged.mod.platform.forge;

import com.terraforged.mod.internal.probe.InspectorOverlayMode;
import com.terraforged.mod.internal.probe.InspectorServerSession;
import com.terraforged.mod.internal.probe.TfOverlayColumn;
import com.terraforged.mod.internal.probe.TfOverlaySampler;
import com.terraforged.mod.internal.probe.TfProbeResult;
import com.terraforged.mod.internal.probe.TfProbeService;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.GeneratorPreset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ProbeNetwork {
    private static final String PROTOCOL = "1";
    private static final ResourceLocation CHANNEL_ID = new ResourceLocation("newterraforged", "probe");
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(CHANNEL_ID, () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static boolean registered;

    private ProbeNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.registerMessage(0, SessionToggle.class, SessionToggle::encode, SessionToggle::decode, SessionToggle::handle);
        CHANNEL.registerMessage(1, StopSession.class, StopSession::encode, StopSession::decode, StopSession::handle);
        CHANNEL.registerMessage(2, ProbeRequest.class, ProbeRequest::encode, ProbeRequest::decode, ProbeRequest::handle);
        CHANNEL.registerMessage(3, ProbeResponse.class, ProbeResponse::encode, ProbeResponse::decode, ProbeResponse::handle);
        CHANNEL.registerMessage(4, OverlayRequest.class, OverlayRequest::encode, OverlayRequest::decode, OverlayRequest::handle);
        CHANNEL.registerMessage(5, OverlayResponse.class, OverlayResponse::encode, OverlayResponse::decode, OverlayResponse::handle);
    }

    public static void sendSession(ServerPlayer player, boolean active) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SessionToggle(active));
    }

    public static void sendProbeRequest(BlockPos pos) {
        CHANNEL.sendToServer(new ProbeRequest(pos));
    }

    public static void sendStopSession() {
        CHANNEL.sendToServer(new StopSession());
    }

    public static void sendOverlayRequest(InspectorOverlayMode mode, BlockPos center, int radius) {
        CHANNEL.sendToServer(new OverlayRequest(mode.id, center, radius));
    }

    public static void sendProbeResult(ServerPlayer player, TfProbeResult result) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ProbeResponse(result));
    }

    public static void sendOverlay(ServerPlayer player, InspectorOverlayMode mode, List<TfOverlayColumn> columns) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OverlayResponse(mode.id, columns));
    }

    public record SessionToggle(boolean active) {
        private static void encode(SessionToggle msg, FriendlyByteBuf buf) {
            buf.writeBoolean(msg.active);
        }

        private static SessionToggle decode(FriendlyByteBuf buf) {
            return new SessionToggle(buf.readBoolean());
        }

        private static void handle(SessionToggle msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Class<?> client = ProbeNetwork.clientClass();
                if (client != null) {
                    try {
                        client.getMethod("onSessionToggle", boolean.class).invoke(null, msg.active);
                    }
                    catch (ReflectiveOperationException ignored) {
                    }
                }
            }));
            ctx.get().setPacketHandled(true);
        }
    }

    public record StopSession() {
        private static void encode(StopSession msg, FriendlyByteBuf buf) {
        }

        private static StopSession decode(FriendlyByteBuf buf) {
            return new StopSession();
        }

        private static void handle(StopSession msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    InspectorServerSession.stop(player);
                    ProbeNetwork.sendSession(player, false);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public record ProbeRequest(BlockPos pos) {
        private static void encode(ProbeRequest msg, FriendlyByteBuf buf) {
            buf.writeBlockPos(msg.pos);
        }

        private static ProbeRequest decode(FriendlyByteBuf buf) {
            return new ProbeRequest(buf.readBlockPos());
        }

        private static void handle(ProbeRequest msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null || !InspectorServerSession.allowProbe(player)) {
                    return;
                }
                TfProbeResult result = TfProbeService.probe(player, msg.pos);
                ProbeNetwork.sendProbeResult(player, result);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public record ProbeResponse(TfProbeResult result) {
        private static void encode(ProbeResponse msg, FriendlyByteBuf buf) {
            buf.writeBlockPos(new BlockPos(msg.result.x(), msg.result.y(), msg.result.z()));
            buf.writeVarInt(msg.result.lines().size());
            for (String line : msg.result.lines()) {
                buf.writeUtf(line, 4096);
            }
        }

        private static ProbeResponse decode(FriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            int count = buf.readVarInt();
            ArrayList<String> lines = new ArrayList<>(count);
            for (int i = 0; i < count; ++i) {
                lines.add(buf.readUtf(4096));
            }
            return new ProbeResponse(new TfProbeResult(pos.getX(), pos.getY(), pos.getZ(), lines));
        }

        private static void handle(ProbeResponse msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Class<?> client = ProbeNetwork.clientClass();
                if (client != null) {
                    try {
                        client.getMethod("onProbeResult", TfProbeResult.class).invoke(null, msg.result);
                    }
                    catch (ReflectiveOperationException ignored) {
                    }
                }
            }));
            ctx.get().setPacketHandled(true);
        }
    }

    public record OverlayRequest(byte modeId, BlockPos center, int radius) {
        private static void encode(OverlayRequest msg, FriendlyByteBuf buf) {
            buf.writeByte(msg.modeId);
            buf.writeBlockPos(msg.center);
            buf.writeVarInt(msg.radius);
        }

        private static OverlayRequest decode(FriendlyByteBuf buf) {
            return new OverlayRequest(buf.readByte(), buf.readBlockPos(), buf.readVarInt());
        }

        private static void handle(OverlayRequest msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null || !InspectorServerSession.allowOverlay(player)) {
                    return;
                }
                ServerLevel level = player.getLevel();
                Generator generator = GeneratorPreset.getGenerator(level);
                if (generator == null) {
                    return;
                }
                InspectorOverlayMode mode = InspectorOverlayMode.byId(msg.modeId);
                List<TfOverlayColumn> columns = TfOverlaySampler.sample(level, generator, mode, msg.center, msg.radius);
                ProbeNetwork.sendOverlay(player, mode, columns);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public record OverlayResponse(byte modeId, List<TfOverlayColumn> columns) {
        private static void encode(OverlayResponse msg, FriendlyByteBuf buf) {
            buf.writeByte(msg.modeId);
            buf.writeVarInt(msg.columns.size());
            for (TfOverlayColumn column : msg.columns) {
                buf.writeVarInt(column.x());
                buf.writeVarInt(column.z());
                buf.writeVarInt(column.yMin());
                buf.writeVarInt(column.yMax());
                buf.writeVarInt(column.rgb());
                buf.writeUtf(column.label() == null ? "" : column.label(), 256);
                buf.writeBoolean(column.surface());
                buf.writeVarInt(Math.max(1, column.xSize()));
                buf.writeVarInt(Math.max(1, column.zSize()));
            }
        }

        private static OverlayResponse decode(FriendlyByteBuf buf) {
            byte modeId = buf.readByte();
            int count = buf.readVarInt();
            ArrayList<TfOverlayColumn> columns = new ArrayList<>(count);
            for (int i = 0; i < count; ++i) {
                columns.add(new TfOverlayColumn(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(256), buf.readBoolean(), buf.readVarInt(), buf.readVarInt()));
            }
            return new OverlayResponse(modeId, columns);
        }

        private static void handle(OverlayResponse msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Class<?> client = ProbeNetwork.clientClass();
                if (client != null) {
                    try {
                        client.getMethod("onOverlay", byte.class, List.class).invoke(null, msg.modeId, msg.columns);
                    }
                    catch (ReflectiveOperationException ignored) {
                    }
                }
            }));
            ctx.get().setPacketHandled(true);
        }
    }

    private static Class<?> clientClass() {
        try {
            return Class.forName("com.terraforged.mod.internal.probe.client.InspectorClient");
        }
        catch (ClassNotFoundException e) {
            return null;
        }
    }
}
