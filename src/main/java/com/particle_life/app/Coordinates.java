package com.particle_life.app;

import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

class Coordinates {

    double width;
    double height;
    Vector3d shift;
    double zoom;

    Coordinates(double width, double height, Vector3d shift, double zoom) {
        this.width = width;
        this.height = height;
        this.shift = shift;
        this.zoom = zoom;
    }

    /**
     * Maps x: [-1, 1] -> [0, width],
     *      y: [-1, 1] -> [0, height]
     */
    private Vector2d map(Vector2d vector) {
        return vector
                .add(1.0, 1.0)
                .div(2.0)
                .mul(width, height);
    }

    /**
     * Scales square to fit smaller dimension (will be scaled along larger dimension of screen)
     */
    private Vector2d quad(Vector2d vector) {
        if (width >= height) {
            vector.mul(height / (double) width, 1);
        } else {
            vector.mul(1, width / (double) height);
        }
        return vector;
    }

    /**
     * screen(x) = map(quad(zoom(shift(x))))
     *           = map(quad(zoom * (x + shift)))
     */
    Vector2d screen(Vector3d vector) {
        vector
                .add(shift)
                .mul(zoom);

        return map(quad(new Vector2d(vector.x, vector.y)));
    }

    Vector3d world(double screenX, double screenY) {
        Vector2d screenTopLeft = screen(new Vector3d(-1, -1, 0));
        Vector2d screenBottomRight = screen(new Vector3d(1, 1, 0));
        return new Vector3d(new Vector2d(screenX, screenY)
                .sub(screenTopLeft)
                .div(screenBottomRight
                        .sub(screenTopLeft))
                .mul(2.0)
                .sub(1.0, 1.0), 0);
    }

    public void apply(Matrix4d transform) {

        transform.scale(1, -1, 1);  // flip y

        // quad(x)
        if (width >= height) {
            transform.scale(height / width, 1, 1);  // fit width
        } else {
            transform.scale(1, width / height, 1);  // fit height
        }

        transform.scale(zoom);
        transform.translate(shift);
    }

    // SETTER METHODS

    Coordinates mouseShift(Vector2d mouseBefore, Vector2d mouseAfter) {
        Vector3d w1 = world(mouseBefore.x, mouseBefore.y);
        Vector3d w2 = world(mouseAfter.x, mouseAfter.y);
        shift.add(w2).sub(w1);
        return this;
    }

    Coordinates zoomInOnMouse(Vector2d mouse, double zoomFactor) {

        Vector3d w = world(mouse.x, mouse.y);

        zoom *= zoomFactor;

        shift.set(new Vector3d(w)
                .add(shift)
                .div(zoomFactor)
                .sub(w));

        return this;
    }
}
