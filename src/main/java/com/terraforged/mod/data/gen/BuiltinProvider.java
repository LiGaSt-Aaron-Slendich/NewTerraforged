package com.terraforged.mod.data.gen;

import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;

public record BuiltinProvider(Path dir) implements DataProvider {
   public String getName() {
      return "TerraForged Builtins";
   }

   public void run(HashCache cache) throws IOException {
      DataGen.export(this.dir);
   }
}
