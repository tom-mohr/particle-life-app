package com.particle_life.app;

class InputState {

    boolean draggingShift = false;
    boolean leftDraggingParticles = false;
    boolean rightDraggingParticles = false;
    boolean leftPressed = false;
    boolean rightPressed = false;
    boolean upPressed = false;
    boolean downPressed = false;
    boolean wPressed = false;
    boolean aPressed = false;
    boolean sPressed = false;
    boolean dPressed = false;
    boolean leftShiftPressed = false;
    boolean rightShiftPressed = false;
    boolean leftControlPressed = false;
    boolean rightControlPressed = false;
    boolean leftAltPressed = false;
    boolean rightAltPressed = false;

    boolean isControlPressed() {
        return leftControlPressed || rightControlPressed;
    }

    boolean isShiftPressed() {
        return leftShiftPressed || rightShiftPressed;
    }

    boolean isAltPressed() {
        return leftAltPressed || rightAltPressed;
    }

    void onKeyPressed(String keyName) {
        switch (keyName) {
            case "LEFT" -> leftPressed = true;
            case "RIGHT" -> rightPressed = true;
            case "UP" -> upPressed = true;
            case "DOWN" -> downPressed = true;
            case "w" -> wPressed = true;
            case "a" -> aPressed = true;
            case "s" -> sPressed = true;
            case "d" -> dPressed = true;
            case "LEFT_SHIFT" -> leftShiftPressed = true;
            case "RIGHT_SHIFT" -> rightShiftPressed = true;
            case "LEFT_CONTROL" -> leftControlPressed = true;
            case "RIGHT_CONTROL" -> rightControlPressed = true;
            case "LEFT_ALT" -> leftAltPressed = true;
            case "RIGHT_ALT" -> rightAltPressed = true;
        }
    }

    void onKeyReleased(String keyName) {
        switch (keyName) {
            case "LEFT" -> leftPressed = false;
            case "RIGHT" -> rightPressed = false;
            case "UP" -> upPressed = false;
            case "DOWN" -> downPressed = false;
            case "w" -> wPressed = false;
            case "a" -> aPressed = false;
            case "s" -> sPressed = false;
            case "d" -> dPressed = false;
            case "LEFT_SHIFT" -> leftShiftPressed = false;
            case "RIGHT_SHIFT" -> rightShiftPressed = false;
            case "LEFT_CONTROL" -> leftControlPressed = false;
            case "RIGHT_CONTROL" -> rightControlPressed = false;
            case "LEFT_ALT" -> leftAltPressed = false;
            case "RIGHT_ALT" -> rightAltPressed = false;
        }
    }

    void clearControlAndS() {
        leftControlPressed = false;
        rightControlPressed = false;
        sPressed = false;
    }
}
