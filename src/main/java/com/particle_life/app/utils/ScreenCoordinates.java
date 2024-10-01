package com.particle_life.app.utils;

import org.joml.Vector2d;
import org.joml.Vector3d;

/**
 * A utility class for mapping world coordinates <-> screen coordinates [0, screenWidth], [0, screenHeight].
 * This class is a wrapper around {@link NormalizedDeviceCoordinates} that adds screen size information.
 */
public class ScreenCoordinates {

    public NormalizedDeviceCoordinates ndc;
    public double screenWidth;
    public double screenHeight;

    public ScreenCoordinates(NormalizedDeviceCoordinates ndc, double screenWidth, double screenHeight) {
        this.ndc = ndc;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public ScreenCoordinates(Vector2d camPos, double camSize, double screenWidth, double screenHeight) {
        this.ndc = new NormalizedDeviceCoordinates(
                camPos,
                CamOperations.getCamDimensions(camSize, screenWidth, screenHeight)
        );
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    /**
     * Maps world coordinates -> screen coordinates [0, screenWidth], [0, screenHeight].
     */
    public Vector2d worldToScreen(Vector3d world) {
        Vector2d x = ndc.map(world); // map to normalized device coordinates [-1, 1]

        // map from [-1, 1] to [0, windowWidth] and [0, windowHeight]
        x.add(1, 1);
        x.div(2, 2);
        x.mul(screenWidth, screenHeight);

        return x;
    }

    public Vector3d screenToWorld(double screenX, double screenY) {
        return screenToWorld(new Vector2d(screenX, screenY));
    }

    /**
     * Inversion of {@link #worldToScreen(Vector3d)}.
     */
    public Vector3d screenToWorld(Vector2d screen) {
        Vector2d x = new Vector2d(screen);

        // map from [0, windowWidth] and [0, windowHeight] to [-1, 1]
        x.div(screenWidth, screenHeight);
        x.mul(2);
        x.sub(1, 1);

        return ndc.invert(x);  // map [-1, 1] to world coordinates
    }

}
