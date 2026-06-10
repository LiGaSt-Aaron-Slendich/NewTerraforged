package com.terraforged.mod.worldgen.util;

import java.util.concurrent.Executor;
import net.minecraft.Util;

public class ThreadPool {
    public static final Executor EXECUTOR = Util.backgroundExecutor();
}
