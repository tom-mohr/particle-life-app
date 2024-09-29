package com.particle_life.app.utils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glGenTextures;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30C.glGenFramebuffers;
import static org.lwjgl.opengl.GL32.GL_TEXTURE_2D_MULTISAMPLE;
import static org.lwjgl.opengl.GL32.glTexImage2DMultisample;

/**
 * This is a utility class for creating a multisampled framebuffer
 * that can be converted to a single-sampled framebuffer.
 */
public class MultisampledFramebuffer {

    public int framebufferMulti;
    public int textureMulti;

    public int framebufferSingle;
    public int textureSingle;

    public int width = -1;
    public int height = -1;

    public void init() {
        framebufferMulti = glGenFramebuffers();
        textureMulti = glGenTextures();

        framebufferSingle = glGenFramebuffers();
        textureSingle = glGenTextures();
    }

    public void clear(float red, float green, float blue, float alpha) {
        int previousFramebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING);

        glDisable(GL_SCISSOR_TEST);
        glClearColor(red, green, blue, alpha);

        glBindFramebuffer(GL_FRAMEBUFFER, framebufferMulti);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glBindFramebuffer(GL_FRAMEBUFFER, previousFramebuffer);
    }

    public void toSingleSampled() {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, framebufferMulti);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebufferSingle);
        glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
    }

    public void ensureSize(int width, int height) {
        if (this.width != width || this.height != height) {

            glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, textureMulti);
            glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, 16, GL_RGBA, width, height, true);
            glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, 0);

            glBindTexture(GL_TEXTURE_2D, textureSingle);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
            glBindTexture(GL_TEXTURE_2D, 0);

            glBindFramebuffer(GL_FRAMEBUFFER, framebufferMulti);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D_MULTISAMPLE, textureMulti, 0);

            glBindFramebuffer(GL_FRAMEBUFFER, framebufferSingle);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureSingle, 0);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);

            this.width = width;
            this.height = height;
        }
    }

    public void delete() {
        glDeleteFramebuffers(framebufferMulti);
        glDeleteTextures(textureMulti);

        glDeleteFramebuffers(framebufferSingle);
        glDeleteTextures(textureSingle);
    }
}
