package com.terraforged.mod.internal.probe.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.terraforged.mod.internal.probe.InspectorOverlayMode;
import com.terraforged.mod.internal.probe.TfOverlayColumn;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class InspectorWorldOverlay {
    private static final int MAX_CAVE_FILLS = 4800;
    private static final float CAVE_FILL_ALPHA = 0.18f;
    private static final float SURFACE_FILL_ALPHA = 0.42f;
    private static final float HOVER_FILL_ALPHA = 0.35f;

    private InspectorWorldOverlay() {
    }

    static void render(PoseStack poseStack, Vec3 cameraPos, InspectorOverlayMode mode, List<TfOverlayColumn> columns, String hoveredLabel, BlockPos pickPos, BlockPos zoneA, BlockPos zoneB) {
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        if (mode == InspectorOverlayMode.BIOMES) {
            List<TfOverlayColumn> cave = InspectorOverlayMerger.mergeCaveVolumes(columns);
            InspectorWorldOverlay.renderCaveFill(poseStack, cave, CAVE_FILL_ALPHA);
            InspectorWorldOverlay.renderSurfaceFill(poseStack, columns);
            if (hoveredLabel != null && !hoveredLabel.isEmpty()) {
                ArrayList<TfOverlayColumn> match = new ArrayList<>();
                for (TfOverlayColumn column : columns) {
                    if (hoveredLabel.equals(column.label())) {
                        match.add(column);
                    }
                }
                List<TfOverlayColumn> hoverCave = InspectorOverlayMerger.mergeCaveVolumes(match);
                InspectorWorldOverlay.renderCaveFill(poseStack, hoverCave, HOVER_FILL_ALPHA);
                InspectorWorldOverlay.renderSurfaceFill(poseStack, match);
            }
        } else {
            InspectorWorldOverlay.renderCaveFill(poseStack, columns, 0.28f);
        }
        MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        if (pickPos != null) {
            AABB pick = new AABB(pickPos).inflate(0.02);
            LevelRenderer.renderLineBox(poseStack, lines, pick, 1.0f, 1.0f, 0.2f, 1.0f);
        }
        if (zoneA != null && zoneB != null) {
            int minX = Math.min(zoneA.getX(), zoneB.getX());
            int maxX = Math.max(zoneA.getX(), zoneB.getX());
            int minZ = Math.min(zoneA.getZ(), zoneB.getZ());
            int maxZ = Math.max(zoneA.getZ(), zoneB.getZ());
            int y = zoneA.getY();
            AABB zone = new AABB(minX, y, minZ, maxX + 1, y + 0.05, maxZ + 1);
            LevelRenderer.renderLineBox(poseStack, lines, zone, 1.0f, 0.55f, 0.1f, 1.0f);
        }
        buffers.endBatch();
        poseStack.popPose();
    }

    private static void renderCaveFill(PoseStack poseStack, List<TfOverlayColumn> cave, float alpha) {
        if (cave.isEmpty()) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int drawn = 0;
        for (TfOverlayColumn column : cave) {
            if (drawn++ >= MAX_CAVE_FILLS) {
                break;
            }
            float r = ((column.rgb() >> 16) & 0xFF) / 255.0f;
            float g = ((column.rgb() >> 8) & 0xFF) / 255.0f;
            float b = (column.rgb() & 0xFF) / 255.0f;
            float x1 = column.x();
            float y1 = column.yMin();
            float z1 = column.z();
            float x2 = column.xEnd();
            float y2 = column.yMax() + 1.0f;
            float z2 = column.zEnd();
            InspectorWorldOverlay.quad(buffer, matrix, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, r, g, b, alpha);
            InspectorWorldOverlay.quad(buffer, matrix, x2, y1, z2, x1, y1, z2, x1, y2, z2, x2, y2, z2, r, g, b, alpha);
            InspectorWorldOverlay.quad(buffer, matrix, x1, y1, z2, x1, y1, z1, x1, y2, z1, x1, y2, z2, r, g, b, alpha);
            InspectorWorldOverlay.quad(buffer, matrix, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, r, g, b, alpha);
            InspectorWorldOverlay.quad(buffer, matrix, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, r, g, b, alpha);
            InspectorWorldOverlay.quad(buffer, matrix, x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1, r, g, b, alpha);
        }
        tesselator.end();
        RenderSystem.depthMask(true);
    }

    private static void renderSurfaceFill(PoseStack poseStack, List<TfOverlayColumn> columns) {
        ArrayList<TfOverlayColumn> surface = new ArrayList<>();
        for (TfOverlayColumn column : columns) {
            if (column.surface()) {
                surface.add(column);
            }
        }
        if (surface.isEmpty()) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (TfOverlayColumn column : surface) {
            float r = ((column.rgb() >> 16) & 0xFF) / 255.0f;
            float g = ((column.rgb() >> 8) & 0xFF) / 255.0f;
            float b = (column.rgb() & 0xFF) / 255.0f;
            float x1 = column.x();
            float y = column.yMin() + 1.01f;
            float z1 = column.z();
            float x2 = column.xEnd();
            float z2 = column.zEnd();
            InspectorWorldOverlay.quad(buffer, matrix, x1, y, z1, x2, y, z1, x2, y, z2, x1, y, z2, r, g, b, SURFACE_FILL_ALPHA);
        }
        tesselator.end();
        RenderSystem.depthMask(true);
    }

    private static void quad(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x3, y3, z3).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x4, y4, z4).color(r, g, b, a).endVertex();
    }
}
