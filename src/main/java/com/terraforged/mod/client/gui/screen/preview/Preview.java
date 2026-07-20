package com.terraforged.mod.client.gui.screen.preview;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.engine.cell.Cell;
import com.terraforged.engine.concurrent.cache.CacheManager;
import com.terraforged.engine.concurrent.task.LazyCallable;
import com.terraforged.engine.concurrent.thread.ThreadPool;
import com.terraforged.engine.concurrent.thread.ThreadPools;
import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.tile.Size;
import com.terraforged.engine.tile.Tile;
import com.terraforged.engine.tile.api.TileFactory;
import com.terraforged.engine.tile.gen.TileGenerator;
import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.engine.world.GeneratorContext;
import com.terraforged.engine.world.continent.MutableVeci;
import com.terraforged.engine.world.continent.SpawnType;
import com.terraforged.engine.world.heightmap.Levels;
import com.terraforged.mod.util.serialization.DataUtils;
import com.terraforged.noise.util.NoiseUtil;
import java.awt.Color;
import java.util.Objects;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;

/**
 * Port of TerraForged 0.2.x {@code Preview}: async {@link TileGenerator} → {@link NativeImage}.
 */
public final class Preview extends AbstractWidget {
    private static final int FACTOR = 4;
    public static final int SIZE = Size.chunkToBlock(1 << FACTOR);
    private static final float[] LEGEND_SCALES = {1.0F, 0.9F, 0.75F, 0.6F};

    private final int offsetX;
    private final int offsetZ;
    private final ThreadPool threadPool = ThreadPools.createDefault();
    private final Random random = new Random(System.currentTimeMillis());
    private final PreviewSettings previewSettings = new PreviewSettings();
    private final DynamicTexture texture = new DynamicTexture(new NativeImage(SIZE, SIZE, true));

    private int seed;
    private long lastUpdate;
    private Tile tile;
    private LazyCallable<Tile> task;
    private CompoundTag lastWorldSettings;
    private CompoundTag lastPreviewSettings;

    private Settings settings = new Settings();
    private final MutableVeci center = new MutableVeci();

    private String hoveredCoords = "";
    private final String[] values = {"", "", ""};
    private final String[] labels = {"Area", "Terrain", "Biome"};

    public Preview(int seed) {
        super(0, 0, SIZE, SIZE, new TextComponent("Preview"));
        this.seed = seed == -1 ? this.random.nextInt() : seed;
        this.offsetX = 0;
        this.offsetZ = 0;
    }

    public int getSeed() {
        return this.seed;
    }

    public PreviewSettings previewSettings() {
        return this.previewSettings;
    }

    public void regenerate() {
        this.seed = this.random.nextInt();
        this.lastWorldSettings = null;
        this.lastPreviewSettings = null;
    }

    public void close() {
        this.texture.close();
        this.threadPool.shutdown();
        CacheManager.get().clear();
    }

    public boolean click(double mx, double my) {
        if (this.updateLegend((int) mx, (int) my) && !this.hoveredCoords.isEmpty()) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            Minecraft.getInstance().keyboardHandler.setClipboard(this.hoveredCoords);
            return true;
        }
        return false;
    }

    @Override
    public void render(PoseStack pose, int mx, int my, float partialTicks) {
        this.height = this.getSize();
        this.preRender();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, this.texture.getId());
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        blit(pose, this.x, this.y, 0, 0, this.width, this.height, this.width, this.height);

        this.updateLegend(mx, my);
        this.renderLegend(pose, mx, my, this.labels, this.values, this.x, this.y + this.width, 10, 0xFFFFFF);
    }

    @Override
    public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        // render() already draws the map; AbstractWidget would call this for button chrome — skip.
    }

    @Override
    public void updateNarration(NarrationElementOutput narration) {
        this.defaultButtonNarrationText(narration);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.click(mouseX, mouseY);
    }

    public void update(Settings settings, CompoundTag prevSettings) {
        long time = System.currentTimeMillis();
        if (time - this.lastUpdate < 20L) {
            return;
        }

        CompoundTag previewSnap = prevSettings.copy();
        CompoundTag worldSettings = DataUtils.toCompactNBT(settings);
        if (Objects.equals(this.lastWorldSettings, worldSettings) && Objects.equals(this.lastPreviewSettings, previewSnap)) {
            return;
        }

        this.lastUpdate = time;
        this.lastWorldSettings = worldSettings;
        this.lastPreviewSettings = previewSnap;

        DataUtils.fromNBT(prevSettings, this.previewSettings);
        settings.world.seed = this.seed;
        this.task = this.generate(settings, prevSettings);
    }

    private int getSize() {
        return this.width;
    }

    private void preRender() {
        if (this.task != null && this.task.isDone()) {
            try {
                this.tile = this.task.get();
                this.renderTile(this.tile);
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                this.task = null;
            }
        }
    }

    private void renderTile(Tile tile) {
        NativeImage image = this.texture.getPixels();
        if (image == null) {
            return;
        }

        RenderMode renderer = this.previewSettings.display;
        Levels levels = new Levels(this.settings.world);
        int stroke = 2;
        int width = tile.getBlockSize().size;

        tile.iterate((cell, x, z) -> {
            if (x < stroke || z < stroke || x >= width - stroke || z >= width - stroke) {
                image.setPixelRGBA(x, z, Color.BLACK.getRGB());
            } else {
                image.setPixelRGBA(x, z, renderer.getColor(cell, levels));
            }
        });
        this.texture.upload();
    }

    private LazyCallable<Tile> generate(Settings settings, CompoundTag prevSettings) {
        DataUtils.fromNBT(prevSettings, this.previewSettings);
        settings.world.seed = this.seed;
        this.settings = settings;

        CacheManager.get().clear();
        GeneratorContext context = GeneratorContext.createNoCache(settings);
        if (settings.world.properties.spawnType == SpawnType.CONTINENT_CENTER) {
            long centerPos = context.worldGenerator.get().getHeightmap().getContinent().getNearestCenter(this.offsetX, this.offsetZ);
            this.center.x = PosUtil.unpackLeft(centerPos);
            this.center.z = PosUtil.unpackRight(centerPos);
        } else {
            this.center.x = 0;
            this.center.z = 0;
        }

        TileFactory renderer = TileGenerator.builder()
                .factory(context.worldGenerator.get())
                .size(FACTOR, 0)
                .pool(this.threadPool)
                .batch(6)
                .build()
                .async();

        return renderer.getTile(this.center.x, this.center.z, this.getZoom(), false);
    }

    private boolean updateLegend(int mx, int my) {
        if (this.tile == null) {
            return false;
        }
        int left = this.x;
        int top = this.y;
        float size = this.width;
        int zoom = this.getZoom();
        int width = Math.max(1, this.tile.getBlockSize().size * zoom);
        int height = Math.max(1, this.tile.getBlockSize().size * zoom);
        this.values[0] = width + "x" + height;
        if (mx >= left && mx <= left + size && my >= top && my <= top + size) {
            float fx = (mx - left) / size;
            float fz = (my - top) / size;
            int ix = NoiseUtil.round(fx * this.tile.getBlockSize().size);
            int iz = NoiseUtil.round(fz * this.tile.getBlockSize().size);
            Cell cell = this.tile.getCell(ix, iz);
            this.values[1] = getTerrainName(cell);
            this.values[2] = getBiomeName(cell);
            int dx = (ix - this.tile.getBlockSize().size / 2) * zoom;
            int dz = (iz - this.tile.getBlockSize().size / 2) * zoom;
            this.hoveredCoords = (this.center.x + dx) + ":" + (this.center.z + dz);
            return true;
        }
        this.hoveredCoords = "";
        return false;
    }

    private float getLegendScale() {
        int index = Minecraft.getInstance().options.guiScale - 1;
        if (index < 0 || index >= LEGEND_SCALES.length) {
            index = LEGEND_SCALES.length - 1;
        }
        return LEGEND_SCALES[index];
    }

    private void renderLegend(PoseStack pose, int mx, int my, String[] labels, String[] values, int left, int top, int lineHeight, int color) {
        float scale = this.getLegendScale();
        pose.pushPose();
        pose.translate(left + 3.75F * scale, top - lineHeight * (3.2F * scale), 0.0D);
        pose.scale(scale, scale, 1.0F);

        Font font = Minecraft.getInstance().font;
        int spacing = 0;
        for (String s : labels) {
            spacing = Math.max(spacing, font.width(s));
        }

        float maxWidth = (this.width - 4) / scale;
        for (int i = 0; i < labels.length && i < values.length; i++) {
            String label = labels[i];
            String value = values[i];
            while (!value.isEmpty() && spacing + font.width(value) > maxWidth) {
                value = value.substring(0, value.length() - 1);
            }
            drawString(pose, font, label, 0, i * lineHeight, color);
            drawString(pose, font, value, spacing, i * lineHeight, color);
        }
        pose.popPose();

        if (PreviewSettings.showCoords && !this.hoveredCoords.isEmpty()) {
            drawCenteredString(pose, font, this.hoveredCoords, mx, my - 10, 0xFFFFFF);
        }
    }

    private int getZoom() {
        return NoiseUtil.round(1.5F * (101 - this.previewSettings.zoom));
    }

    private static String getTerrainName(Cell cell) {
        if (cell.terrain.isRiver()) {
            return "river";
        }
        return cell.terrain.getName().toLowerCase();
    }

    private static String getBiomeName(Cell cell) {
        String terrain = cell.terrain.getName().toLowerCase();
        if (terrain.contains("ocean")) {
            if (cell.temperature < 0.3F) {
                return "cold_" + terrain;
            }
            if (cell.temperature > 0.6F) {
                return "warm_" + terrain;
            }
            return terrain;
        }
        if (terrain.contains("river")) {
            return "river";
        }
        return cell.biome.name().toLowerCase();
    }
}
