package com.terraforged.mod.client.gui.screen.preset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.settings.GeneratorSettings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraftforge.fml.loading.FMLPaths;

public final class PresetLibrary {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String RESOURCE_ROOT = "/assets/newterraforged/presets/";
    private static final String[][] BUNDLED = {
            {"classic.json", "Classic"},
            {"large_continents.json", "Large Continents"},
            {"archipelago.json", "Archipelago"},
            {"shipwrecked.json", "NTF (DS:Shipwrecked)"},
            {"old_world.json", "Old World"},
            {"sparse.json", "Sparse"}
    };

    private PresetLibrary() {
    }

    public static Path userDir() {
        return FMLPaths.GAMEDIR.get().resolve("config").resolve("newterraforged").resolve("presets");
    }

    public static List<PresetEntry> listDefaultPresets() {
        List<PresetEntry> entries = new ArrayList<>();
        for (String[] spec : BUNDLED) {
            try {
                entries.add(PresetEntry.fromResource(RESOURCE_ROOT + spec[0], spec[1]));
            } catch (IOException e) {
                TerraForged.LOG.warn("Failed to load bundled preset {}: {}", spec[0], e.getMessage());
            }
        }
        return entries;
    }

    public static List<PresetEntry> listUserPresets() {
        List<PresetEntry> entries = new ArrayList<>();
        Path dir = userDir();
        if (!Files.isDirectory(dir)) {
            return entries;
        }
        try (var stream = Files.list(dir)) {
            stream.filter(path -> {
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                return name.endsWith(".json") || name.endsWith(".ntpreset");
            }).sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT))).forEach(path -> {
                try {
                    entries.add(PresetEntry.fromFile(path));
                } catch (IOException e) {
                    TerraForged.LOG.warn("Skipping invalid user preset {}: {}", path, e.getMessage());
                }
            });
        } catch (IOException e) {
            TerraForged.LOG.warn("Failed to list user presets in {}", dir, e);
        }
        return entries;
    }

    public static Path saveUserPreset(String fileName, GeneratorSettings settings) throws IOException {
        String safeName = sanitizeFileName(fileName);
        if (safeName.isBlank()) {
            safeName = "preset";
        }
        if (!safeName.endsWith(".json")) {
            safeName = safeName + ".json";
        }
        Path dir = userDir();
        Files.createDirectories(dir);
        Path target = dir.resolve(safeName);
        DataResult<JsonElement> encoded = GeneratorSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings);
        JsonElement json = encoded.result().orElseThrow(() -> new IllegalStateException(encoded.error().map(e -> e.message()).orElse("encode failed")));
        String contents = PresetFormat.buildHeaderLines(false, PresetFormat.generateUserMarker()) + GSON.toJson(json) + System.lineSeparator();
        Files.writeString(target, contents);
        return target;
    }

    private static String sanitizeFileName(String name) {
        return name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
