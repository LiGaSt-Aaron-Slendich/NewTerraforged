package com.terraforged.mod.worldgen.datapack;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.util.FileUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.DataPackConfig;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

public class DataPackExporter {
   public static final String PACK = "file/TerraForged.zip";
   public static final Path CONFIG_DIR = Paths.get("config", "terraforged").toAbsolutePath();
   public static final Path DEFAULT_PACK_DIR = CONFIG_DIR.resolve("pack-v0.1");

   public static void extractDefaultPack() {
      try {
         TerraForged.LOG.info("Extracting default datapack to {}", DEFAULT_PACK_DIR);
         Path path = TerraForged.getPlatform().getContainer();
         FileUtil.createDirCopy(path, "default", DEFAULT_PACK_DIR);
      } catch (IOException ioexception) {
         ioexception.printStackTrace();
      }
   }

   public static Pair<Path, String> getDefaultsPath() {
      if (!Files.exists(DEFAULT_PACK_DIR)) {
         extractDefaultPack();
         if (!Files.exists(DEFAULT_PACK_DIR)) {
            TerraForged.LOG.warn("Failed to extract default datapack to {}", DEFAULT_PACK_DIR);
            return Pair.of(TerraForged.getPlatform().getContainer(), "default");
         }
      }

      return Pair.of(DEFAULT_PACK_DIR, ".");
   }

   public static DataPackConfig setup(@Nullable Path dir, DataPackConfig config) {
      if (dir == null) {
         throw new NullPointerException("Dir is null!");
      } else {
         TerraForged.LOG.info("Generating TerraForged datapack");

         try {
            Pair<Path, String> pair = getDefaultsPath();
            Path path = dir.resolve("TerraForged.zip");
            FileUtil.createZipCopy((Path)pair.getLeft(), (String)pair.getRight(), path);
         } catch (IOException ioexception) {
            ioexception.printStackTrace();
         }

         List<String> list = new ArrayList<>(config.getEnabled());
         list.add("file/TerraForged.zip");
         List<String> list1 = new ArrayList<>(config.getDisabled());
         list1.remove("file/TerraForged.zip");
         return new DataPackConfig(list, list1);
      }
   }
}
