package com.terraforged.mod.platform.forge;

import com.terraforged.mod.TerraForged;
import net.minecraftforge.fml.common.Mod;

@Mod(value="terraforged")
public final class TerraForgedCompatMod {
    public static final String MODID = "terraforged";

    public TerraForgedCompatMod() {
        TerraForged.LOG.info("Registered {} mod id alias for third-party compatibility", MODID);
    }
}
