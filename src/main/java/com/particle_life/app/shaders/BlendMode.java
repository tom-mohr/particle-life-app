package com.particle_life.app.shaders;

import org.lwjgl.opengl.GL11C;

import static org.lwjgl.opengl.GL11C.*;

public enum BlendMode {
    normal(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA),
    add(GL_SRC_ALPHA, GL_ONE),
    subtract(GL_ONE_MINUS_DST_COLOR, GL_ZERO),
    screen(GL_ONE, GL_ONE_MINUS_SRC_COLOR);

    private final int src;
    private final int dst;

    BlendMode(int src, int dst) {
        this.src = src;
        this.dst = dst;
    }

    public void glBlendFunc() {
        GL11C.glBlendFunc(src, dst);
    }
}
