package com.terraforged.mod.platform.forge;

import com.terraforged.mod.TerraForged;
import net.minecraftforge.fml.common.Mod;

/**
 * Legacy Forge mod id alias so third-party mods that depend on {@code terraforged}
 * (e.g. Dynamic Trees TerraForged) still resolve. Real product is {@code newterraforged}.
 */
@Mod("terraforged")
public final class TerraForgedCompatMod {
    public static final String MODID = "terraforged";

    public TerraForgedCompatMod() {
        TerraForged.LOG.info("Registered {} mod id alias for third-party compatibility", MODID);
    }
}
