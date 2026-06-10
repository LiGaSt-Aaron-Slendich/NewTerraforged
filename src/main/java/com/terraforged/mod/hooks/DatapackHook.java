package com.terraforged.mod.hooks;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.util.ReflectionUtil;
import com.terraforged.mod.worldgen.datapack.DataPackExporter;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldPreset;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

public class DatapackHook {
    private static final String PACK_FILE_ID = "file/NewTerraforged-v0.2.zip";
    private static final String PRESET_TRANSLATION_KEY = "generator.newterraforged.newterraforged";
    private static boolean newTerraForgedIntent;
    private static boolean pendingServerApply;

    public static boolean hasNewTerraForgedIntent() {
        return newTerraForgedIntent;
    }

    public static void clearNewTerraForgedIntent() {
        newTerraForgedIntent = false;
        pendingServerApply = false;
    }

    public static void markServerApplyPending() {
        pendingServerApply = true;
    }

    public static boolean consumeServerApplyPending() {
        boolean value = pendingServerApply;
        pendingServerApply = false;
        return value;
    }

    public static boolean shouldPatchServerGenerator() {
        return pendingServerApply || newTerraForgedIntent;
    }

    public static RepositorySource[] injectRepositorySource(RepositorySource[] sources) {
        RepositorySource[] copy = Arrays.copyOf(sources, sources.length + 1);
        copy[sources.length] = new TerraForgedRepositorySource();
        return copy;
    }

    public static void injectDatapack(PackRepository repository, Path dir) {
        ArrayList<String> selected;
        if (!repository.isAvailable(PACK_FILE_ID)) {
            DataPackExporter.createWorldDatapack(dir);
            TerraForgedRepositorySource.inject(repository, dir);
            TerraForged.LOG.info("Injected datapack {}", PACK_FILE_ID);
        }
        if (!(selected = new ArrayList<>(repository.getSelectedIds())).contains(PACK_FILE_ID)) {
            selected = new ArrayList<String>(selected);
            selected.add(PACK_FILE_ID);
            repository.setSelected(selected);
            TerraForged.LOG.info("Selected datapack {}", PACK_FILE_ID);
        }
    }

    public static void selectPreset(Object object) {
        if (!(object instanceof CreateWorldScreen)) {
            return;
        }
        CreateWorldScreen screen = (CreateWorldScreen)object;
        RegistryAccess access = screen.worldGenSettingsComponent.registryHolder();
        if (access == null || access.ownedRegistry(TerraForged.TERRAINS.get()).isEmpty()) {
            return;
        }
        for (GuiEventListener listener : screen.children()) {
            CycleButton button;
            if (!(listener instanceof CycleButton) || !((button = (CycleButton)listener).getValue() instanceof WorldPreset)) continue;
            if (DatapackHook.cycleToPreset(button, PRESET_TRANSLATION_KEY)) {
                newTerraForgedIntent = true;
                TerraForged.LOG.info("Selected NewTerraForged world preset");
            }
            return;
        }
    }

    public static void reselectPreset(CycleButton<?> typeButton) {
        if (!newTerraForgedIntent || typeButton == null) {
            return;
        }
        if (!DatapackHook.isPreset(typeButton.getValue(), PRESET_TRANSLATION_KEY)) {
            DatapackHook.cycleToPreset(typeButton, PRESET_TRANSLATION_KEY);
        }
    }

    private static boolean cycleToPreset(CycleButton<?> button, String translationKey) {
        if (DatapackHook.isPreset(button.getValue(), translationKey)) {
            return false;
        }
        Object first = button.getValue();
        for (int i = 0; i < 64; ++i) {
            button.onPress();
            Object value = button.getValue();
            if (DatapackHook.isPreset(value, translationKey)) {
                return true;
            }
            if (value == first) break;
        }
        return false;
    }

    private static boolean isPreset(Object value, String translationKey) {
        TranslatableComponent component;
        WorldPreset preset;
        Component component2;
        return value instanceof WorldPreset && (component2 = (preset = (WorldPreset)value).description()) instanceof TranslatableComponent && translationKey.equals((component = (TranslatableComponent)component2).getKey());
    }

    public static class TerraForgedRepositorySource
    implements RepositorySource {
        private static final MethodHandle PACK_SOURCES = ReflectionUtil.field(PackRepository.class, Set.class, new String[0]);
        private static final RepositorySource NOOP = (consumer, constructor) -> {};
        protected RepositorySource source = NOOP;

        public void setDir(Path path) {
            this.source = new FolderRepositorySource(path.toFile(), PackSource.DEFAULT);
        }

        public void loadPacks(Consumer<Pack> pack, Pack.PackConstructor constructor) {
            this.source.loadPacks(pack, constructor);
        }

        public static void inject(PackRepository repository, Path dir) {
            try {
                Set<?> set = (Set<?>) (Object) PACK_SOURCES.invokeExact(repository);
                for (Object object : set) {
                    if (!(object instanceof TerraForgedRepositorySource)) continue;
                    TerraForgedRepositorySource source = (TerraForgedRepositorySource)object;
                    source.setDir(dir);
                    return;
                }
            }
            catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}
