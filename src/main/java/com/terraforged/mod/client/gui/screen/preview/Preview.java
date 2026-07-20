package com.terraforged.mod.client.gui.screen.preview;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.client.gui.screen.SettingsDraft;
import com.terraforged.noise.util.NoiseUtil;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.TextComponent;

/**
 * Live preview map widget (scaffold of 0.2.x Preview).
 * Async ClimateNoise paint → DynamicTexture; settings pages will drive this later.
 */
public final class Preview extends AbstractWidget {
    public static final int SIZE = 256;

    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "ntfg-world-preview");
        t.setDaemon(true);
        return t;
    });

    private final SettingsDraft draft;
    private final PreviewSettings previewSettings;
    private final DynamicTexture texture;
    private final AtomicInteger generation = new AtomicInteger();

    private volatile CompletableFuture<?> task;
    private String hovered = "";
    private long lastRequestMs;

    public Preview(SettingsDraft draft, PreviewSettings previewSettings) {
        super(0, 0, SIZE, SIZE, new TextComponent("Preview"));
        this.draft = draft;
        this.previewSettings = previewSettings;
        this.texture = new DynamicTexture(new NativeImage(SIZE, SIZE, true));
        this.fillPlaceholder();
    }

    public void requestUpdate() {
        long now = System.currentTimeMillis();
        if (now - this.lastRequestMs < 40L) {
            return;
        }
        this.lastRequestMs = now;
        this.draft.applyToSettings();
        int gen = this.generation.incrementAndGet();
        int seed = this.draft.seed();
        com.terraforged.engine.settings.Settings settingsSnap = copyEngineSettings(this.draft.settings());
        PreviewSettings snap = this.copySettings();
        if (this.task != null) {
            this.task.cancel(true);
        }
        this.task = CompletableFuture.runAsync(() -> this.paint(gen, seed, settingsSnap, snap), WORKERS);
    }

    public void close() {
        this.generation.incrementAndGet();
        if (this.task != null) {
            this.task.cancel(true);
        }
        this.texture.close();
    }

    @Override
    public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, this.texture.getId());
        RenderSystem.enableBlend();
        blit(pose, this.x, this.y, 0, 0, this.width, this.height, this.width, this.height);
        this.updateHover(mouseX, mouseY);
        if (this.previewSettings.showCoords && !this.hovered.isEmpty()) {
            drawCenteredString(pose, Minecraft.getInstance().font, this.hovered, mouseX, mouseY - 10, 0xFFFFFF);
        }
        String legend = "seed " + this.draft.seed() + "  |  " + this.previewSettings.display.name() + "  |  zoom " + this.previewSettings.zoom;
        drawString(pose, Minecraft.getInstance().font, legend, this.x + 2, this.y + this.height + 4, 0xA0A0A0);
    }

    @Override
    public void updateNarration(NarrationElementOutput narration) {
        this.defaultButtonNarrationText(narration);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOver(mouseX, mouseY) && !this.hovered.isEmpty()) {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.hovered);
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }
        return false;
    }

    private void paint(int gen, int seed, com.terraforged.engine.settings.Settings settings, PreviewSettings snap) {
        try {
            PreviewSampler sampler = PreviewSampler.createFromSettings(seed, this.draft.levels().copy(), settings);
            NativeImage image = this.texture.getPixels();
            if (image == null || gen != this.generation.get()) {
                return;
            }
            float step = Math.max(1.0F, 1.5F * (101 - snap.zoom));
            float half = SIZE * 0.5F;
            for (int z = 0; z < SIZE; ++z) {
                if (gen != this.generation.get()) {
                    return;
                }
                for (int x = 0; x < SIZE; ++x) {
                    float wx = (x - half) * step;
                    float wz = (z - half) * step;
                    int argb = sampler.color(wx, wz, snap.display);
                    image.setPixelRGBA(x, z, argbToNative(argb));
                }
            }
            Minecraft.getInstance().execute(() -> {
                if (gen == this.generation.get()) {
                    this.texture.upload();
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static com.terraforged.engine.settings.Settings copyEngineSettings(com.terraforged.engine.settings.Settings src) {
        com.terraforged.engine.settings.Settings copy = new com.terraforged.engine.settings.Settings();
        net.minecraft.nbt.CompoundTag nbt = com.terraforged.mod.client.gui.util.DataUtils.toCompactNBT(src);
        com.terraforged.mod.client.gui.util.DataUtils.fromNBT(nbt, copy);
        copy.world.seed = src.world.seed;
        return copy;
    }

    private void fillPlaceholder() {
        NativeImage image = this.texture.getPixels();
        if (image == null) {
            return;
        }
        for (int z = 0; z < SIZE; ++z) {
            for (int x = 0; x < SIZE; ++x) {
                int shade = ((x ^ z) & 16) == 0 ? 40 : 55;
                image.setPixelRGBA(x, z, argbToNative(0xFF000000 | shade << 16 | shade << 8 | shade));
            }
        }
        this.texture.upload();
    }

    private void updateHover(int mouseX, int mouseY) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            this.hovered = "";
            return;
        }
        float step = Math.max(1.0F, 1.5F * (101 - this.previewSettings.zoom));
        float half = SIZE * 0.5F;
        int lx = mouseX - this.x;
        int lz = mouseY - this.y;
        int wx = NoiseUtil.round((lx - half) * step);
        int wz = NoiseUtil.round((lz - half) * step);
        this.hovered = wx + ":" + wz;
    }

    private PreviewSettings copySettings() {
        PreviewSettings copy = new PreviewSettings();
        copy.zoom = this.previewSettings.zoom;
        copy.display = this.previewSettings.display;
        copy.showCoords = this.previewSettings.showCoords;
        return copy;
    }

    /** Java ARGB → NativeImage ABGR. */
    private static int argbToNative(int argb) {
        int a = argb >>> 24 & 0xFF;
        int r = argb >> 16 & 0xFF;
        int g = argb >> 8 & 0xFF;
        int b = argb & 0xFF;
        return a << 24 | b << 16 | g << 8 | r;
    }
}
