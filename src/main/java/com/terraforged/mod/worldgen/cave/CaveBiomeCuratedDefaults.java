package com.terraforged.mod.worldgen.cave;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.terraforged.mod.TerraForged;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds config {@code Cave_configs/Biomes/} from classpath hand-curated JSON.
 * Autogen / {@link CaveCondNameDefaults} remain emergency fill only.
 */
public final class CaveBiomeCuratedDefaults {
    public static final int BUNDLED_REVISION = 1;
    private static final String CLASS_ROOT = "/defaultconfigs/NewTerraForged/Cave_configs/Biomes/";
    private static final String INDEX = CLASS_ROOT + "_CURATED_INDEX.txt";

    private CaveBiomeCuratedDefaults() {
    }

    /**
     * Copy / refresh curated cave rules into the live config tree.
     * Overwrites when missing, older revision, or {@code auto_generated=true}.
     * Never overwrites a player-saved rule that already has the current revision
     * and {@code auto_generated=false}.
     */
    public static int seedInto(Path configBiomesRoot) {
        List<String> rels = readIndex();
        if (rels.isEmpty()) {
            TerraForged.LOG.warn("[CaveBiomeRules] no curated index at {}", INDEX);
            return 0;
        }
        int written = 0;
        for (String rel : rels) {
            String resource = CLASS_ROOT + rel;
            try (InputStream in = CaveBiomeCuratedDefaults.class.getResourceAsStream(resource)) {
                if (in == null) {
                    TerraForged.LOG.warn("[CaveBiomeRules] missing curated resource {}", resource);
                    continue;
                }
                String raw = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
                if (raw.isEmpty()) {
                    continue;
                }
                Path dest = configBiomesRoot.resolve(rel.replace('/', java.io.File.separatorChar));
                if (!shouldWrite(dest, raw)) {
                    continue;
                }
                Files.createDirectories(dest.getParent());
                String header = "# ASM: cave_biome_rule v1 — curated defaults\n";
                Files.writeString(dest, header + raw + (raw.endsWith("\n") ? "" : "\n"), StandardCharsets.UTF_8);
                written++;
            } catch (Exception e) {
                TerraForged.LOG.warn("[CaveBiomeRules] curated seed failed {}: {}", rel, e.toString());
            }
        }
        if (written > 0) {
            TerraForged.LOG.info("[CaveBiomeRules] seeded {} curated cave rules (rev {})", written, BUNDLED_REVISION);
        }
        return written;
    }

    private static boolean shouldWrite(Path dest, String curatedRaw) {
        if (!Files.isRegularFile(dest)) {
            return true;
        }
        try {
            String existing = Files.readString(dest, StandardCharsets.UTF_8);
            String body = existing;
            if (body.startsWith("#")) {
                String[] parts = body.split("\\R", 2);
                body = parts.length >= 2 ? parts[1].trim() : "";
            }
            if (body.isEmpty() || !body.startsWith("{")) {
                return true;
            }
            JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
            boolean auto = obj.has("auto_generated") && obj.get("auto_generated").getAsBoolean();
            int rev = obj.has("curated_revision") ? obj.get("curated_revision").getAsInt() : 0;
            if (auto) {
                return true;
            }
            if (rev < BUNDLED_REVISION) {
                // Player may have edited an old migrate dump without revision — refresh once.
                // Skip if they already set explicit cond_* AND climates (treated as edited).
                boolean hasCond = obj.has("cond_temp") || obj.has("cond_humidity") || obj.has("cond_fertility");
                boolean hasClimates = obj.has("climates") && obj.get("climates").isJsonArray()
                        && obj.getAsJsonArray("climates").size() > 0;
                if (!hasCond || !hasClimates) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private static List<String> readIndex() {
        ArrayList<String> out = new ArrayList<>();
        try (InputStream in = CaveBiomeCuratedDefaults.class.getResourceAsStream(INDEX)) {
            if (in == null) {
                return out;
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                char[] buf = new char[4096];
                int n;
                while ((n = reader.read(buf)) >= 0) {
                    sb.append(buf, 0, n);
                }
                for (String line : sb.toString().split("\\R")) {
                    String t = line.trim();
                    if (t.isEmpty() || t.startsWith("#")) {
                        continue;
                    }
                    out.add(t.replace('\\', '/'));
                }
            }
        } catch (Exception e) {
            TerraForged.LOG.warn("[CaveBiomeRules] failed reading curated index: {}", e.toString());
        }
        return out;
    }
}
