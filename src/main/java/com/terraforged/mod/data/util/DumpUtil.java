package com.terraforged.mod.data.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.io.PrintStream;

public class DumpUtil {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static <T> void dump(T t, Codec<T> codec, PrintStream out) {
        codec.encodeStart((DynamicOps)JsonOps.INSTANCE, t).result().ifPresent(json -> {
            out.println();
            GSON.toJson(json, (Appendable)out);
            out.println();
        });
    }
}
