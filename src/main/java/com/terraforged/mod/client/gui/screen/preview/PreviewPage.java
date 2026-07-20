package com.terraforged.mod.client.gui.screen.preview;

import com.terraforged.mod.client.gui.Page;
import com.terraforged.mod.client.gui.screen.ConfigScreen;
import com.terraforged.mod.client.gui.screen.SettingsDraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

/** Right-column preview controls + map. */
public final class PreviewPage implements Page {
    private final SettingsDraft draft;
    private final PreviewSettings previewSettings = new PreviewSettings();
    private final Preview preview;

    public PreviewPage(SettingsDraft draft) {
        this.draft = draft;
        this.preview = new Preview(draft, this.previewSettings);
    }

    public Preview preview() {
        return this.preview;
    }

    public PreviewSettings settings() {
        return this.previewSettings;
    }

    @Override
    public Component title() {
        return new TranslatableComponent("newterraforged.gui.preview");
    }

    @Override
    public void init(ConfigScreen screen, int left, int top, int width, int height) {
        int mapLeft = left + Math.max(0, (width - Preview.SIZE) / 2);
        this.preview.x = mapLeft;
        this.preview.y = top + 48;
        this.preview.setWidth(Preview.SIZE);
        this.preview.setHeight(Preview.SIZE);
        screen.addRenderableWidget(this.preview);

        screen.addRenderableWidget(new Button(mapLeft, top, 100, 20, new TranslatableComponent("newterraforged.gui.preview.seed"), b -> {
            this.draft.randomizeSeed();
            this.refresh();
        }));
        screen.addRenderableWidget(new Button(mapLeft + 104, top, 100, 20, new TextComponent(this.previewSettings.display.name()), b -> {
            this.previewSettings.display = this.previewSettings.display.next();
            b.setMessage(new TextComponent(this.previewSettings.display.name()));
            this.refresh();
        }));
        screen.addRenderableWidget(new Button(mapLeft + 208, top, 48, 20, new TextComponent("−"), b -> {
            this.previewSettings.zoom = Math.max(1, this.previewSettings.zoom - 8);
            this.refresh();
        }));
        screen.addRenderableWidget(new Button(mapLeft + 258, top, 48, 20, new TextComponent("+"), b -> {
            this.previewSettings.zoom = Math.min(100, this.previewSettings.zoom + 8);
            this.refresh();
        }));

        this.refresh();
    }

    public void refresh() {
        this.draft.applyToSettings();
        this.preview.requestUpdate();
    }

    @Override
    public void close() {
        this.preview.close();
    }
}
