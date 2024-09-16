package com.particle_life.app;

import com.particle_life.app.shaders.ParticleShader;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

class ParticleRenderer {

    private int vao;
    private int vboX;
    private int vboV;
    private int vboT;

    public ParticleShader particleShader = null;
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

    void renderParticles(int viewportWidth, int viewportHeight) {
        if (particleShader == null || lastBufferedSize <= 0) return;

        glDisable(GL_SCISSOR_TEST);
        glViewport(0, 0, viewportWidth, viewportHeight);
        particleShader.use();
        glBindVertexArray(vao);
        glDrawArrays(GL_POINTS, 0, lastBufferedSize);
    }

    int[] renderParticlesToImage(int width, int height) {
        int[] pixels = new int[width * height];

        // create, bind
        int framebuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);

        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer is not complete");
        }
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
        renderParticles(width, height);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        // unbind, delete
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteFramebuffers(framebuffer);

        return pixels;
    }
}
