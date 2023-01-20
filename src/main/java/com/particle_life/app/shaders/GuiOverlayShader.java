package com.particle_life.app.shaders;

import org.joml.Matrix4d;

import static org.lwjgl.opengl.GL20.*;

public class GuiOverlayShader {

    private final int shaderProgram;
    private final int transformUniformLocation;
    private final float[] transform = new float[16];

    public GuiOverlayShader() {
        shaderProgram = ShaderUtil.makeShaderProgram("gui_shaders/gui.vert", null, "gui_shaders/gui.frag");
        transformUniformLocation = glGetUniformLocation(shaderProgram, "transform");
    }

    public void use() {
        glUseProgram(shaderProgram);
    }

    public void setTransform(Matrix4d transform) {
        glUniformMatrix4fv(transformUniformLocation, false, transform.get(this.transform));
    }
}
