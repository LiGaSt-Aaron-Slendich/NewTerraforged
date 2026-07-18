package com.terraforged.mod.worldgen.cave;

import com.mojang.serialization.Codec;
import java.util.Locale;

public enum CaveType {
   GLOBAL("global"),
   UNIQUE("unique");

   public static final Codec<CaveType> CODEC = Codec.STRING.xmap(CaveType::forName, CaveType::getName);
   final String name;

   private CaveType(String name) {
      this.name = name;
   }

   public String getName() {
      return this.name;
   }

   public static CaveType forName(String name) {
      name = name.toUpperCase(Locale.ROOT);
      return valueOf(name);
   }
}
