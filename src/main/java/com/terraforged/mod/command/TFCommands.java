package com.terraforged.mod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.command.Arg;
import com.terraforged.mod.command.CaveDebugCommand;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.GeneratorPreset;
import com.terraforged.mod.worldgen.Regenerator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.cave.CaveLocator;
import com.terraforged.mod.worldgen.cave.CaveSubtype;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.noise.continent.ContinentPreview;
import javax.swing.SwingUtilities;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;

public class TFCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(TFCommands.getLocateTerrainCommand());
        dispatcher.register(TFCommands.getLocateCaveCommand());
        dispatcher.register(TFCommands.getTFCommand());
        dispatcher.register(CaveDebugCommand.register());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return (LiteralArgumentBuilder)Commands.literal((String)name).requires(s -> s.hasPermission(2));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> getLocateTerrainCommand() {
        return (LiteralArgumentBuilder)TFCommands.root("locateterrain").then(((RequiredArgumentBuilder)Arg.terrainType().then(Commands.argument((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)1)).executes(c -> TFCommands.locate((CommandContext<CommandSourceStack>)c, true)))).executes(c -> TFCommands.locate((CommandContext<CommandSourceStack>)c, false)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> getLocateCaveCommand() {
        return (LiteralArgumentBuilder)TFCommands.root("locatecave").then(TFCommands.locateCaveTypeBranch());
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> locateCaveTypeBranch() {
        RequiredArgumentBuilder<CommandSourceStack, String> type = (RequiredArgumentBuilder)Commands.argument((String)"type", (ArgumentType)StringArgumentType.string()).suggests((ctx, builder) -> {
            builder.suggest("giga");
            builder.suggest("mega");
            builder.suggest("grotto");
            return builder.buildFuture();
        }).executes(c -> TFCommands.locateCave((CommandContext<CommandSourceStack>)c, CaveSubtype.ANY, CaveLocator.LocateMode.SYSTEM, -1));
        type.then(Commands.argument((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)256, (int)CaveLocator.MAX_SEARCH_RADIUS)).executes(c -> TFCommands.locateCave((CommandContext<CommandSourceStack>)c, CaveSubtype.ANY, CaveLocator.LocateMode.SYSTEM, IntegerArgumentType.getInteger((CommandContext)c, (String)"radius"))));
        type.then(TFCommands.locateCaveEntranceBranch());
        type.then(TFCommands.locateCaveSubtypeBranch("coastal", CaveSubtype.COASTAL));
        type.then(TFCommands.locateCaveSubtypeBranch("tunnel", CaveSubtype.TUNNEL));
        type.then(TFCommands.locateCaveSubtypeBranch("ogpm", CaveSubtype.TUNNEL));
        return type;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> locateCaveEntranceBranch() {
        LiteralArgumentBuilder<CommandSourceStack> branch = (LiteralArgumentBuilder)Commands.literal((String)"entrance").executes(c -> TFCommands.locateCave((CommandContext<CommandSourceStack>)c, CaveSubtype.ANY, CaveLocator.LocateMode.ENTRANCE, -1));
        branch.then(Commands.argument((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)256, (int)CaveLocator.MAX_SEARCH_RADIUS)).executes(c -> TFCommands.locateCave((CommandContext<CommandSourceStack>)c, CaveSubtype.ANY, CaveLocator.LocateMode.ENTRANCE, IntegerArgumentType.getInteger((CommandContext)c, (String)"radius"))));
        branch.then(TFCommands.locateCaveEntranceSubtypeBranch("coastal", CaveSubtype.COASTAL));
        branch.then(TFCommands.locateCaveEntranceSubtypeBranch("tunnel", CaveSubtype.TUNNEL));
        branch.then(TFCommands.locateCaveEntranceSubtypeBranch("ogpm", CaveSubtype.TUNNEL));
        return branch;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> locateCaveEntranceSubtypeBranch(String name, CaveSubtype subtype) {
        LiteralArgumentBuilder<CommandSourceStack> branch = (LiteralArgumentBuilder)Commands.literal((String)name).executes(c -> TFCommands.locateCave((CommandContext<CommandSourceStack>)c, subtype, CaveLocator.LocateMode.ENTRANCE, -1));
        branch.then(Commands.argument((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)256, (int)CaveLocator.MAX_SEARCH_RADIUS)).executes(c -> TFCommands.locateCave((CommandContext<CommandSourceStack>)c, subtype, CaveLocator.LocateMode.ENTRANCE, IntegerArgumentType.getInteger((CommandContext)c, (String)"radius"))));
        return branch;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> locateCaveSubtypeBranch(String name, CaveSubtype subtype) {
        LiteralArgumentBuilder<CommandSourceStack> branch = (LiteralArgumentBuilder)Commands.literal((String)name).executes(c -> TFCommands.locateCave((CommandContext<CommandSourceStack>)c, subtype, CaveLocator.LocateMode.SYSTEM, -1));
        branch.then(Commands.argument((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)256, (int)CaveLocator.MAX_SEARCH_RADIUS)).executes(c -> TFCommands.locateCave((CommandContext<CommandSourceStack>)c, subtype, CaveLocator.LocateMode.SYSTEM, IntegerArgumentType.getInteger((CommandContext)c, (String)"radius"))));
        return branch;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> getTFCommand() {
        LiteralArgumentBuilder<CommandSourceStack> root = TFCommands.root("tf");
        root.then(Commands.literal("locate").then(Commands.literal("cave").then(TFCommands.locateCaveTypeBranch())));
        root.then(Commands.literal("export").then(Commands.literal("structures").executes(TFCommands::export)));
        root.then(Commands.literal("regen").then(Commands.argument("radius", IntegerArgumentType.integer(1)).executes(TFCommands::regen)));
        root.then(Commands.literal("preview").executes(TFCommands::openPreview));
        return root;
    }

    private static int openPreview(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = (CommandSourceStack)context.getSource();
        if (FMLLoader.getDist() != Dist.CLIENT) {
            source.sendFailure((Component)new TextComponent("Continent preview opens on the client (single-player or integrated server)."));
            return 0;
        }
        source.sendSuccess((Component)new TextComponent("Opening TerraForged continent preview \u0432\u0402\u201d R: reload, S: new seed, drag: pan, wheel: zoom."), false);
        SwingUtilities.invokeLater(() -> ContinentPreview.main(new String[0]));
        return 1;
    }

    private static int regen(CommandContext<CommandSourceStack> context) {
        try {
            int radius = IntegerArgumentType.getInteger(context, (String)"radius");
            Vec3 pos = ((CommandSourceStack)context.getSource()).getPosition();
            ChunkPos chunk = new ChunkPos((int)pos.x >> 4, (int)pos.z >> 4);
            Regenerator.regenerateChunks(chunk, radius, ((CommandSourceStack)context.getSource()).getLevel(), (CommandSourceStack)context.getSource());
            return 1;
        }
        catch (Throwable t) {
            t.printStackTrace();
            return 1;
        }
    }

    private static int export(CommandContext<CommandSourceStack> context) {
        RegistryAccess access = ((CommandSourceStack)context.getSource()).registryAccess();
        MutableComponent result = new TextComponent("Exported structure settings").withStyle(s -> s.withColor(ChatFormatting.GREEN));
        ((CommandSourceStack)context.getSource()).sendSuccess((Component)result, false);
        return 1;
    }

    private static int locate(CommandContext<CommandSourceStack> context, boolean withRadius) throws CommandSyntaxException {
        Component result;
        Generator generator = GeneratorPreset.getGenerator(((CommandSourceStack)context.getSource()).getLevel());
        if (generator == null) {
            return 1;
        }
        String name = StringArgumentType.getString(context, (String)"terrain");
        Terrain terrain = TerrainType.get(name);
        int radius = withRadius ? IntegerArgumentType.getInteger(context, (String)"radius") : 1;
        ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayerOrException();
        BlockPos at = player.blockPosition();
        int seed = Seeds.get(player.getLevel().getSeed());
        if (terrain == null) {
            result = TFCommands.text("Invalid terrain: " + name).withStyle(ChatFormatting.RED);
        } else {
            int maxRadius = Math.min(100, radius + 50);
            long pos = generator.getNoiseGenerator().find(seed, at.getX(), at.getZ(), radius, maxRadius, terrain);
            if (pos == 0L) {
                result = TFCommands.text("Unable to locate terrain: " + name).withStyle(ChatFormatting.RED);
            } else {
                int x = PosUtil.unpackLeft(pos);
                int z = PosUtil.unpackRight(pos);
                int y = generator.getBaseHeight(x, z, Heightmap.Types.MOTION_BLOCKING, (LevelHeightAccessor)player.level);
                result = TFCommands.createTerrainTeleportMessage(at, x, y, z, terrain);
            }
        }
        player.sendMessage((Component)result, player.getUUID());
        return 1;
    }

    private static Component createTerrainTeleportMessage(BlockPos pos, int x, int y, int z, Terrain terrain) {
        double distance = Math.sqrt(pos.distToCenterSqr((double)x, (double)y, (double)z));
        String commandText = String.format("/tp %s %s %s", x, y, z);
        String distanceText = String.format("%.1f", distance);
        String positionText = String.format("%s;%s;%s", x, y, z);
        return TFCommands.text("Found terrain: ").withStyle(ChatFormatting.GREEN).append((Component)TFCommands.text(terrain.getName()).withStyle(ChatFormatting.YELLOW)).append((Component)TFCommands.text(" Distance: ").withStyle(ChatFormatting.GREEN)).append((Component)TFCommands.text(distanceText).withStyle(ChatFormatting.YELLOW)).append((Component)TFCommands.text(". ").withStyle(ChatFormatting.GREEN)).append((Component)TFCommands.text("Teleport").withStyle(new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.UNDERLINE}).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, commandText)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TFCommands.text("Location: ").withStyle(ChatFormatting.GREEN).append((Component)TFCommands.text(positionText).withStyle(ChatFormatting.YELLOW))))));
    }

    private static int locateCave(CommandContext<CommandSourceStack> context, CaveSubtype subtype, CaveLocator.LocateMode mode, int radius) throws CommandSyntaxException {
        Generator generator = GeneratorPreset.getGenerator(((CommandSourceStack)context.getSource()).getLevel());
        if (generator == null) {
            ((CommandSourceStack)context.getSource()).sendFailure((Component)TFCommands.text("Not a NewTerraForged world").withStyle(ChatFormatting.RED));
            return 0;
        }
        String typeName = StringArgumentType.getString(context, (String)"type").toLowerCase();
        ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayerOrException();
        BlockPos at = player.blockPosition();
        UUID playerId = player.getUUID();
        MinecraftServer server = player.getServer();
        final boolean grotto = "grotto".equals(typeName);
        CaveType type = null;
        if (!grotto) {
            try {
                type = CaveType.forName(typeName.toUpperCase());
            }
            catch (IllegalArgumentException e) {
                ((CommandSourceStack)context.getSource()).sendFailure((Component)TFCommands.text("Unknown cave type: " + typeName + " (use giga, mega, or grotto)").withStyle(ChatFormatting.RED));
                return 0;
            }
            if (type != CaveType.GIGA && type != CaveType.MEGA) {
                ((CommandSourceStack)context.getSource()).sendFailure((Component)TFCommands.text("Only giga, mega, and grotto are searchable").withStyle(ChatFormatting.RED));
                return 0;
            }
        }
        int searchRadius = radius;
        if (searchRadius < 0) {
            searchRadius = grotto ? CaveLocator.DEFAULT_RADIUS_GROTTO : CaveLocator.defaultRadius(type);
        }
        final int finalRadius = searchRadius;
        final CaveType finalType = type;
        String modeText = mode == CaveLocator.LocateMode.ENTRANCE ? " entrance" : "";
        String subtypeText = subtype != CaveSubtype.ANY ? " " + subtype.getName() : "";
        String targetLabel = grotto ? "grotto" : finalType.getName() + modeText + subtypeText;
        player.sendMessage((Component)TFCommands.text("Searching nearest " + targetLabel + " within " + finalRadius + " blocks (background)...").withStyle(ChatFormatting.GRAY), playerId);
        CompletableFuture.supplyAsync(() -> {
            if (grotto) {
                return CaveLocator.findGrotto(generator, at.getX(), at.getZ(), finalRadius);
            }
            return CaveLocator.find(generator, finalType, subtype, mode, at.getX(), at.getZ(), finalRadius);
        }, Util.backgroundExecutor()).thenAccept(result -> server.execute(() -> {
            ServerPlayer online = server.getPlayerList().getPlayer(playerId);
            if (online == null) {
                return;
            }
            MutableComponent message;
            if (result == null) {
                String failMode = mode == CaveLocator.LocateMode.ENTRANCE ? " entrance" : "";
                String failSubtype = subtype != CaveSubtype.ANY ? " " + subtype.getName() : "";
                if (grotto) {
                    message = TFCommands.text("No river grotto entrance within " + finalRadius + " blocks. Try: /locatecave grotto " + Math.min(finalRadius * 2, CaveLocator.MAX_SEARCH_RADIUS)).withStyle(ChatFormatting.RED);
                } else {
                    message = TFCommands.text("No" + failSubtype + " " + finalType.getName() + failMode + " within " + finalRadius + " blocks. Try: /locatecave " + typeName + (mode == CaveLocator.LocateMode.ENTRANCE ? " entrance " : " ") + Math.min(finalRadius * 2, CaveLocator.MAX_SEARCH_RADIUS)).withStyle(ChatFormatting.RED);
                }
            } else {
                message = TFCommands.createCaveLocateMessage(at, result, finalRadius);
            }
            online.sendMessage((Component)message, playerId);
        }));
        return 1;
    }

    private static MutableComponent createCaveLocateMessage(BlockPos origin, CaveLocator.Result result, int searchRadius) {
        String cmd = String.format("/tp %s %s %s", result.x(), result.y(), result.z());
        String coords = String.format("X: %d  Y: %d  Z: %d", result.x(), result.y(), result.z());
        double distance = Math.sqrt(origin.distToCenterSqr((double)result.x(), (double)result.y(), (double)result.z()));
        String dist = String.format("%.0f", distance);
        String typeLabel = result.type() == CaveType.GLOBAL ? "grotto" : result.type().getName();
        String modeLabel = result.mode() == CaveLocator.LocateMode.ENTRANCE ? ", entrance" : ", system center";
        String kindLabel = result.entranceKind() != CaveLocator.EntranceKind.SYSTEM ? ", " + result.entranceKind().label() : "";
        String subtypeLabel = result.subtype() != CaveSubtype.ANY ? ", subtype " + result.subtype().getName() : "";
        String strengthLabel = result.mode() == CaveLocator.LocateMode.SYSTEM
                ? ", strength " + String.format("%.2f", result.strength())
                : "";
        return TFCommands.text("Found " + typeLabel + modeLabel + kindLabel + subtypeLabel + strengthLabel + ". ").withStyle(ChatFormatting.GREEN)
                .append((Component)TFCommands.text(coords).withStyle(ChatFormatting.YELLOW))
                .append((Component)TFCommands.text(" (").withStyle(ChatFormatting.GREEN))
                .append((Component)TFCommands.text(dist + "m away").withStyle(ChatFormatting.AQUA))
                .append((Component)TFCommands.text(", searched " + searchRadius + "m). ").withStyle(ChatFormatting.GREEN))
                .append((Component)TFCommands.text("Teleport").withStyle(new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.UNDERLINE}).withStyle(s -> s
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TFCommands.text(coords + " — click to teleport")))));
    }

    private static MutableComponent text(String message) {
        return new TextComponent(message);
    }
}
