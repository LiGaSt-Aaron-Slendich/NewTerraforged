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
        return (LiteralArgumentBuilder)TFCommands.root("locatecave").then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument((String)"type", (ArgumentType)StringArgumentType.string()).suggests((ctx, builder) -> {
            builder.suggest("giga");
            builder.suggest("mega");
            return builder.buildFuture();
        }).executes(c -> TFCommands.locateCave((CommandContext<CommandSourceStack>)c, CaveSubtype.ANY, -1))).then(Commands.argument((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)256, (int)100000)).executes(c -> TFCommands.locateCave((CommandContext<CommandSourceStack>)c, CaveSubtype.ANY, IntegerArgumentType.getInteger((CommandContext)c, (String)"radius"))))).then(((RequiredArgumentBuilder)Commands.argument((String)"subtype", (ArgumentType)StringArgumentType.string()).suggests((ctx, builder) -> {
            builder.suggest("coastal");
            builder.suggest("tunnel");
            builder.suggest("ogpm");
            return builder.buildFuture();
        }).executes(c -> {
            CaveSubtype st = TFCommands.parseSubtype((CommandContext<CommandSourceStack>)c);
            return st == null ? 0 : TFCommands.locateCave((CommandContext<CommandSourceStack>)c, st, -1);
        })).then(Commands.argument((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)256, (int)100000)).executes(c -> {
            CaveSubtype st = TFCommands.parseSubtype((CommandContext<CommandSourceStack>)c);
            return st == null ? 0 : TFCommands.locateCave((CommandContext<CommandSourceStack>)c, st, IntegerArgumentType.getInteger((CommandContext)c, (String)"radius"));
        }))));
    }

    private static CaveSubtype parseSubtype(CommandContext<CommandSourceStack> context) {
        String raw = StringArgumentType.getString(context, (String)"subtype");
        try {
            return CaveSubtype.forName(raw);
        }
        catch (IllegalArgumentException e) {
            ((CommandSourceStack)context.getSource()).sendFailure((Component)TFCommands.text("Unknown cave subtype: " + raw + " (use coastal, tunnel, ogpm)").withStyle(ChatFormatting.RED));
            return null;
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> getTFCommand() {
        return (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)TFCommands.root("tf").then(Commands.literal((String)"locate").then(Commands.literal((String)"cave").then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument((String)"type", (ArgumentType)StringArgumentType.string()).suggests((ctx, builder) -> {
            builder.suggest("giga");
            builder.suggest("mega");
            return builder.buildFuture();
        }).executes(c -> TFCommands.locateCave((CommandContext<CommandSourceStack>)c, CaveSubtype.ANY, -1))).then(Commands.argument((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)256, (int)100000)).executes(c -> TFCommands.locateCave((CommandContext<CommandSourceStack>)c, CaveSubtype.ANY, IntegerArgumentType.getInteger((CommandContext)c, (String)"radius"))))).then(((RequiredArgumentBuilder)Commands.argument((String)"subtype", (ArgumentType)StringArgumentType.string()).suggests((ctx, builder) -> {
            builder.suggest("coastal");
            builder.suggest("tunnel");
            builder.suggest("ogpm");
            return builder.buildFuture();
        }).executes(c -> {
            CaveSubtype st = TFCommands.parseSubtype((CommandContext<CommandSourceStack>)c);
            return st == null ? 0 : TFCommands.locateCave((CommandContext<CommandSourceStack>)c, st, -1);
        })).then(Commands.argument((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)256, (int)100000)).executes(c -> {
            CaveSubtype st = TFCommands.parseSubtype((CommandContext<CommandSourceStack>)c);
            return st == null ? 0 : TFCommands.locateCave((CommandContext<CommandSourceStack>)c, st, IntegerArgumentType.getInteger((CommandContext)c, (String)"radius"));
        }))))))).then(Commands.literal((String)"export").then(Commands.literal((String)"structures").executes(TFCommands::export)))).then(Commands.literal((String)"regen").then(Commands.argument((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)1)).executes(TFCommands::regen)))).then(Commands.literal((String)"preview").executes(TFCommands::openPreview));
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

    private static int locateCave(CommandContext<CommandSourceStack> context, CaveSubtype subtype, int radius) throws CommandSyntaxException {
        MutableComponent message;
        ServerPlayer player;
        BlockPos at;
        CaveLocator.Result result;
        CaveType type;
        Generator generator = GeneratorPreset.getGenerator(((CommandSourceStack)context.getSource()).getLevel());
        if (generator == null) {
            ((CommandSourceStack)context.getSource()).sendFailure((Component)TFCommands.text("Not a NewTerraForged world").withStyle(ChatFormatting.RED));
            return 0;
        }
        String typeName = StringArgumentType.getString(context, (String)"type").toUpperCase();
        try {
            type = CaveType.forName(typeName);
        }
        catch (IllegalArgumentException e) {
            ((CommandSourceStack)context.getSource()).sendFailure((Component)TFCommands.text("Unknown cave type: " + typeName + " (use giga or mega)").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (type != CaveType.GIGA && type != CaveType.MEGA) {
            ((CommandSourceStack)context.getSource()).sendFailure((Component)TFCommands.text("Only giga and mega are searchable").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (radius < 0) {
            radius = CaveLocator.defaultRadius(type);
        }
        if ((result = CaveLocator.find(generator, type, subtype, (at = (player = ((CommandSourceStack)context.getSource()).getPlayerOrException()).blockPosition()).getX(), at.getZ(), radius)) == null) {
            String subtypeText = subtype != CaveSubtype.ANY ? " " + subtype.getName() : "";
            message = TFCommands.text("No" + subtypeText + " " + type.getName() + " cave within " + radius + " blocks. Try larger radius.").withStyle(ChatFormatting.RED);
        } else {
            String cmd = String.format("/tp %s %s %s", result.x(), result.y(), result.z());
            String dist = String.format("%.0f", Math.sqrt(at.distToCenterSqr((double)result.x(), (double)result.y(), (double)result.z())));
            String subtypeLabel = result.subtype() != CaveSubtype.ANY ? ", subtype " + result.subtype().getName() : "";
            message = TFCommands.text("Found " + type.getName() + " cave" + subtypeLabel + " (~" + dist + "m, strength ").withStyle(ChatFormatting.GREEN).append((Component)TFCommands.text(String.format("%.2f", Float.valueOf(result.strength()))).withStyle(ChatFormatting.YELLOW)).append((Component)TFCommands.text("). ").withStyle(ChatFormatting.GREEN)).append((Component)TFCommands.text("Teleport").withStyle(new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.UNDERLINE}).withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TFCommands.text("Go to " + result.x() + " " + result.y() + " " + result.z())))));
        }
        player.sendMessage((Component)message, player.getUUID());
        return 1;
    }

    private static MutableComponent text(String message) {
        return new TextComponent(message);
    }
}
