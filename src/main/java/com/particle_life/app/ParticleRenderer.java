package com.particle_life.app;

import com.particle_life.app.shaders.ParticleShader;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30C.*;

class ParticleRenderer {

    private int vao;
    private int vboX;
    private int vboV;
    private int vboT;
    /**
     * Remember the last buffered size in order to use subBufferData instead of bufferData whenever possible.
     */
    private int lastBufferedSize = -1;
    private int lastShaderProgram = -1;

    void init() {
        vao = glGenVertexArrays();
        vboX = glGenBuffers();
        vboV = glGenBuffers();
        vboT = glGenBuffers();
    }

    void bufferParticleData(ParticleShader particleShader, double[] x, double[] v, int[] types) {

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

    void drawParticles() {
        if (lastBufferedSize <= 0) return;
        glBindVertexArray(vao);
        glDrawArrays(GL_POINTS, 0, lastBufferedSize);
    }
}
