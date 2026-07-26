package com.terraforged.mod.internal.probe.client;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class InspectorCamera {
    private Vec3 position = Vec3.ZERO;
    private Vec3 focus = Vec3.ZERO;
    private float yaw;
    private float pitch;
    private double focusDistance = 10.0;

    public void initFromEye(Vec3 eye, float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        Vec3 look = this.lookVector();
        this.focus = eye.add(look.scale(8.0));
        this.position = eye;
        this.focusDistance = 8.0;
    }

    public Vec3 position() {
        return this.position;
    }

    public Vec3 focus() {
        return this.focus;
    }

    public float yaw() {
        return this.yaw;
    }

    public float pitch() {
        return this.pitch;
    }

    public Vec3 eyePosition() {
        return this.position;
    }

    public void setFocus(Vec3 focus) {
        this.focus = focus;
        this.focusDistance = this.position.distanceTo(focus);
    }

    public Vec3 lookVector() {
        float pitchRad = (float)Math.toRadians(this.pitch);
        float yawRad = (float)Math.toRadians(this.yaw);
        float cosPitch = Mth.cos(pitchRad);
        return new Vec3(-Mth.sin(yawRad) * cosPitch, -Mth.sin(pitchRad), Mth.cos(yawRad) * cosPitch);
    }

    public Vec3 horizontalForward() {
        float yawRad = (float)Math.toRadians(this.yaw);
        return new Vec3(-Mth.sin(yawRad), 0.0, Mth.cos(yawRad));
    }

    public Vec3 horizontalRight() {
        float yawRad = (float)Math.toRadians(this.yaw);
        return new Vec3(Mth.cos(yawRad), 0.0, Mth.sin(yawRad));
    }

    /** Blender-style orbit around focus (MMB). */
    public void orbit(double dx, double dy) {
        this.yaw = (float)((double)this.yaw + dx * 0.55);
        this.pitch = Mth.clamp(this.pitch - (float)(dy * 0.55), -89.0f, 89.0f);
        this.position = this.focus.subtract(this.lookVector().scale(this.focusDistance));
    }

    /** Shift+MMB pan. */
    public void pan(double dx, double dy) {
        double scale = Math.max(0.15, this.focusDistance) * 0.0045;
        Vec3 delta = this.horizontalRight().scale(dx * scale).add(0.0, -dy * scale, 0.0);
        this.focus = this.focus.add(delta);
        this.position = this.position.add(delta);
    }

    /** Scroll dolly toward focus. */
    public void dolly(double scrollDelta) {
        this.focusDistance = Mth.clamp(this.focusDistance - scrollDelta * 2.4, 2.0, 220.0);
        this.position = this.focus.subtract(this.lookVector().scale(this.focusDistance));
    }

    /** WASD fly — moves camera and focus together. */
    public void fly(double forward, double right, double up, double speed) {
        Vec3 delta = this.horizontalForward().scale(forward).add(this.horizontalRight().scale(-right)).add(0.0, up, 0.0);
        if (delta.lengthSqr() <= 1.0E-4) {
            return;
        }
        delta = delta.normalize().scale(speed);
        this.position = this.position.add(delta);
        this.focus = this.focus.add(delta);
        this.focusDistance = this.position.distanceTo(this.focus);
    }

    public void lookAt(Vec3 target) {
        Vec3 dir = target.subtract(this.position).normalize();
        this.pitch = (float)Math.toDegrees(-Math.asin(Mth.clamp(dir.y, -1.0, 1.0)));
        this.yaw = (float)Math.toDegrees(Math.atan2(-dir.x, dir.z));
        this.focus = target;
        this.focusDistance = this.position.distanceTo(target);
    }
}
