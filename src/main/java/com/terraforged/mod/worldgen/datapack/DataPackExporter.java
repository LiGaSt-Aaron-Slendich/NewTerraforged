package com.terraforged.mod.worldgen.datapack;

import com.terraforged.mod.CommonAPI;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.util.FileUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.lang3.tuple.Pair;

public class DataPackExporter {
    public static final String PACK_NAME = "NewTerraforged-v0.2";
    public static final String PACK_FILE_NAME = "NewTerraforged-v0.2.zip";
    public static final Path CONFIG_DIR = Paths.get("config", "NewTerraForged").toAbsolutePath();
    public static final Path DEFAULT_PACK_DIR = CONFIG_DIR.resolve("pack-v0.2");

    public static void extractDefaultPack() {
        try {
            TerraForged.LOG.info("Extracting default datapack to {}", DEFAULT_PACK_DIR);
            Path root = CommonAPI.get().getContainer();
            FileUtil.createDirCopy(root, "default", DEFAULT_PACK_DIR);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Pair<Path, String> getDefaultsPath() {
        if (!Files.exists(DEFAULT_PACK_DIR, new LinkOption[0])) {
            DataPackExporter.extractDefaultPack();
            if (!Files.exists(DEFAULT_PACK_DIR, new LinkOption[0])) {
                TerraForged.LOG.warn("Failed to extract default datapack to {}", DEFAULT_PACK_DIR);
                return Pair.of(CommonAPI.get().getContainer(), "default");
            }
        }
        return Pair.of(DEFAULT_PACK_DIR, ".");
    }

    public static void createWorldDatapack(Path dir) {
        TerraForged.LOG.info("Copying world-instance datapack to {}", dir);
        try {
            Pair<Path, String> src = DataPackExporter.getDefaultsPath();
            Path dest = dir.resolve(PACK_FILE_NAME);
            FileUtil.createZipCopy(src.getLeft(), src.getRight(), dest);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
