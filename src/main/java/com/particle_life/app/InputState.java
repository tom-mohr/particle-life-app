package com.particle_life.app;

public class InputState {

    public boolean draggingShift = false;
    public boolean leftDraggingParticles = false;
    public boolean rightDraggingParticles = false;
    public boolean leftPressed = false;
    public boolean rightPressed = false;
    public boolean upPressed = false;
    public boolean downPressed = false;
    public boolean wPressed = false;
    public boolean aPressed = false;
    public boolean sPressed = false;
    public boolean dPressed = false;
    public boolean leftShiftPressed = false;
    public boolean rightShiftPressed = false;
    public boolean leftControlPressed = false;
    public boolean rightControlPressed = false;
    public boolean leftAltPressed = false;
    public boolean rightAltPressed = false;

    public boolean isControlPressed() {
        return leftControlPressed || rightControlPressed;
    }

    public boolean isShiftPressed() {
        return leftShiftPressed || rightShiftPressed;
    }

    public boolean isAltPressed() {
        return leftAltPressed || rightAltPressed;
    }

    public void onKeyPressed(String keyName) {
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

    public void onKeyReleased(String keyName) {
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

    public void clearControlAndS() {
        leftControlPressed = false;
        rightControlPressed = false;
        sPressed = false;
    }
}
