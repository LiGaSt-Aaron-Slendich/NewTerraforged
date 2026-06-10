package com.terraforged.mod;

import com.terraforged.mod.TerraForged;

public interface Environment {
    public static final boolean DEV_ENV = Environment.hasFlag("dev");
    public static final boolean PROFILING = DEV_ENV || Environment.hasFlag("profiling");
    public static final boolean UNLIMITED = DEV_ENV || Environment.hasFlag("unlimited");
    public static final boolean DEBUGGING = DEV_ENV || Environment.hasFlag("debugging");
    public static final boolean DATA_GEN = Environment.hasFlag("datagen");
    public static final int CORES = Runtime.getRuntime().availableProcessors();

    public static boolean hasFlag(String flag) {
        return System.getProperty(flag) != null;
    }

    public static void log() {
        TerraForged.LOG.info("Environment:");
        TerraForged.LOG.info("- Dev:       {}", DEV_ENV);
        TerraForged.LOG.info("- Profiling: {}", PROFILING);
        TerraForged.LOG.info("- Unlimited: {}", UNLIMITED);
        TerraForged.LOG.info("- Debugging: {}", DEBUGGING);
        TerraForged.LOG.info("- Data Gen:  {}", DATA_GEN);
        TerraForged.LOG.info("- Cores:     {}", CORES);
    }
}
