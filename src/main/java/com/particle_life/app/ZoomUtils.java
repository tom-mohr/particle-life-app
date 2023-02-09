package com.particle_life.app;

final class ZoomUtils {
    static final double MAX_ZOOM = 0.1;

    /**
     * Controls maximum value of the camera zoom out.
     * @param currentZoom is the current value of screen zoom
     * @param zoomStepFactor is the screen zoom step factor
     * @return new screen zoom value
     */
    public static double zoomOut(double currentZoom, double zoomStepFactor) {
        double newZoom = currentZoom / Math.pow(zoomStepFactor, 2);

        if (newZoom < MAX_ZOOM) {
            return currentZoom;
        }

        return newZoom;
    }
}