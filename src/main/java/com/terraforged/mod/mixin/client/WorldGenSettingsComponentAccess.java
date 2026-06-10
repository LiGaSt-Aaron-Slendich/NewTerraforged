package com.terraforged.mod.mixin.client;

import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.worldselection.WorldGenSettingsComponent;
import net.minecraft.client.gui.screens.worldselection.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={WorldGenSettingsComponent.class})
public interface WorldGenSettingsComponentAccess {
    @Accessor(value="typeButton")
    public CycleButton<WorldPreset> newtf$getTypeButton();
}
