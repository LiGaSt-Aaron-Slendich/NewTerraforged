package com.terraforged.mod.data.gen;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.data.gen.DataGen;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;

public record TerraForgedDataProvider(Path dir) implements DataProvider
{
    public void run(HashCache cachedOutput) {
        DataGen.export(this.dir, cachedOutput).join();
        new Timer().schedule(new TimerTask(){

            @Override
            public void run() {
                TerraForged.LOG.warn("Forcibly shutting down datagen process");
                System.exit(0);
            }
        }, 1000L);
    }

    public String getName() {
        return "NewTerraForged Builtins";
    }
}
