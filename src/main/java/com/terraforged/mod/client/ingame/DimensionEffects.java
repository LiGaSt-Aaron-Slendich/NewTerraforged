package com.terraforged.mod.client.ingame;

import com.terraforged.mod.util.ReflectionUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class DimensionEffects
extends DimensionSpecialEffects {
    public static final AtomicInteger CLOUD_HEIGHT = new AtomicInteger(300);
    private static final MethodHandle REGISTRY_GETTER = ReflectionUtil.field(DimensionSpecialEffects.class, Object2ObjectMap.class, new String[0]);
    protected final DimensionSpecialEffects source;

    public DimensionEffects() {
        this((DimensionSpecialEffects)new DimensionSpecialEffects.OverworldEffects());
    }

    public DimensionEffects(DimensionSpecialEffects source) {
        super(source.getCloudHeight(), source.hasGround(), source.skyType(), source.forceBrightLightmap(), source.constantAmbientLight());
        this.source = source;
    }

    public float getCloudHeight() {
        return CLOUD_HEIGHT.get();
    }

    public Vec3 getBrightnessDependentFogColor(Vec3 pos, float intensity) {
        return this.source.getBrightnessDependentFogColor(pos, intensity);
    }

    public boolean isFoggyAt(int x, int z) {
        return this.source.isFoggyAt(x, z);
    }

    public static void register(ResourceLocation location, DimensionSpecialEffects effects) {
        try {
            DimensionEffects.getRegistry().put(location, effects);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static Object2ObjectMap<ResourceLocation, DimensionSpecialEffects> getRegistry() {
        try {
            return (Object2ObjectMap<ResourceLocation, DimensionSpecialEffects>) REGISTRY_GETTER.invoke();
        }
        catch (Throwable e) {
            throw new Error(e);
        }
    }
}
