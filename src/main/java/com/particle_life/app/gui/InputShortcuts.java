package com.particle_life.app.gui;

import com.particle_life.app.InputState;
import com.particle_life.app.PhysicsSession;

public class InputShortcuts {

    private InputShortcuts() {
    }

    public static void setPositions(Context ctx) {
        ctx.loop.enqueue(ctx.physics::setPositions);
    }

    public static void setColors(Context ctx) {
        ctx.loop.enqueue(() -> PhysicsSession.setTypesFromSelection(ctx.physics, ctx.typeSetters.getActive()));
    }

    public static void generateMatrix(Context ctx) {
        ctx.loop.enqueue(ctx.physics::generateMatrix);
    }

    public static void toggleWrap(Context ctx) {
        final boolean newWrap = !ctx.settings.wrap;
        ctx.loop.enqueue(() -> ctx.physics.settings.wrap = newWrap);
    }

    public static void togglePause(Context ctx) {
        ctx.loop.pause ^= true;
    }

    public static void toggleTraces(Context ctx) {
        ctx.traces ^= true;
    }

    public static void openSaves(Context ctx) {
        ctx.showSavesPopup.set(true);
        ctx.requestedSaveCardsLoading.set(true);
    }

    public static void openGraphics(Context ctx) {
        ctx.showGraphicsWindow.set(true);
    }

    public static void toggleGraphics(Context ctx) {
        ctx.showGraphicsWindow.set(!ctx.showGraphicsWindow.get());
    }

    public static void handleKey(String keyName, Context ctx, InputState input) {
        input.onKeyPressed(keyName);

        if (input.isControlPressed()) {
            if ("s".equals(keyName)) {
                openSaves(ctx);
                input.clearControlAndS();
            }
            return;
        }

        switch (keyName) {
            case "ESCAPE" -> ctx.showGui.set(!ctx.showGui.get());
            case "f" -> ctx.setFullscreen.accept(!ctx.isFullscreen.get());
            case "t" -> toggleTraces(ctx);
            case "+", "=" -> ctx.zoomIn.run();
            case "-" -> ctx.zoomOut.run();
            case "z" -> ctx.resetCamera.accept(false);
            case "Z" -> ctx.resetCamera.accept(true);
            case "p" -> setPositions(ctx);
            case "c" -> setColors(ctx);
            case "g" -> toggleGraphics(ctx);
            case "m" -> generateMatrix(ctx);
            case "b" -> ctx.loop.enqueue(() -> ctx.physics.settings.wrap ^= true);
            case " " -> togglePause(ctx);
            case "q" -> ctx.closeApp.run();
        }
    }
}
