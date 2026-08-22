package com.particle_life.app;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;

class GuiAboutDialog {

    static void draw(GuiContext ctx) {
        if (!ctx.showAboutWindow.get()) return;

        ImGui.setNextWindowPos(ctx.width / 2f, ctx.height / 2f, imgui.flag.ImGuiCond.FirstUseEver, 0.5f, 0.5f);
        if (ImGui.begin("About", ctx.showAboutWindow, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse)) {
            ImGui.text("Particle Life App " + ctx.appVersion);
            ImGui.text("By Tom Mohr.");
            ImGui.text("GPL-3.0 License.");
            ImGui.dummy(0, 10);
            if (com.particle_life.app.utils.ImGuiUtils.link("particle-life.com", "https://particle-life.com")) {
                ctx.setFullscreen.accept(false);
            }
            ImGui.dummy(0, 10);
            ImGui.text("Java Home: " + ctx.javaHome);
            ImGui.text("JVM Version: " + ctx.jvmVersion);
            ImGui.text("LWJGL Version: " + ctx.lwjglVersion);
            ImGui.text("OpenGL Vendor: " + ctx.openGlVendor);
            ImGui.text("OpenGL Renderer: " + ctx.openGlRenderer);
            ImGui.text("OpenGL Version: " + ctx.openGlVersion);
            ImGui.text("OpenGL Profile: " + ctx.openGlProfile);
            ImGui.text("GLSL Version: " + ctx.glslVersion);
        }
        ImGui.end();
    }
}
