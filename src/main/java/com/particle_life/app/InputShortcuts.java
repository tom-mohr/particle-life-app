package com.particle_life.app;

class InputShortcuts {

    static void setPositions(GuiContext ctx) {
        ctx.loop.enqueue(ctx.physics::setPositions);
    }

    static void setColors(GuiContext ctx) {
        ctx.loop.enqueue(() -> PhysicsSession.setTypesFromSelection(ctx.physics, ctx.typeSetters.getActive()));
    }

    static void generateMatrix(GuiContext ctx) {
        ctx.loop.enqueue(ctx.physics::generateMatrix);
    }

    static void toggleWrap(GuiContext ctx) {
        final boolean newWrap = !ctx.settings.wrap;
        ctx.loop.enqueue(() -> ctx.physics.settings.wrap = newWrap);
    }

    static void togglePause(GuiContext ctx) {
        ctx.loop.pause ^= true;
    }

    static void toggleTraces(GuiContext ctx) {
        ctx.traces ^= true;
    }

    static void openSaves(GuiContext ctx) {
        ctx.showSavesPopup.set(true);
        ctx.requestedSaveCardsLoading.set(true);
    }

    static void openGraphics(GuiContext ctx) {
        ctx.showGraphicsWindow.set(true);
    }

    static void toggleGraphics(GuiContext ctx) {
        ctx.showGraphicsWindow.set(!ctx.showGraphicsWindow.get());
    }

    static void handleKey(String keyName, GuiContext ctx, InputState input) {
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
