package com.particle_life.app;

import com.particle_life.app.utils.ImGuiUtils;
import imgui.ImGui;
import imgui.flag.ImGuiSliderFlags;
import imgui.flag.ImGuiWindowFlags;

class GuiGraphicsPanel {

    static void draw(GuiContext ctx) {
        if (!ctx.showGraphicsWindow.get()) return;

        ImGui.setNextWindowSize(400, 300);
        ImGui.setNextWindowPos(ctx.width / 2f, ctx.height / 2f, imgui.flag.ImGuiCond.FirstUseEver, 0.5f, 0.5f);
        if (ImGui.begin("Graphics", ctx.showGraphicsWindow,
                ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoNavFocus | ImGuiWindowFlags.NoCollapse)) {
            ImGui.pushItemWidth(200);
            ImGui.text(String.format("Graphics FPS: %.0f", ctx.renderClock.getAvgFramerate()));

            ImGuiUtils.renderCombo("Shader", ctx.shaders);
            ImGuiUtils.helpMarker("Use this to set how the particles are displayed");

            ImGuiUtils.renderCombo("Palette", ctx.palettes);
            ImGuiUtils.helpMarker("Color of particles");

            ImGui.text("Particle Size:");
            ImGuiUtils.helpMarker("[shift+scroll]" +
                    "\nHow large the particles are displayed relative to rmax.");
            float[] particleSizeSliderValue = new float[]{ctx.appSettings.particleSize};
            if (ImGui.sliderFloat("##particle size", particleSizeSliderValue, 0.001f, 1f)) {
                ctx.appSettings.particleSize = particleSizeSliderValue[0];
            }
            ImGui.sameLine();
            if (ImGui.checkbox("Zoom-Independent", ctx.appSettings.keepParticleSizeIndependentOfZoom)) {
                ctx.appSettings.keepParticleSizeIndependentOfZoom ^= true;
            }

            if (ImGui.checkbox("Traces [t]", ctx.traces)) {
                InputShortcuts.toggleTraces(ctx);
            }

            if (ImGui.treeNode("Camera Settings")) {
                float[] camSpeed = new float[]{(float) ctx.appSettings.camMovementSpeed};
                if (ImGui.sliderFloat("Cam Speed", camSpeed, 0.0f, 2.0f, "%0.2f")) {
                    ctx.appSettings.camMovementSpeed = camSpeed[0];
                }

                float[] camSmoothing = new float[]{(float) (1.0 - ctx.appSettings.zoomSmoothness)};
                if (ImGui.sliderFloat("Cam Smoothing", camSmoothing, 0.0f, 1.0f, "%0.2f")) {
                    ctx.appSettings.zoomSmoothness = 1.0 - camSmoothing[0];
                    ctx.appSettings.shiftSmoothness = 1.0 - camSmoothing[0];
                }

                float[] zoomStep = new float[]{(float) (ctx.appSettings.zoomStepFactor - 1) * 100};
                if (ImGui.sliderFloat("Zoom Step", zoomStep, 0.0f, 100.0f, "%.1f%%", ImGuiSliderFlags.Logarithmic)) {
                    ctx.appSettings.zoomStepFactor = 1 + zoomStep[0] * 0.01;
                }

                ImGui.treePop();
            }

            ImGui.popItemWidth();
        }
        ImGui.end();
    }
}
