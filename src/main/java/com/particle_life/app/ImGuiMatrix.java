package com.particle_life.app;

import com.particle_life.Matrix;
import com.particle_life.app.color.Color;
import com.particle_life.app.color.Palette;
import com.particle_life.app.utils.ImGuiUtils;
import com.particle_life.app.utils.MathUtils;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import org.joml.Vector2i;

class ImGuiMatrix {

    interface SetCallback {
        /**
         * @param newValue is guaranteed to be in [-1, 1].
         */
        void set(int i, int j, double newValue);
    }

    /**
     * @param w
     * @param h
     * @param palette
     * @param stepSize
     * @param matrix
     * @return field of matrix that is hovered by mouse or null
     */
    public static Vector2i draw(float w, float h, Palette palette, double stepSize, Matrix matrix, SetCallback setCallback) {
        ImDrawList drawList = ImGui.getWindowDrawList();

        ImVec2 cursor = new ImVec2();
        ImGui.getCursorScreenPos(cursor);

        int matrixSize = matrix.size();
        float rectSize = (Math.min(w, h)) / (matrixSize + 1);

        // draw matrix values
        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                float x = cursor.x + rectSize + j * rectSize;
                float y = cursor.y + rectSize + i * rectSize;
                float value = (float) matrix.get(i, j);

                int color = value >= 0 ? ImGui.colorConvertFloat4ToU32(0, value, 0, 1) : ImGui.colorConvertFloat4ToU32(-value, 0, 0, 1);
                drawList.addRectFilled(x, y, x + rectSize, y + rectSize, color);
            }
        }

        // draw a background-colored rounded rectangle around the matrix
        // to give the matrix rounded corners
        float cornerRadius = 8;
        drawList.addRect(cursor.x + rectSize - cornerRadius, cursor.y + rectSize - cornerRadius,
                cursor.x + rectSize * (1 + matrixSize) + cornerRadius, cursor.y + rectSize * (1 + matrixSize) + cornerRadius,
                ImGui.getColorU32(ImGuiCol.WindowBg), 2 * cornerRadius, 0, 2 * cornerRadius);

        float circleRadius = rectSize / 2.0f * 0.8f;

        // draw colored circles (rows)
        for (int i = 0; i < matrixSize; i++) {
            float x = cursor.x;
            float y = cursor.y + rectSize + i * rectSize;
            Color color = palette.getColor(i, matrixSize);
            drawList.addCircleFilled(x + rectSize / 2.0f, y + rectSize / 2.0f, circleRadius, ImGui.colorConvertFloat4ToU32(color.r, color.g, color.b, color.a), 32);
        }

        // draw colored circles (columns)
        for (int j = 0; j < matrixSize; j++) {
            float x = cursor.x + rectSize + rectSize * j;
            float y = cursor.y;
            Color color = palette.getColor(j, matrixSize);
            drawList.addCircleFilled(x + rectSize / 2.0f, y + rectSize / 2.0f, circleRadius,
                    ImGui.colorConvertFloat4ToU32(color.r, color.g, color.b, color.a), 32);
        }

        // draw hovering highlight & tooltip
        boolean hovering = false;
        int iHovering = 0;
        int jHovering = 0;
        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                float x = cursor.x + rectSize + j * rectSize;
                float y = cursor.y + rectSize + i * rectSize;
                float value = (float) matrix.get(i, j);

                if (ImGui.isMouseHoveringRect(x, y, x + rectSize, y + rectSize)) {
                    drawList.addRect(x, y, x + rectSize, y + rectSize,
                            ImGui.colorConvertFloat4ToU32(1, 1, 1, 1), 4);
                    ImGui.setTooltip(String.format("%5.2f", value));

                    hovering = true;
                    iHovering = i;
                    jHovering = j;
                }
            }
        }

        // user interaction
        if (hovering) {
            double previousValue = matrix.get(iHovering, jHovering);
            double val = previousValue;
            if (ImGui.isMouseClicked(0, true)) {
                val = MathUtils.tolerantFloor((previousValue + stepSize) / stepSize, 0.001) * stepSize;
            }
            if (ImGui.isMouseClicked(1, true)) {
                val = MathUtils.tolerantCeil((previousValue - stepSize) / stepSize, 0.001) * stepSize;
            }
            if (ImGui.isMouseClicked(2)) {
                val = 0;
            }
            val = MathUtils.clamp(val, -1, 1);
            if (val != previousValue) {
                setCallback.set(iHovering, jHovering, val);
            }
        }

        ImGui.dummy(w, h);

        ImGuiUtils.helpMarker(String.format(
                "Click with different mouse buttons to change values:\nLeft +%.2f.\nRight -%.2f.\nMiddle 0.", stepSize, stepSize
        ));

        return hovering ? new Vector2i(iHovering, jHovering) : null;
    }
}
