package com.terraforged.mod.client.gui.element;

import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.client.gui.screen.nv.NvAccess;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.nbt.CompoundTag;

/** Boolean toggle bound to an NBT field. */
public class TFCheckBox extends Button implements Element {
    private final String name;
    private final CompoundTag value;
    private final boolean featureBlocked;
    private boolean checked;
    private Runnable callback = () -> {
    };

    public TFCheckBox(String name, CompoundTag value) {
        super(0, 0, 100, 20, Component.literal(Element.getDisplayName(name, value)), b -> {
        });
        this.name = name;
        this.value = value;
        this.featureBlocked = NvAccess.isBlockedSetting(name);
        this.checked = value.getBoolean(name);
        this.updateLabel();
        if (this.featureBlocked) {
            this.active = false;
        }
    }

    public TFCheckBox(String displayString, boolean isChecked) {
        super(0, 0, 70, 20, Component.literal(displayString), b -> {
        });
        this.name = null;
        this.value = null;
        this.featureBlocked = false;
        this.checked = isChecked;
    }

    public TFCheckBox callback(Runnable callback) {
        this.callback = callback;
        return this;
    }

    public boolean isFeatureBlocked() {
        return this.featureBlocked;
    }

    public boolean isChecked() {
        return this.checked;
    }

    public void setChecked(boolean checked) {
        if (this.featureBlocked) {
            return;
        }
        this.checked = checked;
        if (this.value != null && this.name != null) {
            this.value.putBoolean(this.name, checked);
        }
        this.updateLabel();
    }

    @Override
    public List<String> getTooltip() {
        if (this.featureBlocked) {
            return NvAccess.blockedTooltip();
        }
        if (this.name != null && this.value != null) {
            return Element.getToolTip(this.name, this.value);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Vanilla requires active=true; blocked features stay inactive but must show tooltips.
        if (this.featureBlocked && this.visible) {
            return mouseX >= this.x
                    && mouseY >= this.y
                    && mouseX < this.x + this.width
                    && mouseY < this.y + this.height;
        }
        return super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void onPress() {
        if (this.featureBlocked) {
            return;
        }
        this.setChecked(!this.checked);
        this.callback.run();
    }

    @Override
    public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        if (this.featureBlocked) {
            this.active = false;
            super.renderButton(pose, mouseX, mouseY, partialTick);
            return;
        }
        // Keep clickable whenever visible (ScrollPage may have toggled active for cull).
        if (this.visible) {
            this.active = true;
        }
        boolean wasActive = this.active;
        // Grey "checked" look without permanently disabling the widget.
        this.active = !this.checked;
        super.renderButton(pose, mouseX, mouseY, partialTick);
        this.active = wasActive;
    }

    private void updateLabel() {
        if (this.featureBlocked) {
            this.setMessage(Component.literal(NvAccess.BLOCKED_LABEL));
            return;
        }
        if (this.name != null && this.value != null) {
            String display = Element.getDisplayName(this.name, this.value);
            this.setMessage(Component.literal(display + ": " + (this.checked ? "ON" : "OFF")));
        }
    }
}
