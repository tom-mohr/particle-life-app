package com.particle_life.app.utils;

import org.joml.Vector2d;
import org.joml.Vector3d;

/**
 * A utility class for camera operations like dragging and zooming.
 * The camera position and size are defined in world coordinates.
 * The utility methods {@link #dragCam(Vector2d, Vector2d)} and {@link #zoom(double, double, double)}
 * actually modify the camera position and camera size vectors passed to the constructor of this class.
 */
public class CamOperations {

    public final Vector2d camPos;
    public double camSize;
    public double screenWidth;
    public double screenHeight;

    public static class BoundingBox {
        public double left;
        public double top;
        public double right;
        public double bottom;
    }

    public CamOperations(Vector2d camPos, double camSize, double screenWidth, double screenHeight) {
        this.camPos = camPos;
        this.camSize = camSize;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    /**
     * Modify the internal cam position by simulating a drag operation by the user.
     *
     * @param dragStart The screen coordinates where the drag started.
     * @param dragStop  The screen coordinates where the drag stopped.
     */
    public void dragCam(Vector2d dragStart, Vector2d dragStop) {
        ScreenCoordinates screen = new ScreenCoordinates(
                camPos, camSize,
                screenWidth, screenHeight
        );
        Vector3d dragStartWorld = screen.screenToWorld(dragStart);
        Vector3d dragStopWorld = screen.screenToWorld(dragStop);
        Vector3d delta = new Vector3d(dragStopWorld).sub(dragStartWorld);
        camPos.sub(delta.x, delta.y);
    }

    /**
     * Change the internal {@link #camSize} while keeping the world position fixed on a certain point on the screen.
     * This method will modify this object's {@link #camSize} and {@link #camPos} fields.
     *
     * @param screenPivotX The x-coordinate of the screen point that should stay fixed.
     * @param screenPivotY The y-coordinate of the screen point that should stay fixed.
     * @param newCamSize  The new cam size.
     */
    public void zoom(double screenPivotX, double screenPivotY, double newCamSize) {
        Vector3d worldPivot = new ScreenCoordinates(
                camPos, camSize,
                screenWidth, screenHeight
        ).screenToWorld(screenPivotX, screenPivotY);

        camPos.sub(worldPivot.x, worldPivot.y);
        camPos.mul(newCamSize / camSize);
        camPos.add(worldPivot.x, worldPivot.y);

        camSize = newCamSize;
    }

    /**
     * Compute the camera dimensions matching the aspect ratio
     * dictated by this object's {@code screenWidth} and {@code screenHeight}.
     * The smaller component of the returned vector is {@link #camSize},
     * the other component is scaled up to match the aspect ratio of the window.
     */
    public Vector2d getCamDimensions() {
        return getCamDimensions(camSize);
    }

    /**
     * Compute the camera dimensions matching the aspect ratio
     * dictated by this object's {@code screenWidth} and {@code screenHeight}.
     * The smaller component of the returned vector is {@code camSize},
     * the other component is scaled up to match the aspect ratio of the window.
     * The original {@code camSize} can therefore always be restored via
     * {@code camDimensions.minComponent()}.
     */
    public Vector2d getCamDimensions(double camSize) {
        return getCamDimensions(camSize, screenWidth, screenHeight);
    }

    /**
     * Compute the camera dimensions matching the aspect ratio
     * dictated by the given {@code screenWidth} and {@code screenHeight}.
     * The smaller component of the returned vector is {@code camSize},
     * the other component is scaled up to match the aspect ratio of the window.
     * The original {@code camSize} can therefore always be restored via
     * {@code camDimensions.minComponent()}.
     */
    public static Vector2d getCamDimensions(double camSize, double screenWidth, double screenHeight) {
        Vector2d c = new Vector2d(camSize);
        if (screenWidth > screenHeight) {
            c.x *= screenWidth / screenHeight;
        } else if (screenHeight > screenWidth) {
            c.y *= screenHeight / screenWidth;
        }
        return c;
    }

    public BoundingBox getBoundingBox() {
        BoundingBox bb = new BoundingBox();
        Vector2d halfDim = getCamDimensions().div(2);
        bb.left = camPos.x - halfDim.x;
        bb.right = camPos.x + halfDim.x;
        bb.top = camPos.y - halfDim.y;
        bb.bottom = camPos.y + halfDim.y;
        return bb;
    }
}
