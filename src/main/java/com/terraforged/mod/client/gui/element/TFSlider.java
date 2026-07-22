package com.terraforged.mod.client.gui.element;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.engine.serialization.serializer.Serializer;
import com.terraforged.mod.client.gui.screen.nv.NvAccess;
import com.terraforged.noise.util.NoiseUtil;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.client.gui.widget.ForgeSlider;

/** Settings slider bound to an NBT field (port of 0.2.x TFSlider → ForgeSlider). */
public abstract class TFSlider extends ForgeSlider implements Element {
    protected final String name;
    protected final CompoundTag value;
    private final List<String> tooltip;
    private final DependencyBinding binding;
    private final boolean featureBlocked;
    private boolean lock;
    private Runnable callback = () -> {
    };

    protected TFSlider(String name, CompoundTag value, boolean decimal) {
        super(
                0,
                0,
                100,
                20,
                new TextComponent(
                        NvAccess.isBlockedSetting(name)
                                ? NvAccess.BLOCKED_LABEL
                                : Element.getDisplayName(name, value) + ": "
                ),
                TextComponent.EMPTY,
                min(name, value),
                max(name, value),
                min(name, value),
                decimal ? 0.0D : 1.0D,
                decimal ? 3 : 0,
                true
        );
        this.name = name;
        this.value = value;
        this.featureBlocked = NvAccess.isBlockedSetting(name);
        this.tooltip = this.featureBlocked ? NvAccess.blockedTooltip() : Element.getToolTip(name, value);
        this.binding = DependencyBinding.of(name, value);
        if (this.featureBlocked) {
            this.active = false;
            this.drawString = false;
            this.setMessage(new TextComponent(NvAccess.BLOCKED_LABEL));
        }
    }

    public TFSlider callback(Runnable callback) {
        this.callback = callback;
        return this;
    }

    public boolean isFeatureBlocked() {
        return this.featureBlocked;
    }

    @Override
    public List<String> getTooltip() {
        return this.tooltip;
    }

    @Override
    protected void updateMessage() {
        if (this.featureBlocked) {
            this.setMessage(new TextComponent(NvAccess.BLOCKED_LABEL));
            return;
        }
        super.updateMessage();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (this.featureBlocked && this.visible) {
            return mouseX >= this.x
                    && mouseY >= this.y
                    && mouseX < this.x + this.width
                    && mouseY < this.y + this.height;
        }
        return super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        if (this.featureBlocked) {
            this.active = false;
        } else {
            this.active = this.visible && this.binding.isValid();
        }
        super.render(pose, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.featureBlocked) {
            return false;
        }
        if (button == 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (this.clicked(mouseX, mouseY)) {
            this.reset();
            this.callback.run();
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }
        return false;
    }

    @Override
    protected void applyValue() {
        if (this.featureBlocked) {
            return;
        }
        if (!this.lock) {
            this.lock = true;
            this.onChange(this.value);
            this.lock = false;
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        if (!this.featureBlocked && this.isHoveredOrFocused()) {
            this.callback.run();
        }
        super.onRelease(mouseX, mouseY);
    }

    protected abstract void onChange(CompoundTag value);

    protected abstract void reset();

    private static float min(String name, CompoundTag value) {
        CompoundTag meta = value.getCompound(Serializer.META_PREFIX + name);
        return meta.contains(Serializer.BOUND_MIN) ? meta.getFloat(Serializer.BOUND_MIN) : 0.0F;
    }

    private static float max(String name, CompoundTag value) {
        CompoundTag meta = value.getCompound(Serializer.META_PREFIX + name);
        return meta.contains(Serializer.BOUND_MAX) ? meta.getFloat(Serializer.BOUND_MAX) : 1.0F;
    }

    public static final class Int extends TFSlider {
        private final int defaultValue;

        public Int(String name, CompoundTag value) {
            super(name, value, false);
            this.defaultValue = value.getInt(name);
            this.setValue(this.defaultValue);
        }

        @Override
        protected void onChange(CompoundTag value) {
            value.putInt(this.name, this.getValueInt());
        }

        @Override
        protected void reset() {
            this.setValue(this.defaultValue);
            value.putInt(this.name, this.defaultValue);
        }
    }

    public static final class Float extends TFSlider {
        private final float defaultValue;

        public Float(String name, CompoundTag value) {
            super(name, value, true);
            this.defaultValue = value.getFloat(name);
            this.setValue(this.defaultValue);
        }

        @Override
        protected void onChange(CompoundTag value) {
            int i = (int)(this.getValue() * 1000.0D);
            float f = i / 1000.0F;
            value.putFloat(this.name, f);
        }

        @Override
        protected void reset() {
            this.setValue(this.defaultValue);
            value.putFloat(this.name, this.defaultValue);
        }
    }

    public abstract static class BoundSlider extends TFSlider {
        protected final float pad;
        protected final String lower;
        protected final String upper;

        protected BoundSlider(String name, CompoundTag value, float defaultPad, boolean decimal) {
            super(name, value, decimal);
            CompoundTag meta = value.getCompound(Serializer.META_PREFIX + name);
            float pad = meta.contains(Serializer.LINK_PAD) ? meta.getFloat(Serializer.LINK_PAD) : -1.0F;
            this.pad = pad < 0.0F ? defaultPad : pad;
            this.lower = meta.getString(Serializer.LINK_LOWER);
            this.upper = meta.getString(Serializer.LINK_UPPER);
        }

        protected float getLower(CompoundTag value) {
            if (this.lower == null || this.lower.isEmpty()) {
                return (float)(this.minValue - this.pad);
            }
            return value.contains(this.lower)
                    ? (value.contains(this.lower, Tag.TAG_INT) ? value.getInt(this.lower) : value.getFloat(this.lower))
                    : (float)(this.minValue - this.pad);
        }

        protected float getUpper(CompoundTag value) {
            if (this.upper == null || this.upper.isEmpty()) {
                return (float)(this.maxValue + this.pad);
            }
            return value.contains(this.upper)
                    ? (value.contains(this.upper, Tag.TAG_INT) ? value.getInt(this.upper) : value.getFloat(this.upper))
                    : (float)(this.maxValue + this.pad);
        }
    }

    public static final class BoundFloat extends BoundSlider {
        private final float defaultValue;

        public BoundFloat(String name, CompoundTag value) {
            this(name, value, 0.005F);
        }

        public BoundFloat(String name, CompoundTag value, float pad) {
            super(name, value, pad, true);
            this.defaultValue = value.getFloat(name);
            this.setValue(this.defaultValue);
        }

        @Override
        protected void onChange(CompoundTag value) {
            int i = (int)(this.getValue() * 1000.0D);
            float lower = this.getLower(value) + this.pad;
            float upper = this.getUpper(value) - this.pad;
            float val = NoiseUtil.clamp(i / 1000.0F, lower, upper);
            value.putFloat(this.name, val);
            this.setValue(val);
        }

        @Override
        protected void reset() {
            this.setValue(this.defaultValue);
            value.putFloat(this.name, this.defaultValue);
        }
    }

    public static final class BoundInt extends BoundSlider {
        private final int defaultValue;

        public BoundInt(String name, CompoundTag value) {
            this(name, value, 1);
        }

        public BoundInt(String name, CompoundTag value, int pad) {
            super(name, value, pad, false);
            this.defaultValue = value.getInt(name);
            this.setValue(this.defaultValue);
        }

        @Override
        protected void onChange(CompoundTag value) {
            int i = this.getValueInt();
            float lower = this.getLower(value) + this.pad;
            float upper = this.getUpper(value) - this.pad;
            int val = NoiseUtil.round(NoiseUtil.clamp(i, lower, upper));
            value.putInt(this.name, val);
            this.setValue(val);
        }

        @Override
        protected void reset() {
            this.setValue(this.defaultValue);
            value.putInt(this.name, this.defaultValue);
        }
    }
}
