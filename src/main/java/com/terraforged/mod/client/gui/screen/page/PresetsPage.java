package com.terraforged.mod.client.gui.screen.page;

import com.terraforged.mod.client.gui.Page;
import com.terraforged.mod.client.gui.screen.ConfigScreen;
import com.terraforged.mod.client.gui.screen.SettingsDraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

/** Presets page — reset to factory defaults (full file browser omitted). */
public final class PresetsPage implements Page {
    private final SettingsDraft draft;
    private final Runnable onChange;
    private final Component title = new TranslatableComponent("newterraforged.gui.page.presets");

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
        screen.addRenderableWidget(new Button(
                left,
                top,
                Math.min(width, 180),
                20,
                new TranslatableComponent("newterraforged.gui.presets.reset"),
                b -> {
                    this.draft.resetDefaults();
                    this.onChange.run();
                    screen.reloadPages();
                }
        ));
    }
}
