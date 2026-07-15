/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.render;

import com.terraforged.engine.render.RenderBuffer;

public interface RenderAPI {
    public void pushMatrix();

    public void popMatrix();

    public void translate(float var1, float var2, float var3);

    public void rotateX(float var1);

    public void rotateY(float var1);

    public void rotateZ(float var1);

    public RenderBuffer createBuffer();
}

