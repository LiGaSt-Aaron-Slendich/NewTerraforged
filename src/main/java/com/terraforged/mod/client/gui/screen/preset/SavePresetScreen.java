package com.terraforged.mod.client.gui.screen.preset;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.client.gui.screen.ConfigScreen;
import com.terraforged.mod.client.gui.screen.SettingsDraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public final class SavePresetScreen extends Screen {
    private final ConfigScreen parent;
    private final SettingsDraft draft;
    private EditBox nameBox;

    public SavePresetScreen(ConfigScreen parent, SettingsDraft draft) {
        super(new TranslatableComponent("newterraforged.gui.presets.save.title"));
        this.parent = parent;
        this.draft = draft;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        this.nameBox = new EditBox(this.font, cx - 100, cy - 10, 200, 20, new TranslatableComponent("newterraforged.gui.presets.save.name"));
        this.nameBox.setMaxLength(48);
        this.nameBox.setValue("my_preset");
        this.addWidget(this.nameBox);
        this.setInitialFocus(this.nameBox);
        this.addRenderableWidget(new Button(cx - 155, cy + 24, 150, 20, CommonComponents.GUI_DONE, b -> this.save()));
        this.addRenderableWidget(new Button(cx + 5, cy + 24, 150, 20, CommonComponents.GUI_CANCEL, b -> this.onClose()));
    }

    private void save() {
        try {
            // Sync preview seed into draft before encoding the preset.
            this.draft.applyToSettings();
            var path = PresetLibrary.saveUserPreset(this.nameBox.getValue(), this.draft.toGeneratorSettings());
            this.minecraft.getToasts().addToast(SystemToast.multiline(
                    this.minecraft,
                    SystemToast.SystemToastIds.WORLD_GEN_SETTINGS_TRANSFER,
                    new TextComponent("NewTF settings saved"),
                    new TextComponent(path.toString())
            ));
            this.onClose();
        } catch (Throwable t) {
            TerraForged.LOG.error("Failed to save preset", t);
            this.minecraft.gui.getChat().addMessage(new TextComponent("NewTF: failed to save preset"));
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        drawCenteredString(pose, this.font, this.title, this.width / 2, 40, 0xFFFFFF);
        drawCenteredString(pose, this.font, new TranslatableComponent("newterraforged.gui.presets.save.hint", PresetLibrary.userDir().toString()), this.width / 2, 56, 0xA0A0A0);
        this.nameBox.render(pose, mouseX, mouseY, partialTick);
        super.render(pose, mouseX, mouseY, partialTick);
    }
}
