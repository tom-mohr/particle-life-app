package com.particle_life.app.gui;

import imgui.ImGui;

public class PhysicsNotReactingDialog {

    private PhysicsNotReactingDialog() {
    }

    public static void draw(Context ctx) {
        long physicsNotReactingSince = System.currentTimeMillis() - ctx.physicsSnapshot.snapshotTime;
        boolean physicsNotReacting = physicsNotReactingSince > ctx.physicsNotReactingThreshold;
        if (physicsNotReacting) ImGui.openPopup("Not reacting");
        if (ImGui.beginPopupModal("Not reacting")) {
            if (!physicsNotReacting) {
                ImGui.closeCurrentPopup();
            }

            ImGui.text("Physics didn't react since %4.0f seconds.".formatted(physicsNotReactingSince / 1000.0));

            if (ImGui.button("Reset Physics")) {
                ctx.restartPhysics.run();
            }

            ImGui.endPopup();
        }
    }
}
