package com.particle_life.app;

import com.particle_life.app.cursors.Cursor;
import com.particle_life.app.shaders.ParticleShader;
import imgui.ImDrawData;
import imgui.gl3.ImGuiImplGl3;
import org.joml.Matrix4d;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

class Renderer {

    private int vao;
    private int vboX;
    private int vboV;
    private int vboT;

    public ParticleShader particleShader = null;
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    /**
     * Remember the last buffered size in order to use subBufferData instead of bufferData whenever possible.
     */
    private int lastBufferedSize = -1;
    private int lastShaderProgram = -1;

    void init() {
        // Method initializes LWJGL3 renderer.
        // This method SHOULD be called after you've initialized your ImGui configuration (fonts and so on).
        // ImGui context should be created as well.
        imGuiGl3.init("#version 410 core");

        vao = glGenVertexArrays();
        vboX = glGenBuffers();
        vboV = glGenBuffers();
        vboT = glGenBuffers();
    }

    void bufferParticleData(double[] x, double[] v, int[] types) {

        glBindVertexArray(vao);

        // detect change
        boolean shaderChanged = particleShader.shaderProgram != lastShaderProgram;
        boolean bufferSizeChanged = types.length != lastBufferedSize;
        lastBufferedSize = types.length;
        lastShaderProgram = particleShader.shaderProgram;

        if (shaderChanged) {

            // enable vertex attributes

            if (particleShader.xAttribLocation != -1) {
                glBindBuffer(GL_ARRAY_BUFFER, vboX);
                glVertexAttribPointer(particleShader.xAttribLocation, 3, GL_DOUBLE, false, 0, 0);
                glEnableVertexAttribArray(particleShader.xAttribLocation);
            }
            if (particleShader.vAttribLocation != -1) {
                glBindBuffer(GL_ARRAY_BUFFER, vboV);
                glVertexAttribPointer(particleShader.vAttribLocation, 3, GL_DOUBLE, false, 0, 0);
                glEnableVertexAttribArray(particleShader.vAttribLocation);
            }
            if (particleShader.typeAttribLocation != -1) {
                glBindBuffer(GL_ARRAY_BUFFER, vboT);
                glVertexAttribIPointer(particleShader.typeAttribLocation, 1, GL_INT, 0, 0);
                glEnableVertexAttribArray(particleShader.typeAttribLocation);
            }
        }

        final int usage = GL_DYNAMIC_DRAW;  // for convenience

        if (particleShader.xAttribLocation != -1) {
            glBindBuffer(GL_ARRAY_BUFFER, vboX);

            if (bufferSizeChanged || shaderChanged) {
                glBufferData(GL_ARRAY_BUFFER, x, usage);
            } else {
                glBufferSubData(GL_ARRAY_BUFFER, 0, x);
            }
        }

        if (particleShader.vAttribLocation != -1) {
            glBindBuffer(GL_ARRAY_BUFFER, vboV);

            if (bufferSizeChanged || shaderChanged) {
                glBufferData(GL_ARRAY_BUFFER, v, usage);
            } else {
                glBufferSubData(GL_ARRAY_BUFFER, 0, v);
            }
        }

        if (particleShader.typeAttribLocation != -1) {
            glBindBuffer(GL_ARRAY_BUFFER, vboT);

            if (bufferSizeChanged || shaderChanged) {
                glBufferData(GL_ARRAY_BUFFER, types, usage);
            } else {
                glBufferSubData(GL_ARRAY_BUFFER, 0, types);
            }
        }
    }

    void clear() {
        // The OpenGL Specification states that glClear() only clears the scissor rectangle when the scissor test is enabled.
        glDisable(GL_SCISSOR_TEST);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
    }

    void run(Matrix4d transform, boolean drawCursor, Cursor cursor, ImDrawData imDrawData, int width, int height) {

        // draw particles
        if (particleShader != null && lastBufferedSize > 0) {
            glDisable(GL_SCISSOR_TEST);
            glViewport(0, 0, width, height);
            particleShader.use();
            glBindVertexArray(vao);
            glDrawArrays(GL_POINTS, 0, lastBufferedSize);
        }

        if (drawCursor) cursor.draw(transform);

        imGuiGl3.render(imDrawData);  // will change shader and vao
    }

    void dispose() {
        imGuiGl3.dispose();
    }
}
