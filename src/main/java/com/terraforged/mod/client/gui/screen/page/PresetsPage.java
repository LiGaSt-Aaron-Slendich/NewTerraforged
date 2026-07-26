package com.terraforged.mod.client.gui.screen.page;

import com.terraforged.mod.client.gui.Page;
import com.terraforged.mod.client.gui.screen.ConfigScreen;
import com.terraforged.mod.client.gui.screen.PresetWidgets;
import com.terraforged.mod.client.gui.screen.SettingsDraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Presets page — reset, save to json, import from json. */
public final class PresetsPage implements Page {
    private final SettingsDraft draft;
    private final Runnable onChange;
    private final Component title = Component.translatable("newterraforged.gui.page.presets");

    public PresetsPage(SettingsDraft draft, Runnable onChange) {
        this.draft = draft;
        this.onChange = onChange;
    }

    @Override
    public Component title() {
        return this.title;
    }

    @Override
    public void init(ConfigScreen screen, int left, int top, int width, int height) {
        int buttonWidth = Math.min(width, 180);
        int gap = 4;
        screen.addRenderableWidget(new Button(
                left,
                top,
                buttonWidth,
                20,
                Component.translatable("newterraforged.gui.presets.reset"),
                b -> {
                    this.draft.resetDefaults();
                    this.onChange.run();
                    screen.reloadPages();
                }
        ));
        screen.addRenderableWidget(new Button(
                left,
                top + 20 + gap,
                buttonWidth,
                20,
                Component.translatable("newterraforged.gui.presets.save"),
                b -> PresetWidgets.save(screen, this.draft)
        ));
        screen.addRenderableWidget(new Button(
                left,
                top + (20 + gap) * 2,
                buttonWidth,
                20,
                Component.translatable("newterraforged.gui.presets.import"),
                b -> PresetWidgets.openImport(screen, this.draft, () -> {
                    this.onChange.run();
                    screen.reloadPages();
                })
        ));
    }
}
