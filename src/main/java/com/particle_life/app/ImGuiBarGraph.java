package com.particle_life.app;

import com.particle_life.app.color.Color;
import com.particle_life.app.color.Palette;
import com.particle_life.app.utils.ImGuiUtils;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;

import java.util.Arrays;

class ImGuiBarGraph {

    interface SetCallback {
        /**
         * @param newCount is guaranteed to be >= 0.
         */
        void set(int type, int newCount);
    }

    /**
     * @return type that is hovered by mouse or -1
     */
    public static int draw(float w, float h, Palette palette, int stepSize, int[] typeCount, SetCallback setCallback, boolean percentage) {

        boolean hovering = false;
        int typeHovering = 0;

        ImDrawList drawList = ImGui.getWindowDrawList();

        ImVec2 cursorAbsolute = new ImVec2();
        ImGui.getCursorScreenPos(cursorAbsolute);

        ImVec2 cursorRelative = new ImVec2();
        ImGui.getCursorPos(cursorRelative);

        int nTypes = typeCount.length;
        float barHeight = h / nTypes;

        int total = Arrays.stream(typeCount).sum();
        int maxCount = Arrays.stream(typeCount).max().orElse(0);
        ImVec2 maxCountTextSize = new ImVec2();
        ImGui.calcTextSize(maxCountTextSize, formatText(maxCount, total, percentage));
        float spaceBeforeText = 2;
        float spaceAfterText = 2;
        boolean renderText = maxCountTextSize.y < barHeight;

        for (int type = 0; type < nTypes; type++) {
            float absX = cursorAbsolute.x;
            float absY = cursorAbsolute.y + type * barHeight;

            int count = typeCount[type];
            float fractionOfMax = maxCount != 0 ? count / (float) maxCount : 0;

            Color color = palette.getColor(type, nTypes);
            float barWidth;
            if (renderText) {
                float textSpace = maxCountTextSize.x + spaceBeforeText + spaceAfterText;
                barWidth = Math.max(0, (w - textSpace) * fractionOfMax);
            } else {
                barWidth = w * fractionOfMax;
            }
            drawList.addRectFilled(absX, absY, absX + barWidth, absY + barHeight,
                    ImGui.colorConvertFloat4ToU32(color.r, color.g, color.b, color.a), barHeight / 2);

            if (renderText) {
                float relX = cursorRelative.x;
                float relY = cursorRelative.y + type * barHeight;
                ImGui.setCursorPos(relX + barWidth + spaceBeforeText, relY + (barHeight - maxCountTextSize.y) / 2);
                ImGui.text(formatText(count, total, percentage));
                ImGui.setCursorPos(cursorRelative.x, cursorRelative.y);
            }

            if (ImGui.isMouseHoveringRect(absX, absY, absX + w, absY + barHeight)) {
                drawList.addRect(absX, absY, absX + w, absY + barHeight,
                        ImGui.colorConvertFloat4ToU32(1, 1, 1, 1), barHeight / 2);
                ImGui.setTooltip("" + count);

                hovering = true;
                typeHovering = type;
            }
        }

        if (hovering) {
            int previousValue = typeCount[typeHovering];
            int val = previousValue;
            if (ImGui.isMouseClicked(0, true)) {
                val += stepSize;
            }
            if (ImGui.isMouseClicked(1, true)) {
                val -= stepSize;
            }
            if (ImGui.isMouseClicked(2)) {
                val = 0;
            }
            val = Math.max(0, val);
            if (val != previousValue) {
                setCallback.set(typeHovering, val);
            }
        }

        ImGui.dummy(w, h);

        ImGuiUtils.helpMarker(String.format(
                "Click with different mouse buttons to change values:\nLeft +%d.\nRight -%d.\nMiddle 0.", stepSize, stepSize
        ));

        return hovering ? typeHovering : -1;
    }

    private static String formatText(int count, int total, boolean percentage) {
        if (percentage) {
            return "%.0f%%".formatted(total != 0 ? 100 * count / (double) total : 0);
        } else {
            return "%d".formatted(count);
        }
    }
}
