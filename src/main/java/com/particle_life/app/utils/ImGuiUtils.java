package com.particle_life.app.utils;

import imgui.ImGui;

public final class ImGuiUtils {

    /**
     * Helper to display a little (?) mark which shows a tooltip when hovered.
     *
     * @param text will be displayed as tooltip when hovered
     */
    public static void helpMarker(String text) {
        helpMarker("(?)", text);
    }

    /**
     * Helper to display disabled text which shows a tooltip when hovered.
     *
     * @param label will always be displayed
     * @param text  will be displayed as tooltip when hovered
     */
    public static void helpMarker(String label, String text) {
        ImGui.textDisabled(label);
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.pushTextWrapPos(ImGui.getFontSize() * 35.0f);
            ImGui.textUnformatted(text);
            ImGui.popTextWrapPos();
            ImGui.endTooltip();
        }
    }
}
