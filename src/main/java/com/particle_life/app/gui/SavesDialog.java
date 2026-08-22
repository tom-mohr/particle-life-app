package com.particle_life.app.gui;

import com.particle_life.app.ImGuiCardView;
import com.particle_life.app.utils.ImGuiUtils;
import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;

public class SavesDialog {

    private SavesDialog() {
    }

    public static void draw(Context ctx) {
        if (ctx.showSavesPopup.get()) ImGui.openPopup("Saves");
        ImGui.setNextWindowSize(480, -1, imgui.flag.ImGuiCond.Always);
        ImGui.setNextWindowPos(ctx.width / 2f, ctx.height / 2f, imgui.flag.ImGuiCond.Appearing, 0.5f, 0.5f);
        ImGui.setNextWindowBgAlpha(1f);
        if (ImGui.beginPopupModal("Saves", ctx.showSavesPopup, ImGuiWindowFlags.NoResize)) {
            ImGui.textDisabled("""
                    Left-click to load, middle-click to delete.
                    The most recent saves are at the top.
                    Each save corresponds to a .zip file in the 'saves' directory.
                    """
            );
            ImGuiUtils.separator();

            float cardViewWidth = ImGui.getWindowContentRegionMaxX() - 2 * ImGui.getStyle().getFramePaddingX();
            ImGui.beginChild("save cards", cardViewWidth, 250);
            ImGuiCardView.Card[] filteredCards = Arrays
                    .stream(ctx.saveCards)
                    .filter(card -> card.name.contains(ctx.saveName.get()))
                    .sorted(Comparator.comparing(card -> -card.file.lastModified()))
                    .toArray(ImGuiCardView.Card[]::new);
            ImGuiCardView.draw(
                    cardViewWidth,
                    100,
                    8,
                    filteredCards,
                    card -> {
                        ctx.onLoadSave.accept(card.file);
                        ctx.showSavesPopup.set(false);
                    },
                    card -> {
                        try {
                            Files.deleteIfExists(card.file.toPath());
                        } catch (IOException e) {
                            ctx.reportError.accept(e);
                        }
                        ctx.requestedSaveCardsLoading.set(true);
                    }
            );
            ImGui.endChild();

            if (!ImGui.isAnyItemActive() && !ImGui.isMouseClicked(0)) {
                ImGui.setKeyboardFocusHere(0);
            }
            boolean shouldSave = ImGui.inputTextWithHint("##save name", "Save Name", ctx.saveName, ImGuiInputTextFlags.EnterReturnsTrue);
            ImGuiUtils.helpMarker("Enter a name and press Enter to save the current state.");
            if (shouldSave) {
                String title = ctx.saveName.get();
                ctx.saveName.clear();
                if (!title.isBlank()) {
                    ctx.selectedSaveFile = new File("saves/" + title + ".zip");
                    ctx.onSaveRequested.run();
                }
            }
            ImGui.endPopup();
        }
    }
}
