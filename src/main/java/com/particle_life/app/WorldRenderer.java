package com.particle_life.app;

import com.particle_life.app.color.Palette;
import com.particle_life.app.cursors.Cursor;
import com.particle_life.app.shaders.CursorShader;
import com.particle_life.app.shaders.ParticleShader;
import com.particle_life.app.utils.CamOperations;
import com.particle_life.app.utils.MultisampledFramebuffer;
import com.particle_life.app.utils.NormalizedDeviceCoordinates;
import com.particle_life.backend.PhysicsSettings;
import org.joml.Matrix4d;
import org.joml.Vector2d;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.*;

class WorldRenderer {

    record RenderResult(int texWidth, int texHeight, CamOperations.BoundingBox camBox) {
    }

    private final Matrix4d transform = new Matrix4d();

    RenderResult render(
            MultisampledFramebuffer worldTexture,
            MultisampledFramebuffer cursorTexture,
            ParticleRenderer particleRenderer,
            ParticleShader particleShader,
            CursorShader cursorShader,
            Cursor cursor,
            AppSettings appSettings,
            PhysicsSettings settings,
            Palette palette,
            Vector2d camPos,
            double camSize,
            int width,
            int height,
            boolean traces
    ) {
        int texWidth;
        int texHeight;

        int desiredTexSize = (int) Math.round(Math.min(width, height) / camSize);
        if (camSize > 1) {
            texWidth = desiredTexSize;
            texHeight = desiredTexSize;
            new NormalizedDeviceCoordinates(
                    new Vector2d(0.5, 0.5),
                    new Vector2d(1, 1)
            ).getMatrix(transform);
        } else {
            if (settings.wrap) {
                texWidth = Math.min(desiredTexSize, width);
                texHeight = Math.min(desiredTexSize, height);
            } else {
                texWidth = width;
                texHeight = height;
            }
            Vector2d texCamSize = new Vector2d(camSize);
            if (width > height) texCamSize.x *= (double) texWidth / texHeight;
            else if (height > width) texCamSize.y *= (double) texHeight / texWidth;
            new NormalizedDeviceCoordinates(
                    new Vector2d(texCamSize.x / 2, texCamSize.y / 2),
                    texCamSize
            ).getMatrix(transform);
        }

        worldTexture.ensureSize(texWidth, texHeight, 16);

        particleShader.use();
        particleShader.setTime(System.nanoTime() / 1000_000_000.0f);
        particleShader.setPalette(PaletteUtils.getColors(settings.matrix.size(), palette));
        particleShader.setTransform(transform);

        CamOperations cam = new CamOperations(camPos, camSize, width, height);
        CamOperations.BoundingBox camBox = cam.getBoundingBox();
        if (camSize > 1) {
            particleShader.setCamTopLeft(0, 0);
        } else {
            particleShader.setCamTopLeft((float) camBox.left, (float) camBox.top);
        }
        particleShader.setWrap(settings.wrap);
        particleShader.setSize(appSettings.particleSize * 2 * (float) settings.rmax
                * (appSettings.keepParticleSizeIndependentOfZoom ? (float) camSize : 1));

        if (!traces) worldTexture.clear(0, 0, 0, 0);

        glEnable(GL_BLEND);
        particleShader.blendMode.glBlendFunc();

        glDisable(GL_SCISSOR_TEST);
        glViewport(0, 0, texWidth, texHeight);

        glBindFramebuffer(GL_FRAMEBUFFER, worldTexture.framebufferMulti);
        particleRenderer.drawParticles();
        worldTexture.toSingleSampled();

        glBindTexture(GL_TEXTURE_2D, worldTexture.textureSingle);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, settings.wrap ? GL_REPEAT : GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, settings.wrap ? GL_REPEAT : GL_CLAMP_TO_BORDER);
        glBindTexture(GL_TEXTURE_2D, 0);

        cursorTexture.ensureSize(width, height, 16);
        cursorTexture.clear(0, 0, 0, 0);
        if (appSettings.showCursor) {
            new NormalizedDeviceCoordinates(camPos, cam.getCamDimensions()).getMatrix(transform);
            transform.translate(cursor.position);
            transform.scale(cursor.size);

            glViewport(0, 0, width, height);
            glBindFramebuffer(GL_FRAMEBUFFER, cursorTexture.framebufferMulti);

            cursorShader.use();
            cursorShader.setTransform(transform);
            cursor.draw();
        }
        cursorTexture.toSingleSampled();

        return new RenderResult(texWidth, texHeight, camBox);
    }

    static void drawBackgroundImages(
            MultisampledFramebuffer worldTexture,
            MultisampledFramebuffer cursorTexture,
            RenderResult result,
            double camSize,
            int width,
            int height
    ) {
        if (camSize > 1) {
            imgui.ImGui.getBackgroundDrawList().addImage(worldTexture.textureSingle, 0, 0, width, height,
                    (float) result.camBox().left, (float) result.camBox().top,
                    (float) result.camBox().right, (float) result.camBox().bottom);
        } else {
            imgui.ImGui.getBackgroundDrawList().addImage(worldTexture.textureSingle, 0, 0, width, height,
                    0, 0, (float) width / result.texWidth(), (float) height / result.texHeight());
        }
        imgui.ImGui.getBackgroundDrawList().addImage(cursorTexture.textureSingle, 0, 0, width, height,
                0, 0, 1, 1);
    }
}
