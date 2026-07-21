package com.terraforged.mod.client.gui.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.settings.GeneratorSettings;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.TextComponent;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class PresetWidgets {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PresetWidgets() {
    }

    public static void save(SettingsDraft draft) {
        String path = TinyFileDialogs.tinyfd_saveFileDialog(
                "Save NewTF settings",
                "newterraforged-settings.json",
                null,
                "JSON Files"
        );
        if (path == null || path.isBlank()) {
            return;
        }

        try {
            GeneratorSettings settings = draft.toGeneratorSettings();
            DataResult<JsonElement> encoded = GeneratorSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings);
            JsonElement json = encoded.result().orElseThrow(() -> new IllegalStateException(encoded.error().map(e -> e.message()).orElse("Unknown encode error")));
            Path file = Path.of(path);
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(json, writer);
            }
            toast("NewTF settings saved", file.getFileName().toString());
        } catch (Throwable t) {
            TerraForged.LOG.error("Failed to save NewTF settings", t);
            toast("NewTF settings save failed", t.getMessage());
        }
    }

    public static boolean load(SettingsDraft draft) {
        String path = TinyFileDialogs.tinyfd_openFileDialog(
                "Import NewTF settings",
                null,
                null,
                "JSON Files",
                false
        );
        if (path == null || path.isBlank()) {
            return false;
        }

        try (Reader reader = Files.newBufferedReader(Path.of(path))) {
            JsonElement json = JsonParser.parseReader(reader);
            DataResult<GeneratorSettings> parsed = GeneratorSettings.CODEC.parse(JsonOps.INSTANCE, json);
            GeneratorSettings settings = parsed.result().orElseThrow(() -> new IllegalStateException(parsed.error().map(e -> e.message()).orElse("Unknown parse error")));
            draft.loadGeneratorSettings(settings);
            toast("NewTF settings imported", Path.of(path).getFileName().toString());
            return true;
        } catch (Throwable t) {
            TerraForged.LOG.error("Failed to import NewTF settings", t);
            toast("NewTF import failed", t.getMessage());
            return false;
        }
    }

    private static void toast(String title, String detail) {
        Minecraft mc = Minecraft.getInstance();
        mc.getToasts().addToast(SystemToast.multiline(mc, SystemToast.SystemToastIds.WORLD_GEN_SETTINGS_TRANSFER, new TextComponent(title), new TextComponent(detail == null ? "" : detail)));
    }
}
