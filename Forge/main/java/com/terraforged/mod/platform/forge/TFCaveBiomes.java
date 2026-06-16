package com.terraforged.mod.platform.forge;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.data.ModCaveBiomeFactories;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

public final class TFCaveBiomes {
    public static final DeferredRegister<Biome> REGISTER = DeferredRegister.create((IForgeRegistry)ForgeRegistries.BIOMES, (String)"newterraforged");
    public static final RegistryObject<Biome> THERMAL_SPRINGS = REGISTER.register("cave_thermal_springs", ModCaveBiomeFactories::thermalSprings);
    public static final RegistryObject<Biome> UNDERGROUND_JUNGLE = REGISTER.register("cave_underground_jungle", ModCaveBiomeFactories::undergroundJungle);
    public static final RegistryObject<Biome> STEAMING_JUNGLE = REGISTER.register("cave_steaming_jungle", ModCaveBiomeFactories::steamingJungle);
    public static final RegistryObject<Biome> SULFUR_RIVER = REGISTER.register("cave_sulfur_river", ModCaveBiomeFactories::sulfurRiver);

    private TFCaveBiomes() {
    }

    public static void register(IEventBus modBus) {
        REGISTER.register(modBus);
        TerraForged.LOG.info("[TFCaveBiomes] Registered custom cave biomes via Forge");
    }
}
