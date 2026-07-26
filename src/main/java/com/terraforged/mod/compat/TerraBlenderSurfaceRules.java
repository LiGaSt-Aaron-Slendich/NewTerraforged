package com.terraforged.mod.compat;

import com.google.common.collect.ImmutableMap;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.GenerationFeatureGates;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * When TerraBlender + content mods are loaded, apply their registered namespace surface rules
 * on NewTerraForged worlds without replacing {@code ChunkGenerator}.
 */
public final class TerraBlenderSurfaceRules {
    private static final String MANAGER = "terrablender.api.SurfaceRuleManager";
    private static final String CATEGORY = "terrablender.api.SurfaceRuleManager$RuleCategory";
    private static final String NAMESPACED = "terrablender.worldgen.surface.NamespacedSurfaceRuleSource";

    private TerraBlenderSurfaceRules() {
    }

    public static SurfaceRules.RuleSource wrap(SurfaceRules.RuleSource vanillaBase) {
        if (!GenerationFeatureGates.namespacedModSurfaceRulesEnabled || !TerraBlenderCompat.isTerraBlenderLoaded()) {
            return vanillaBase;
        }
        Map<String, SurfaceRules.RuleSource> modRules = TerraBlenderSurfaceRules.loadModNamespaceRules();
        if (modRules.isEmpty()) {
            return vanillaBase;
        }
        SurfaceRules.RuleSource namespaced = TerraBlenderSurfaceRules.createNamespaced(vanillaBase, modRules);
        return namespaced != null ? namespaced : vanillaBase;
    }

    private static SurfaceRules.RuleSource createNamespaced(SurfaceRules.RuleSource base, Map<String, SurfaceRules.RuleSource> modRules) {
        try {
            Class<?> cls = Class.forName(NAMESPACED);
            for (Constructor<?> ctor : cls.getConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 2 && SurfaceRules.RuleSource.class.isAssignableFrom(params[0]) && Map.class.isAssignableFrom(params[1])) {
                    return (SurfaceRules.RuleSource) ctor.newInstance(base, modRules);
                }
            }
            Method getNamespaced = Class.forName(MANAGER).getMethod("getNamespacedRules", Class.forName(CATEGORY), SurfaceRules.RuleSource.class);
            Object overworld = Enum.valueOf((Class<Enum>) Class.forName(CATEGORY), "OVERWORLD");
            return (SurfaceRules.RuleSource) getNamespaced.invoke(null, overworld, base);
        }
        catch (Throwable t) {
            TerraForged.LOG.debug("TerraBlender namespaced surface wrapper unavailable: {}", t.toString());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, SurfaceRules.RuleSource> loadModNamespaceRules() {
        try {
            Class<?> categoryClass = Class.forName(CATEGORY);
            Object overworld = Enum.valueOf((Class<Enum>) categoryClass, "OVERWORLD");
            Field rulesField = Class.forName(MANAGER).getDeclaredField("surfaceRules");
            rulesField.setAccessible(true);
            Map<Object, Map<String, SurfaceRules.RuleSource>> all = (Map<Object, Map<String, SurfaceRules.RuleSource>>) rulesField.get(null);
            Map<String, SurfaceRules.RuleSource> overworldRules = all.get(overworld);
            if (overworldRules == null || overworldRules.isEmpty()) {
                return Map.of();
            }
            ImmutableMap.Builder<String, SurfaceRules.RuleSource> builder = ImmutableMap.builder();
            for (Map.Entry<String, SurfaceRules.RuleSource> entry : overworldRules.entrySet()) {
                if ("minecraft".equals(entry.getKey())) {
                    continue;
                }
                builder.put(entry.getKey(), entry.getValue());
            }
            return builder.build();
        }
        catch (Throwable t) {
            TerraForged.LOG.debug("TerraBlender surface rules unavailable: {}", t.toString());
            return Map.of();
        }
    }
}
