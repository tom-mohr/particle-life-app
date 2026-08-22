package com.particle_life.app;

import com.particle_life.app.cursors.CursorAction;
import com.particle_life.app.utils.ImGuiUtils;
import imgui.ImGui;
import imgui.flag.ImGuiDir;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;

class GuiCursorPanel {

    static void draw(GuiContext ctx) {
        ImGui.setNextWindowSize(290, 250, imgui.flag.ImGuiCond.FirstUseEver);
        ImGui.setNextWindowPos(0, ctx.height, imgui.flag.ImGuiCond.Always, 0.0f, 1.0f);
        ImGui.getStyle().setWindowMenuButtonPosition(ImGuiDir.Left);
        if (ImGui.begin("Cursor",
                ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoNavFocus | ImGuiWindowFlags.NoMove)) {
            ImGui.pushItemWidth(200);

            ImGui.text("Hovered Particles: " + ctx.cursorParticleCount);
            if (ImGui.checkbox("Show", ctx.appSettings.showCursor)) {
                ctx.appSettings.showCursor ^= true;
            }
            ImGuiUtils.numberInput("Size",
                    0.001f, 1f,
                    (float) ctx.cursor.size,
                    "%.3f",
                    value -> ctx.cursor.size = value);
            ImGuiUtils.helpMarker("[ctrl+scroll]");

            ImGuiUtils.renderCombo("Shape##cursor", ctx.cursorShapes);
            ctx.cursor.shape = ctx.cursorShapes.getActive();

            ImGuiUtils.separator();

            if (ImGui.beginTable("Cursor Action Table", 2, ImGuiTableFlags.None)) {
                ImGui.tableSetupColumn("", ImGuiTableColumnFlags.WidthFixed, 100);
                ImGui.tableSetupColumn("", ImGuiTableColumnFlags.WidthFixed, 100);

                ImGui.tableNextRow();
                ImGui.tableSetColumnIndex(0);
                ImGui.text("Left");
                ImGui.tableSetColumnIndex(1);
                ImGui.text("Right");

                ImGui.tableNextRow();
                ImGui.tableSetColumnIndex(0);
                ImGui.pushItemWidth(100);
                ImGuiUtils.renderCombo("##cursoraction1", ctx.cursorActions1);
                ImGui.popItemWidth();
                ImGui.tableSetColumnIndex(1);
                ImGui.pushItemWidth(100);
                ImGuiUtils.renderCombo("##cursoraction2", ctx.cursorActions2);
                ImGui.popItemWidth();

                ImGui.tableNextRow();
                ImGui.endTable();
            }

            ImGui.indent();
            if (ctx.cursorActions1.getActive() == CursorAction.BRUSH || ctx.cursorActions2.getActive() == CursorAction.BRUSH) {
                ImInt inputValue = new ImInt(ctx.appSettings.brushPower);
                ImGui.pushItemWidth(100);
                if (ImGui.inputInt("Brush Power", inputValue, 10, ImGuiInputTextFlags.EnterReturnsTrue)) {
                    ctx.appSettings.brushPower = Math.max(0, inputValue.get());
                }
                ImGui.popItemWidth();
                ImGuiUtils.helpMarker("Number of particles added per frame.");
            }
            ImGui.unindent();

            ImGui.popItemWidth();
        }
        ImGui.end();
    }
}
