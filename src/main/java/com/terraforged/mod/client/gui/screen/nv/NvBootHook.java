package com.terraforged.mod.client.gui.screen.nv;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/** Title-screen key probe (U→I→0→1). */
public final class NvBootHook {
    private static final long STEP_TIMEOUT_MS = 2500L;
    private static final int[] SEQUENCE = {
            GLFW.GLFW_KEY_U,
            GLFW.GLFW_KEY_I,
            GLFW.GLFW_KEY_0,
            GLFW.GLFW_KEY_1
    };

    private static int step;
    private static long lastStepMs;

    private NvBootHook() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(NvBootHook.class);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.KeyInputEvent event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        Screen screen = mc.screen;
        if (!(screen instanceof TitleScreen)) {
            step = 0;
            return;
        }
        if (screen instanceof NvCmdOverlay || screen instanceof NvFlagPanel) {
            return;
        }

        long now = System.currentTimeMillis();
        if (step > 0 && now - lastStepMs > STEP_TIMEOUT_MS) {
            step = 0;
        }

        int key = event.getKey();
        if (key == SEQUENCE[step]) {
            step++;
            lastStepMs = now;
            if (step >= SEQUENCE.length) {
                step = 0;
                mc.setScreen(new NvCmdOverlay(screen));
            }
            return;
        }
        if (key == SEQUENCE[0]) {
            step = 1;
            lastStepMs = now;
        } else {
            step = 0;
        }
    }
}
