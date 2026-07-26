package com.terraforged.mod.platform.forge;

import com.terraforged.mod.TerraForged;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal stub — full cave-biomes.toml / CaveBiomeRuleRegistry wiring deferred with NewTF cave extras.
 */
public final class TFCaveBiomeConfig {
    public static TFCaveBiomeConfig INSTANCE = new TFCaveBiomeConfig();

    public boolean autoRegisterCaveBiomes = true;
    public boolean enableInspector = false;
    public final List<Entry> primary = new ArrayList<>();
    public final List<Entry> transition = new ArrayList<>();
    public final List<Entry> special = new ArrayList<>();
    public final List<Entry> coastal = new ArrayList<>();

    public static void load() {
        INSTANCE = new TFCaveBiomeConfig();
        TerraForged.LOG.info("[TFCaveBiomeConfig] stub load (cave biome rules deferred on 1.19)");
    }

    public boolean isBlacklisted(net.minecraft.resources.ResourceLocation id) {
        return false;
    }

    public record Entry(String biomeId, float temperature, float vegetationDensity, float weight, String placementType,
                        float ceilingPatchMin, float ceilingPatchMax, float islandMaxRadius) {}
}
