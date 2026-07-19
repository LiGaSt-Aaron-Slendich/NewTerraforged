package com.terraforged.mod;

public interface Environment {
   boolean DEV_ENV = hasFlag("dev");
   /** Only enabled via explicit {@code -Dprofiling}; no longer implied by {@code -Ddev}. */
   boolean PROFILING = hasFlag("profiling");
   boolean UNLIMITED = DEV_ENV || hasFlag("unlimited");
   boolean DEBUGGING = DEV_ENV || hasFlag("debugging");
   boolean DATA_GEN = hasFlag("datagen");
   int CORES = Runtime.getRuntime().availableProcessors();

   static boolean hasFlag(String flag) {
      return System.getProperty(flag) != null;
   }

   static void log() {
      TerraForged.LOG.info("Environment:");
      TerraForged.LOG.info("- Dev:       {}", DEV_ENV);
      TerraForged.LOG.info("- Profiling: {}", PROFILING);
      TerraForged.LOG.info("- Unlimited: {}", UNLIMITED);
      TerraForged.LOG.info("- Debugging: {}", DEBUGGING);
      TerraForged.LOG.info("- Data Gen:  {}", DATA_GEN);
      TerraForged.LOG.info("- Cores:     {}", CORES);
   }
}
