package com.terraforged.mod.client.gui.screen.page;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.engine.serialization.serializer.Serializer;
import com.terraforged.mod.client.gui.Page;
import com.terraforged.mod.client.gui.element.Element;
import com.terraforged.mod.client.gui.element.TFCheckBox;
import com.terraforged.mod.client.gui.element.TFLabel;
import com.terraforged.mod.client.gui.element.TFSlider;
import com.terraforged.mod.client.gui.screen.ConfigScreen;
import com.terraforged.mod.client.gui.screen.SettingsDraft;
import com.terraforged.mod.client.gui.util.DataUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * Scrollable settings subsection built from {@link DataUtils#toNBT} of a Settings nested object.
 * (1.18 port of 0.2.x SimplePage + ScrollPane pattern.)
 */
public class ScrollPage implements Page {
    private static final int ROW = 22;

    private final Component title;
    private final SettingsDraft draft;
    private final String sectionKey;
    private final Supplier<Object> sectionSupplier;
    private final Runnable onChange;
    private final Set<String> skipKeys;

    private final List<AbstractWidget> widgets = new ArrayList<>();
    private int left;
    private int top;
    private int width;
    private int height;
    private int contentHeight;
    private double scroll;

    public ScrollPage(
            String titleKey,
            SettingsDraft draft,
            String sectionKey,
            Supplier<Object> sectionSupplier,
            Runnable onChange
    ) {
        this(titleKey, draft, sectionKey, sectionSupplier, onChange, Collections.emptySet());
    }

    public ScrollPage(
            String titleKey,
            SettingsDraft draft,
            String sectionKey,
            Supplier<Object> sectionSupplier,
            Runnable onChange,
            Set<String> skipKeys
    ) {
        this.title = new TranslatableComponent(titleKey);
        this.draft = draft;
        this.sectionKey = sectionKey;
        this.sectionSupplier = sectionSupplier;
        this.onChange = onChange;
        this.skipKeys = skipKeys != null ? skipKeys : Collections.emptySet();
    }

    /** Alias used by ConfigScreen wiring. */
    public static ScrollPage section(
            String titleKey,
            SettingsDraft draft,
            String sectionKey,
            Supplier<Object> sectionSupplier,
            Runnable onChange
    ) {
        return new ScrollPage(titleKey, draft, sectionKey, sectionSupplier, onChange, Collections.emptySet());
    }

    @Override
    public Component title() {
        return this.title;
    }

    @Override
    public void init(ConfigScreen screen, int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.scroll = 0.0D;
        this.widgets.clear();

        this.draft.applyToSettings();
        this.draft.refreshNbt();
        CompoundTag section = this.draft.settingsData().getCompound(this.sectionKey);
        if (section.isEmpty() && this.sectionSupplier.get() != null) {
            section = DataUtils.toNBT(this.sectionSupplier.get());
            this.draft.settingsData().put(this.sectionKey, section);
        }

        AtomicInteger y = new AtomicInteger(0);
        this.addElements(section, true, y);
        this.contentHeight = y.get();
        this.layout();

        for (AbstractWidget widget : this.widgets) {
            screen.addRenderableWidget(widget);
        }
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        int bottom = this.top + this.height;
        for (AbstractWidget widget : this.widgets) {
            boolean inView = widget.y + widget.getHeight() > this.top && widget.y < bottom;
            widget.visible = inView;
            // Keep inactive when scrolled away so they cannot steal Done/Cancel clicks.
            // Must re-enable when scrolled back (TFSlider does this itself; checkboxes do not).
            widget.active = inView;
        }
    }

    @Override
    public void save() {
        this.draft.applyToSettings();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < this.left || mouseX > this.left + this.width || mouseY < this.top || mouseY > this.top + this.height) {
            return false;
        }
        int maxScroll = Math.max(0, this.contentHeight - this.height);
        if (maxScroll <= 0) {
            return false;
        }
        this.scroll = Math.max(0.0D, Math.min(maxScroll, this.scroll - delta * 12.0D));
        this.layout();
        return true;
    }

    private void layout() {
        int offset = (int)this.scroll;
        int y = this.top;
        for (AbstractWidget widget : this.widgets) {
            widget.x = this.left;
            widget.setWidth(this.width);
            widget.y = y - offset;
            y += ROW;
        }
    }

    private void addElements(CompoundTag settings, boolean deep, AtomicInteger y) {
        if (skip(settings)) {
            return;
        }
        DataUtils.streamKeys(settings).forEach(name -> {
            if (this.shouldSkipKey(name)) {
                return;
            }
            AbstractWidget button = this.createButton(name, settings);
            if (button != null) {
                button.setWidth(this.width);
                button.setHeight(20);
                this.widgets.add(button);
                y.addAndGet(ROW);
            } else if (deep) {
                Tag child = settings.get(name);
                if (!(child instanceof CompoundTag compound) || skip(compound)) {
                    return;
                }
                if (!settings.getCompound("#" + name).contains(Serializer.NO_NAME)) {
                    TFLabel label = new TFLabel(Element.getDisplayName(name, settings), 0xE0E0E0);
                    label.setWidth(this.width);
                    this.widgets.add(label);
                    y.addAndGet(ROW);
                }
                this.addElements(compound, true, y);
            }
        });
    }

    private AbstractWidget createButton(String name, CompoundTag value) {
        Tag tag = value.get(name);
        if (tag == null) {
            return null;
        }
        byte type = tag.getId();
        Runnable callback = () -> {
            this.draft.applyToSettings();
            this.onChange.run();
        };
        if (type == Tag.TAG_INT) {
            if (hasLimit(name, value)) {
                return new TFSlider.BoundInt(name, value).callback(callback);
            }
            return new TFSlider.Int(name, value).callback(callback);
        }
        if (type == Tag.TAG_FLOAT) {
            if (hasLimit(name, value)) {
                return new TFSlider.BoundFloat(name, value).callback(callback);
            }
            return new TFSlider.Float(name, value).callback(callback);
        }
        if (type == Tag.TAG_BYTE || hasBoolOptions(name, value)) {
            return new TFCheckBox(name, value).callback(callback);
        }
        return null;
    }

    private boolean shouldSkipKey(String name) {
        if (name == null || this.skipKeys.isEmpty()) {
            return false;
        }
        String key = name.toLowerCase(Locale.ROOT);
        for (String skip : this.skipKeys) {
            if (skip != null && key.equals(skip.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean skip(CompoundTag value) {
        Tag tag = value.get(Serializer.HIDE);
        return tag instanceof ByteTag byteTag && byteTag.getAsByte() == 1;
    }

    private static boolean hasLimit(String name, CompoundTag value) {
        CompoundTag meta = value.getCompound(Serializer.META_PREFIX + name);
        return meta.contains(Serializer.LINK_LOWER) || meta.contains(Serializer.LINK_UPPER);
    }

    private static boolean hasBoolOptions(String name, CompoundTag value) {
        CompoundTag meta = value.getCompound(Serializer.META_PREFIX + name);
        return meta.contains(Serializer.OPTIONS) && value.contains(name, Tag.TAG_BYTE);
    }
}
