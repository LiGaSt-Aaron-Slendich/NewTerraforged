package com.terraforged.mod.client.gui.element;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;

/** Boolean toggle bound to an NBT field. */
public class TFCheckBox extends Button implements Element {
    private final String name;
    private final CompoundTag value;
    private boolean checked;
    private Runnable callback = () -> {
    };

    public TFCheckBox(String name, CompoundTag value) {
        super(0, 0, 100, 20, new TextComponent(Element.getDisplayName(name, value)), b -> {
        });
        this.name = name;
        this.value = value;
        this.checked = value.getBoolean(name);
        this.updateLabel();
    }

    public TFCheckBox(String displayString, boolean isChecked) {
        super(0, 0, 70, 20, new TextComponent(displayString), b -> {
        });
        this.name = null;
        this.value = null;
        this.checked = isChecked;
    }

    public TFCheckBox callback(Runnable callback) {
        this.callback = callback;
        return this;
    }

    public boolean isChecked() {
        return this.checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
        if (this.value != null && this.name != null) {
            this.value.putBoolean(this.name, checked);
        }
        this.updateLabel();
    }

    @Override
    public void onPress() {
        this.setChecked(!this.checked);
        this.callback.run();
    }

    @Override
    public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        boolean wasActive = this.active;
        this.active = !this.checked;
        super.renderButton(pose, mouseX, mouseY, partialTick);
        this.active = wasActive;
    }

    private void updateLabel() {
        if (this.name != null && this.value != null) {
            String display = Element.getDisplayName(this.name, this.value);
            this.setMessage(new TextComponent(display + ": " + (this.checked ? "ON" : "OFF")));
        }
    }
}
