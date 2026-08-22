package com.particle_life.app.gui;

import com.particle_life.app.ImGuiBarGraph;
import com.particle_life.app.ImGuiMatrix;
import com.particle_life.app.MatrixParser;
import com.particle_life.app.utils.ImGuiUtils;
import com.particle_life.backend.Matrix;
import com.particle_life.backend.MatrixGenerator;
import com.particle_life.backend.PositionSetter;
import imgui.ImGui;
import imgui.flag.ImGuiDir;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImFloat;
import imgui.type.ImInt;

import java.util.Arrays;

public class ParticlesPanel {

    private ParticlesPanel() {
    }

    public static void draw(Context ctx) {
        ImGui.setNextWindowSize(-1, -1, imgui.flag.ImGuiCond.FirstUseEver);
        ImGui.setNextWindowPos(ctx.width, 0, imgui.flag.ImGuiCond.Always, 1.0f, 0.0f);
        ImGui.getStyle().setWindowMenuButtonPosition(ImGuiDir.Right);
        if (ImGui.begin("Particles",
                ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoNavFocus | ImGuiWindowFlags.NoMove)) {
            ImGui.pushItemWidth(200);

            ImInt particleCountInput = new ImInt(ctx.particleCount);
            if (ImGui.inputInt("Particle count", particleCountInput, 1000, 1000, ImGuiInputTextFlags.EnterReturnsTrue)) {
                final int newCount = Math.max(0, particleCountInput.get());
                ctx.loop.enqueue(() -> ctx.physics.setParticleCount(newCount));
            }

            if (ImGuiUtils.renderCombo("##positions", ctx.positionSetters)) {
                final PositionSetter nextPositionSetter = ctx.positionSetters.getActive();
                ctx.loop.enqueue(() -> ctx.physics.positionSetter = nextPositionSetter);
            }
            ImGui.sameLine();
            if (ImGui.button("Positions")) {
                InputShortcuts.setPositions(ctx);
            }
            ImGuiUtils.helpMarker("[p]");

            ImGuiUtils.separator();

            if (ImGuiUtils.renderCombo("##matrix", ctx.matrixGenerators)) {
                final MatrixGenerator nextMatrixGenerator = ctx.matrixGenerators.getActive();
                ctx.loop.enqueue(() -> ctx.physics.matrixGenerator = nextMatrixGenerator);
            }
            ImGui.sameLine();
            if (ImGui.button("Matrix")) {
                InputShortcuts.generateMatrix(ctx);
            }
            ImGuiUtils.helpMarker("[m]");

            ImGuiMatrix.draw(200 * ctx.scale, 200 * ctx.scale,
                    ctx.palettes.getActive(),
                    ctx.appSettings.matrixGuiStepSize,
                    ctx.settings.matrix,
                    (i, j, newValue) -> ctx.loop.enqueue(() -> ctx.physics.settings.matrix.set(i, j, newValue))
            );
            if (ImGui.button("Copy")) {
                ImGui.setClipboardText(MatrixParser.matrixToString(ctx.settings.matrix));
            }
            ImGui.sameLine();
            if (ImGui.button("Paste")) {
                Matrix parsedMatrix = MatrixParser.parseMatrix(ImGui.getClipboardText());
                if (parsedMatrix != null) {
                    ctx.loop.enqueue(() -> {
                        ctx.physics.setMatrixSize(parsedMatrix.size());
                        ctx.physics.settings.matrix = parsedMatrix;
                    });
                }
            }
            ImGuiUtils.helpMarker("Save / load matrix via the clipboard.");
            if (ImGui.treeNode("Settings##matrix")) {
                ImFloat inputValue = new ImFloat((float) ctx.appSettings.matrixGuiStepSize);
                if (ImGui.inputFloat("Step Size##Matrix", inputValue, 0.05f, 0.05f, "%.2f")) {
                    ctx.appSettings.matrixGuiStepSize = com.particle_life.app.utils.MathUtils.clamp(inputValue.get(), 0.05f, 1.0f);
                }
                ImGui.treePop();
            }

            ImGuiUtils.separator();

            ImGuiUtils.renderCombo("##colors", ctx.typeSetters);
            ImGui.sameLine();
            if (ImGui.button("Colors")) {
                InputShortcuts.setColors(ctx);
            }
            ImGuiUtils.helpMarker("[c] Use this to set colors of particles without changing their position.");

            ImInt matrixSizeInput = new ImInt(ctx.settings.matrix.size());
            if (ImGui.inputInt("Colors##input", matrixSizeInput, 1, 1, ImGuiInputTextFlags.EnterReturnsTrue)) {
                final int newSize = Math.max(1, Math.min(matrixSizeInput.get(), 256));
                ctx.loop.enqueue(() -> ctx.physics.setMatrixSize(newSize));
            }

            ImGuiBarGraph.draw(200, 100,
                    ctx.palettes.getActive(),
                    ctx.typeCountDiagramStepSize,
                    ctx.physicsSnapshot.typeCount,
                    (type, newValue) -> {
                        final int[] newTypeCount = Arrays.copyOf(ctx.physicsSnapshot.typeCount, ctx.physicsSnapshot.typeCount.length);
                        newTypeCount[type] = newValue;
                        ctx.loop.enqueue(() -> ctx.physics.setTypeCount(newTypeCount));
                    },
                    ctx.typeCountDisplayPercentage
            );
            if (ImGui.button("Equalize")) {
                ctx.loop.enqueue(() -> ctx.physics.setTypeCountEqual());
            }
            if (ImGui.treeNode("Settings##colorbars")) {
                ImInt inputValue = new ImInt(ctx.typeCountDiagramStepSize);
                if (ImGui.inputInt("Step Size##ColorCount", inputValue, 10)) {
                    ctx.typeCountDiagramStepSize = Math.max(0, inputValue.get());
                }

                ImInt selected = new ImInt(ctx.typeCountDisplayPercentage ? 1 : 0);
                ImGui.radioButton("Absolute", selected, 0);
                ImGui.sameLine();
                ImGui.radioButton("Percentage", selected, 1);
                ctx.typeCountDisplayPercentage = selected.get() == 1;
                ImGui.treePop();
            }

            ImGui.popItemWidth();
        }
        ImGui.end();
    }
}
