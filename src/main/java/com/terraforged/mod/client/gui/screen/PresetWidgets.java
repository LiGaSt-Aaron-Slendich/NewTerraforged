package com.terraforged.mod.client.gui.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.settings.GeneratorSettings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class PresetWidgets {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_PREFIX = "#ASM de LiGaSt ,NewTF-presset ";
    private static final String DEFAULT_FILE = "newterraforged-settings.json";

    private PresetWidgets() {
    }

    public static void save(SettingsDraft draft) {
        Path dir = getPresetDir();
        String path = TinyFileDialogs.tinyfd_saveFileDialog(
                "Save NewTF settings",
                dir.resolve(DEFAULT_FILE).toString(),
                null,
                "JSON Files"
        );
        if (path == null || path.isBlank()) {
            return;
        }

        try {
            Files.createDirectories(dir);
            GeneratorSettings settings = draft.toGeneratorSettings();
            DataResult<JsonElement> encoded = GeneratorSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings);
            JsonElement json = encoded.result().orElseThrow(() -> new IllegalStateException(encoded.error().map(e -> e.message()).orElse("Unknown encode error")));
            Path file = Path.of(path);
            Files.createDirectories(file.getParent() == null ? dir : file.getParent());
            String contents = FILE_PREFIX + generateMarkerValue() + System.lineSeparator() + GSON.toJson(json) + System.lineSeparator();
            Files.writeString(file, contents);
            toast("NewTF settings saved", file.toString());
        } catch (Throwable t) {
            TerraForged.LOG.error("Failed to save NewTF settings", t);
            toast("NewTF settings save failed", t.getMessage());
        }
    }

    public static boolean load(SettingsDraft draft) {
        Path dir = getPresetDir();
        String path = TinyFileDialogs.tinyfd_openFileDialog(
                "Import NewTF settings",
                dir.toString(),
                null,
                "JSON Files",
                false
        );
        if (path == null || path.isBlank()) {
            return false;
        }

        try {
            String jsonBody = readAndValidatePreset(Path.of(path));
            JsonElement json = JsonParser.parseReader(new StringReader(jsonBody));
            DataResult<GeneratorSettings> parsed = GeneratorSettings.CODEC.parse(JsonOps.INSTANCE, json);
            GeneratorSettings settings = parsed.result().orElseThrow(() -> new IllegalStateException(parsed.error().map(e -> e.message()).orElse("Unknown parse error")));
            draft.loadGeneratorSettings(settings);
            toast("NewTF settings imported", Path.of(path).toString());
            return true;
        } catch (Throwable t) {
            TerraForged.LOG.error("Failed to import NewTF settings", t);
            toast("NewTF import failed", t.getMessage());
            return false;
        }
    }

    private static Path getPresetDir() {
        return FMLPaths.GAMEDIR.get().resolve("config").resolve("newterraforged").resolve("presets");
    }

    private static String readAndValidatePreset(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String firstLine = reader.readLine();
            if (!isValidHeader(firstLine)) {
                throw new IllegalArgumentException("This is not a NewTF world preset");
            }
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line).append(System.lineSeparator());
            }
            if (body.length() == 0) {
                throw new IllegalArgumentException("Preset file is empty");
            }
            return body.toString();
        }
    }

    private static boolean isValidHeader(String line) {
        if (line == null || !line.startsWith(FILE_PREFIX)) {
            return false;
        }
        try {
            long value = Long.parseLong(line.substring(FILE_PREFIX.length()).trim());
            return isFormulaValue(value);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ((x/21 + x/7 + x*12) / 10) = 128*x/105, so integer results are multiples of 128 for x=105*k.
    private static long generateMarkerValue() {
        long k = ThreadLocalRandom.current().nextLong(1, 1_000_000);
        return 128L * k;
    }

    private static boolean isFormulaValue(long value) {
        return value > 0L && value % 128L == 0L;
    }

    private static void toast(String title, String detail) {
        Minecraft mc = Minecraft.getInstance();
        mc.getToasts().addToast(SystemToast.multiline(mc, SystemToast.SystemToastIds.WORLD_GEN_SETTINGS_TRANSFER, new TextComponent(title), new TextComponent(detail == null ? "" : detail)));
    }
}
