package com.terraforged.mod.platform.forge;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.platform.forge.TFConfigWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Locale;
import net.minecraftforge.fml.loading.FMLPaths;

final class TFConfigLoader {
    private static final int MIN_VALID_BYTES = 16;

    private TFConfigLoader() {
    }

    static CommentedFileConfig open(String relativePath) {
        Path path = FMLPaths.CONFIGDIR.get().resolve(relativePath);
        TFConfigLoader.ensureDefaultIfMissingOrTooSmall(relativePath, path);
        CommentedFileConfig config = (CommentedFileConfig)CommentedFileConfig.builder((Path)path).sync().build();
        try {
            config.load();
            return config;
        }
        catch (Exception parseError) {
            return TFConfigLoader.recoverFromSyntaxError(relativePath, path, config, parseError);
        }
    }

    private static void ensureDefaultIfMissingOrTooSmall(String relativePath, Path target) {
        try {
            Path legacyTarget;
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent(), new FileAttribute[0]);
            }
            if (Files.exists(target, new LinkOption[0]) && Files.size(target) >= 16L) {
                return;
            }
            String legacyRelative = relativePath.replace("NewTerraForged/", "NewTerraforged/");
            if (!legacyRelative.equals(relativePath) && Files.exists(legacyTarget = FMLPaths.CONFIGDIR.get().resolve(legacyRelative), new LinkOption[0]) && Files.size(legacyTarget) >= 16L) {
                Files.copy(legacyTarget, target, StandardCopyOption.REPLACE_EXISTING);
                TerraForged.LOG.info("[TFConfig] Migrated {} -> {}", legacyRelative, relativePath);
                return;
            }
            TFConfigLoader.copyBundledDefault(relativePath, target, "missing or too small");
        }
        catch (Exception e) {
            TerraForged.LOG.warn("[TFConfig] Could not install default for {}", relativePath, e);
        }
    }

    private static CommentedFileConfig recoverFromSyntaxError(String relativePath, Path path, CommentedFileConfig failed, Exception parseError) {
        String summary = TFConfigLoader.summarize(parseError);
        TerraForged.LOG.error("[TFConfig] Syntax error in {} \u0432\u0402\u201d restoring bundled default: {}", relativePath, summary, parseError);
        TFConfigWarnings.recordFallback(relativePath, summary);
        try {
            failed.close();
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            TFConfigLoader.copyBundledDefault(relativePath, path, "syntax error recovery");
        }
        catch (Exception copyError) {
            TerraForged.LOG.error("[TFConfig] Could not restore bundled default for {}", relativePath, copyError);
            throw new IllegalStateException("Invalid config and no bundled default: " + relativePath, parseError);
        }
        CommentedFileConfig restored = (CommentedFileConfig)CommentedFileConfig.builder((Path)path).sync().build();
        try {
            restored.load();
            return restored;
        }
        catch (Exception again) {
            try {
                restored.close();
            }
            catch (Exception exception) {
                // empty catch block
            }
            throw new IllegalStateException("Bundled default for " + relativePath + " is invalid", again);
        }
    }

    private static String summarize(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            message = error.getClass().getSimpleName();
        }
        if (message.length() > 180) {
            return message.substring(0, 177) + "...";
        }
        return message;
    }

    private static void copyBundledDefault(String relativePath, Path target, String reason) throws IOException {
        String resourcePath = "/defaultconfigs/" + relativePath.replace('\\', '/');
        try (InputStream in = TFConfigLoader.class.getResourceAsStream(resourcePath);){
            if (in == null) {
                TerraForged.LOG.warn("[TFConfig] No bundled default for {} (expected {})", relativePath, resourcePath);
                return;
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            TerraForged.LOG.info("[TFConfig] Wrote default {} ({})", relativePath, reason);
        }
    }

    static Config section(Config root, String key) {
        Object value = TFConfigLoader.getIgnoreCase(root, key);
        if (value instanceof Config) {
            Config cfg = (Config)value;
            return cfg;
        }
        return Config.inMemory();
    }

    static Object getIgnoreCase(Config cfg, String key) {
        if (cfg.contains(key)) {
            return cfg.get(key);
        }
        String lower = key.toLowerCase(Locale.ROOT);
        for (String k : cfg.valueMap().keySet()) {
            if (!k.equalsIgnoreCase(lower)) continue;
            return cfg.get(k);
        }
        return null;
    }

    static float getFloat(Config cfg, String key, float def) {
        Object value = TFConfigLoader.getIgnoreCase(cfg, key);
        if (value instanceof Number) {
            Number n = (Number)value;
            return n.floatValue();
        }
        return def;
    }

    static int getInt(Config cfg, String key, int def) {
        Object value = TFConfigLoader.getIgnoreCase(cfg, key);
        if (value instanceof Number) {
            Number n = (Number)value;
            return n.intValue();
        }
        return def;
    }

    static double getDouble(Config cfg, String key, double def) {
        Object value = TFConfigLoader.getIgnoreCase(cfg, key);
        if (value instanceof Number) {
            Number n = (Number)value;
            return n.doubleValue();
        }
        return def;
    }

    static boolean getBool(Config cfg, String key, boolean def) {
        Object value = TFConfigLoader.getIgnoreCase(cfg, key);
        if (value instanceof Boolean) {
            Boolean b = (Boolean)value;
            return b;
        }
        return def;
    }

    static String getString(Config cfg, String key, String def) {
        Object value = TFConfigLoader.getIgnoreCase(cfg, key);
        if (value != null) {
            return String.valueOf(value);
        }
        return def;
    }
}
