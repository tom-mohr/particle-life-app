package com.particle_life.app;

import com.particle_life.app.color.NaturalRainbowPalette;
import com.particle_life.app.shaders.ParticleShader;
import com.particle_life.app.utils.MultisampledFramebuffer;
import com.particle_life.app.utils.NormalizedDeviceCoordinates;
import com.particle_life.backend.PhysicsSettings;
import org.joml.Vector2d;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.*;

class SaveThumbnailRenderer {

    static final int SAVE_IMAGE_SIZE = 256;

    static int[] render(
            ParticleRenderer particleRenderer,
            ParticleShader particleShader,
            PhysicsSettings settings
    ) {
        glEnable(GL_BLEND);
        particleShader.blendMode.glBlendFunc();

        particleShader.use();
        particleShader.setTime(0);
        particleShader.setPalette(PaletteUtils.getColors(
                settings.matrix.size(),
                new NaturalRainbowPalette()));
        particleShader.setTransform(new NormalizedDeviceCoordinates(
                new Vector2d(0.5, 0.5),
                new Vector2d(1, 1)
        ).getMatrix());
        particleShader.setSize(0.015f);
        particleShader.setCamTopLeft(0, 0);
        particleShader.setWrap(false);

        int[] pixels = new int[SAVE_IMAGE_SIZE * SAVE_IMAGE_SIZE];
        MultisampledFramebuffer tex = new MultisampledFramebuffer();
        tex.init();
        tex.ensureSize(SAVE_IMAGE_SIZE, SAVE_IMAGE_SIZE, 16);
        tex.clear(0, 0, 0, 0);
        glViewport(0, 0, SAVE_IMAGE_SIZE, SAVE_IMAGE_SIZE);
        glBindFramebuffer(GL_FRAMEBUFFER, tex.framebufferMulti);
        particleRenderer.drawParticles();
        tex.toSingleSampled();
        glBindFramebuffer(GL_FRAMEBUFFER, tex.framebufferSingle);
        glReadPixels(0, 0, SAVE_IMAGE_SIZE, SAVE_IMAGE_SIZE, GL_BGRA, GL_UNSIGNED_BYTE, pixels);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        tex.delete();

        return pixels;
    }
}
