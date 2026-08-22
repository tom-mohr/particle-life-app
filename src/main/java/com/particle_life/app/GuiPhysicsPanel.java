package com.particle_life.app;

import com.particle_life.app.utils.ImGuiUtils;
import imgui.ImGui;
import imgui.flag.ImGuiDir;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;

class GuiPhysicsPanel {

    static void draw(GuiContext ctx) {
        ImGui.setNextWindowSize(-1, -1, imgui.flag.ImGuiCond.FirstUseEver);
        ImGui.setNextWindowPos(ctx.width, ctx.height, imgui.flag.ImGuiCond.Always, 1.0f, 1.0f);
        ImGui.getStyle().setWindowMenuButtonPosition(ImGuiDir.Right);
        if (ImGui.begin("Physics",
                ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoNavFocus | ImGuiWindowFlags.NoMove)) {
            ImGui.pushItemWidth(200);

            if (ImGui.button(ctx.loop.pause ? "Play" : "Pause", 80, 0)) {
                InputShortcuts.togglePause(ctx);
            }
            ImGuiUtils.helpMarker("[SPACE] " +
                    "The physics simulation runs independently from the graphics in the background.");

            ImGui.sameLine();
            if (ctx.loop.getAvgFramerate() < 100000) {
                ImGui.text(String.format("FPS: %5.0f", ctx.loop.getAvgFramerate()));
            } else {
                ImGui.text("");
            }

            ImGuiUtils.numberInput("rmax",
                    0.005f, 1f,
                    (float) ctx.settings.rmax,
                    "%.3f",
                    value -> ctx.loop.enqueue(() -> ctx.physics.settings.rmax = value));
            ImGuiUtils.helpMarker("The distance at which particles interact.");

            ImGuiUtils.numberInput("Friction Coefficient",
                    0f, 1f,
                    (float) ctx.settings.friction,
                    "%.3f",
                    value -> ctx.loop.enqueue(() -> ctx.physics.settings.friction = value),
                    false);
            ImGuiUtils.helpMarker("The velocity of all particles is multiplied with this value" +
                    " in each update step to simulate friction (assuming 60 fps).");

            ImGuiUtils.numberInput("Force Scaling",
                    0f, 100f,
                    (float) ctx.settings.force,
                    "%.1f",
                    value -> ctx.loop.enqueue(() -> ctx.physics.settings.force = value));
            ImGuiUtils.helpMarker("Scales the forces between all particles with a constant factor.");

            ImGuiUtils.separator();

            if (ImGui.checkbox("Periodic Boundaries", ctx.settings.wrap)) {
                InputShortcuts.toggleWrap(ctx);
            }
            ImGuiUtils.helpMarker("[b] Determines if the space wraps around at the borders or not.");

            if (ctx.appSettings.autoDt) ImGui.beginDisabled();
            ImGuiUtils.numberInput(
                    "Time Step",
                    0, 100,
                    (float) ctx.appSettings.dt * 1000f,
                    "%.2f ms",
                    value -> ctx.appSettings.dt = Math.max(0, value / 1000));
            if (ctx.appSettings.autoDt) ImGui.endDisabled();
            ImGui.sameLine();
            if (ImGui.checkbox("Auto", ctx.appSettings.autoDt)) ctx.appSettings.autoDt ^= true;
            ImGuiUtils.helpMarker("[ctrl+shift+scroll] The time step of the physics computation." +
                    "\nIf 'Auto' is ticked, the time step will be chosen automatically based on the real passed time.");

            ImInt threadNumberInput = new ImInt(ctx.preferredNumberOfThreads);
            if (ImGui.inputInt("Threads", threadNumberInput, 1, 1, ImGuiInputTextFlags.EnterReturnsTrue)) {
                final int newThreadNumber = Math.max(1, threadNumberInput.get());
                ctx.loop.enqueue(() -> ctx.physics.preferredNumberOfThreads = newThreadNumber);
            }
            ImGuiUtils.helpMarker("The number of threads used by your processor for the physics computation." +
                    "\n(If you don't know what this means, just ignore it.)");

            ImGui.popItemWidth();
        }
        ImGui.end();
    }
}
