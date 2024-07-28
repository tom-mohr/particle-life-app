package com.particle_life.app.utils;

import com.particle_life.app.selection.SelectionManager;
import imgui.ImGui;
import imgui.flag.ImGuiSliderFlags;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public final class ImGuiUtils {

    /**
     * Helper to display a little (?) mark which shows a tooltip when hovered.
     *
     * @param text will be displayed as tooltip when hovered
     */
    public static void helpMarker(String text) {
        helpMarker(" ? ", text);
    }

    /**
     * Helper to display disabled text which shows a tooltip when hovered.
     *
     * @param label will always be displayed
     * @param text  will be displayed as tooltip when hovered
     */
    public static void helpMarker(String label, String text) {
        ImGui.sameLine();
        ImGui.textDisabled(label);
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.pushTextWrapPos(ImGui.getFontSize() * 35.0f);
            ImGui.textUnformatted(text);
            ImGui.popTextWrapPos();
            ImGui.endTooltip();
        }
    }

    public static boolean link(String label, String url) {
        if (ImGui.button(label)) {
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(new URI(url));
                    return true;
                } catch (IOException | URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * @return if the selection index changed
     */
    public static boolean renderCombo(String label, SelectionManager<?> selectionManager) {
        int previousIndex = selectionManager.getActiveIndex();
        if (ImGui.beginCombo(label, selectionManager.getActiveName())) {
            for (int i = 0; i < selectionManager.size(); i++) {
                boolean isSelected = selectionManager.getActiveIndex() == i;
                if (ImGui.selectable(selectionManager.get(i).name, isSelected)) {
                    selectionManager.setActive(i);
                }
                if (isSelected) {
                    ImGui.setItemDefaultFocus();
                }
                if (ImGui.isItemHovered()) {
                    String description = selectionManager.get(i).description;
                    if (!description.isBlank()) {
                        ImGui.beginTooltip();
                        ImGui.textUnformatted(description);
                        ImGui.endTooltip();
                    }
                }
            }
            ImGui.endCombo();
        }
        return selectionManager.getActiveIndex() != previousIndex;
    }

    public interface NumberInputCallback {
        void onValueChanged(float value);
    }

    public static void numberInput(String label,
                                   float min, float max,
                                   float value,
                                   String format,
                                   NumberInputCallback callback) {
        float[] valueBuffer = new float[]{value};
        if (ImGui.sliderFloat(label, valueBuffer, min, max, format,
                ImGuiSliderFlags.Logarithmic | ImGuiSliderFlags.NoRoundToFormat)) {
            callback.onValueChanged(valueBuffer[0]);
        }
    }

    public static void separator() {
        ImGui.dummy(0, 2);
        ImGui.separator();
        ImGui.dummy(0, 2);
    }
}
