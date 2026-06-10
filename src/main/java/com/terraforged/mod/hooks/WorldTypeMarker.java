package com.terraforged.mod.hooks;

import com.terraforged.mod.TerraForged;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import net.minecraft.nbt.CompoundTag;

public final class WorldTypeMarker {
    public static final String NBT_KEY = "newterraforged:NewTerraForgedWorld";
    private static final String MARKER_FILE = ".newterraforged";
    private static final String[] LEGACY_NBT_KEYS = new String[]{"newtf:TerraForgedWorld", "terraforged:TerraForgedWorld"};

    private WorldTypeMarker() {
    }

    public static String[] legacyNbtKeys() {
        return LEGACY_NBT_KEYS;
    }

    public static boolean isMarked(CompoundTag tag) {
        if (tag != null && tag.getBoolean(NBT_KEY)) {
            return true;
        }
        if (tag == null) {
            return false;
        }
        for (String legacy : LEGACY_NBT_KEYS) {
            if (!tag.getBoolean(legacy)) continue;
            return true;
        }
        return false;
    }

    public static boolean hasMarkerFile(Path worldRoot) {
        if (worldRoot == null) {
            return false;
        }
        return Files.isRegularFile(worldRoot.resolve(MARKER_FILE), new LinkOption[0]);
    }

    public static void writeMarkerFile(Path worldRoot) {
        if (worldRoot == null) {
            return;
        }
        try {
            Files.writeString(worldRoot.resolve(MARKER_FILE), (CharSequence)"true\n", StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (IOException e) {
            TerraForged.LOG.warn("Could not write NewTerraForged marker file in {}", worldRoot, e);
        }
    }

    public static boolean hasWorldDatapack(Path worldRoot) {
        if (worldRoot == null) {
            return false;
        }
        Path datapacks = worldRoot.resolve("datapacks");
        if (!Files.isDirectory(datapacks, new LinkOption[0])) {
            return false;
        }
        String prefix = "NewTerraforged-v0.2";
        try (Stream<Path> stream = Files.list(datapacks)) {
            return stream.anyMatch(p -> {
                String name = p.getFileName().toString().toLowerCase();
                return name.startsWith(prefix.toLowerCase()) || name.startsWith("NewTerraforged".toLowerCase()) || name.startsWith("terraforged") || name.startsWith("newterraforged");
            });
        } catch (IOException e) {
            return false;
        }
    }

    /*
     * Enabled aggressive exception aggregation
     */
    public static boolean hasGeneratorInLevelDat(Path worldRoot) {
        if (worldRoot == null) {
            return false;
        }
        Path levelDat = worldRoot.resolve("level.dat");
        if (!Files.isRegularFile(levelDat, new LinkOption[0])) {
            return false;
        }
        try (InputStream in = Files.newInputStream(levelDat, new OpenOption[0]);){
            boolean bl;
            try (GZIPInputStream gzip = new GZIPInputStream(in);){
                String raw = new String(gzip.readAllBytes(), StandardCharsets.ISO_8859_1);
                bl = raw.contains("newterraforged:generator") || raw.contains("terraforged:generator") || raw.contains(NBT_KEY);
            }
            return bl;
        }
        catch (IOException e) {
            return false;
        }
    }
}
