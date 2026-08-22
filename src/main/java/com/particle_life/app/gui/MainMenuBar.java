package com.particle_life.app.gui;

import imgui.ImGui;

public class MainMenuBar {

    private MainMenuBar() {
    }

    public static void draw(Context ctx) {
        if (ImGui.beginMenu("Menu")) {
            if (ImGui.menuItem("Saves##menu", "Ctrl+s")) {
                InputShortcuts.openSaves(ctx);
            }
            if (ImGui.menuItem("Controls..")) {
                ctx.showControlsWindow.set(true);
            }
            if (ImGui.menuItem("About..")) {
                ctx.showAboutWindow.set(true);
            }
            if (ImGui.menuItem("Quit", "Alt+F4, q")) {
                ctx.closeApp.run();
            }
            ImGui.endMenu();
        }

        if (ImGui.beginMenu("View")) {
            if (ctx.isFullscreen.get()) {
                if (ImGui.menuItem("Exit Fullscreen", "F11, f")) {
                    ctx.setFullscreen.accept(false);
                }
            } else {
                if (ImGui.menuItem("Fullscreen", "F11, f")) {
                    ctx.setFullscreen.accept(true);
                }
            }

            if (ImGui.menuItem("Hide GUI", "Esc")) {
                ctx.showGui.set(false);
            }

            if (ImGui.beginMenu("Zoom")) {
                if (ImGui.menuItem("100%", "z")) {
                    ctx.resetCamera.accept(false);
                }
                if (ImGui.menuItem("Fit", "Z")) {
                    ctx.resetCamera.accept(true);
                }
                ImGui.endMenu();
            }

            if (ImGui.menuItem("Graphics..", "g")) {
                InputShortcuts.openGraphics(ctx);
            }

            ImGui.endMenu();
        }
    }
}
