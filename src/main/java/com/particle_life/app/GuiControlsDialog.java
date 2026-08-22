package com.particle_life.app;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;

class GuiControlsDialog {

    static void draw(GuiContext ctx) {
        if (!ctx.showControlsWindow.get()) return;

        ImGui.setNextWindowPos(ctx.width / 2f, ctx.height / 2f, imgui.flag.ImGuiCond.FirstUseEver, 0.5f, 0.5f);
        if (ImGui.begin("Controls", ctx.showControlsWindow, ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize)) {
            ImGui.text("""
                    [+], [=]: zoom in
                    [-]: zoom out
                    [z]: reset zoom
                    [Z]: reset zoom (fit window)
                    [ESCAPE]: hide / show GUI
                    [g]: show / hide graphics settings
                    [SPACE]: pause physics
                    [p]: set positions
                    [c]: set colors
                    [m]: set matrix
                    [b]: toggle boundaries (clamped / periodic)
                    [t]: toggle traces
                    [F11], [f]: toggle full screen
                    [ALT]+[F4], [q]: quit
                    """);
        }
        ImGui.end();
    }
}
