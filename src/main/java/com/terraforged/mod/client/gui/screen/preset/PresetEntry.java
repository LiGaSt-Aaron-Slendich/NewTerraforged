package com.terraforged.mod.client.gui.screen.preset;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.terraforged.mod.worldgen.settings.GeneratorSettings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;

public final class PresetEntry {
    private final String id;
    private final String displayName;
    private final boolean bundled;
    @Nullable
    private final Path file;
    @Nullable
    private final String resourcePath;
    private final PresetFormat.Header header;
    @Nullable
    private GeneratorSettings cachedSettings;

    private PresetEntry(String id, String displayName, boolean bundled, @Nullable Path file, @Nullable String resourcePath, PresetFormat.Header header) {
        this.id = id;
        this.displayName = displayName;
        this.bundled = bundled;
        this.file = file;
        this.resourcePath = resourcePath;
        this.header = header;
    }

    public static PresetEntry fromResource(String resourcePath, String displayName) throws IOException {
        try (InputStream in = PresetEntry.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Missing preset resource: " + resourcePath);
            }
            Parsed parsed = parseBody(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
            String id = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
            return new PresetEntry(id, displayName, true, null, resourcePath, parsed.header);
        }
    }

    public static PresetEntry fromFile(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            Parsed parsed = parseBody(reader);
            String name = file.getFileName().toString();
            int dot = name.lastIndexOf('.');
            String displayName = dot > 0 ? name.substring(0, dot) : name;
            return new PresetEntry(file.toString(), displayName, false, file, null, parsed.header);
        }
    }

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    public boolean bundled() {
        return this.bundled;
    }

    public String displayAuthor() {
        return this.header.displayAuthor();
    }

    public GeneratorSettings loadSettings() throws IOException {
        if (this.cachedSettings != null) {
            return this.cachedSettings;
        }
        Parsed parsed;
        if (this.file != null) {
            try (BufferedReader reader = Files.newBufferedReader(this.file)) {
                parsed = parseBody(reader);
            }
        } else if (this.resourcePath != null) {
            try (InputStream in = PresetEntry.class.getResourceAsStream(this.resourcePath)) {
                if (in == null) {
                    throw new IOException("Missing preset resource: " + this.resourcePath);
                }
                parsed = parseBody(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } else {
            throw new IOException("Preset has no source");
        }
        DataResult<GeneratorSettings> decoded = GeneratorSettings.CODEC.parse(JsonOps.INSTANCE, parsed.json);
        this.cachedSettings = decoded.result().orElseThrow(() -> new IOException(decoded.error().map(e -> e.message()).orElse("Invalid preset json")));
        return this.cachedSettings;
    }

    private static Parsed parseBody(BufferedReader reader) throws IOException {
        String line1 = reader.readLine();
        PresetFormat.Header partial = PresetFormat.parseHeaderLine1(line1);
        String line2 = reader.readLine();
        PresetFormat.Header header = PresetFormat.finalizeHeader(partial, line2);
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line).append(System.lineSeparator());
        }
        if (body.length() == 0) {
            throw new IOException("Preset file is empty");
        }
        JsonElement json = JsonParser.parseReader(new StringReader(body.toString()));
        return new Parsed(header, json);
    }

    private record Parsed(PresetFormat.Header header, JsonElement json) {
    }
}
