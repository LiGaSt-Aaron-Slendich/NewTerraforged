package com.terraforged.mod.internal.probe.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.internal.probe.TfProbeResult;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

final class InspectorDetailPanel {
    private static final int LINE_HEIGHT = 9;
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 300;
    private static final int PADDING = 6;
    private static final int HEADER_HEIGHT = 14;

    private List<String> lines = List.of();
    private int scroll;
    private int pickX;
    private int pickY;
    private int pickZ;

    private InspectorDetailPanel() {
    }

    static final InspectorDetailPanel INSTANCE = new InspectorDetailPanel();

    boolean isOpen() {
        return !this.lines.isEmpty();
    }

    void show(TfProbeResult result) {
        this.lines = result.lines();
        this.pickX = result.x();
        this.pickY = result.y();
        this.pickZ = result.z();
        this.scroll = 0;
    }

    void clear() {
        this.lines = List.of();
        this.scroll = 0;
    }

    void scroll(double delta) {
        int contentHeight = this.lines.size() * LINE_HEIGHT;
        int viewHeight = PANEL_HEIGHT - HEADER_HEIGHT - 8;
        int max = Math.max(0, contentHeight - viewHeight);
        this.scroll = (int)Math.max(0, Math.min(max, this.scroll - delta * LINE_HEIGHT * 2.0));
    }

    boolean isMouseOver(Minecraft mc) {
        if (!this.isOpen()) {
            return false;
        }
        double[] mouse = InspectorDetailPanel.guiMouse(mc);
        int left = this.panelLeft(mc.getWindow().getGuiScaledWidth());
        int top = 8;
        return mouse[0] >= left && mouse[0] <= left + PANEL_WIDTH && mouse[1] >= top && mouse[1] <= top + PANEL_HEIGHT;
    }

    boolean tryCloseAt(Minecraft mc) {
        if (!this.isOpen()) {
            return false;
        }
        double[] mouse = InspectorDetailPanel.guiMouse(mc);
        if (this.closeButtonHit((int)mouse[0], (int)mouse[1], mc.getWindow().getGuiScaledWidth())) {
            this.clear();
            return true;
        }
        return false;
    }

    void render(PoseStack poseStack, int screenWidth, int screenHeight) {
        if (this.lines.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int left = this.panelLeft(screenWidth);
        int top = 8;
        int bottom = top + PANEL_HEIGHT;
        ScreenFill.fill(poseStack, left, top, left + PANEL_WIDTH, bottom, 0xDD101010);
        ScreenFill.fill(poseStack, left, top, left + PANEL_WIDTH, top + HEADER_HEIGHT, 0xEE204020);
        font.draw(poseStack, String.format("Probe %d %d %d", this.pickX, this.pickY, this.pickZ), left + PADDING, top + 3, 0xAAFFAA);
        int closeLeft = left + PANEL_WIDTH - 16;
        ScreenFill.fill(poseStack, closeLeft, top + 2, left + PANEL_WIDTH - 4, top + HEADER_HEIGHT - 2, 0xAA552222);
        font.draw(poseStack, "X", closeLeft + 4, top + 3, 0xFFFF8888);
        int contentTop = top + HEADER_HEIGHT + 2;
        int contentBottom = bottom - 4;
        ScreenFill.fill(poseStack, left + 2, contentTop, left + PANEL_WIDTH - 2, contentBottom, 0xAA080808);
        int y = contentTop + 2 - this.scroll;
        for (String line : this.lines) {
            if (y + LINE_HEIGHT > contentTop && y < contentBottom) {
                font.draw(poseStack, line, left + PADDING, y, 0xDDDDDD);
            }
            y += LINE_HEIGHT;
        }
        int contentHeight = this.lines.size() * LINE_HEIGHT;
        int viewHeight = contentBottom - contentTop;
        if (contentHeight > viewHeight) {
            int barHeight = Math.max(12, viewHeight * viewHeight / contentHeight);
            int barTop = contentTop + (int)((long)(contentBottom - contentTop - barHeight) * this.scroll / Math.max(1, contentHeight - viewHeight));
            ScreenFill.fill(poseStack, left + PANEL_WIDTH - 5, barTop, left + PANEL_WIDTH - 2, barTop + barHeight, 0xFF668866);
        }
        font.draw(poseStack, "Wheel over panel to scroll", left + PADDING, bottom - 10, 0x888888);
    }

    private int panelLeft(int screenWidth) {
        return screenWidth - PANEL_WIDTH - 8;
    }

    private boolean closeButtonHit(int mouseX, int mouseY, int screenWidth) {
        int left = this.panelLeft(screenWidth);
        int top = 8;
        int closeLeft = left + PANEL_WIDTH - 16;
        int closeRight = left + PANEL_WIDTH - 4;
        return mouseX >= closeLeft && mouseX <= closeRight && mouseY >= top && mouseY <= top + HEADER_HEIGHT;
    }

    private static double[] guiMouse(Minecraft mc) {
        double[] raw = InspectorInputHelper.cursorPos(mc);
        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();
        double scaleX = (double)guiW / (double)Math.max(1, mc.getWindow().getScreenWidth());
        double scaleY = (double)guiH / (double)Math.max(1, mc.getWindow().getScreenHeight());
        return new double[]{raw[0] * scaleX, raw[1] * scaleY};
    }
}
