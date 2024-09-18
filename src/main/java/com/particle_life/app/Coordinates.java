package com.particle_life.app;

import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

class Coordinates {

    double windowWidth;
    double windowHeight;
    Vector2d camPos;
    double camSize;

    Coordinates(double windowWidth, double windowHeight, Vector2d camPos, double camSize) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.camPos = camPos;
        this.camSize = camSize;
    }

    Vector2d screen(Vector3d world) {
        Vector2d x = new Vector2d(world.x, world.y);
        x.sub(camPos);
        x.div(camSize);

        // quad
        // (Scales square to fit smaller dimension -- will be skewed along larger dimension of screen)
        if (windowWidth > windowHeight) {
            // fit width
            x.x *= windowHeight / windowWidth;
        } else if (windowHeight > windowWidth) {
            // fit height
            x.y *= windowWidth / windowHeight;
        }

        x.add(0.5, 0.5);
        x.mul(windowWidth, windowHeight);

        return x;
    }

    Vector2d world(double screenX, double screenY) {
        Vector2d screenTopLeft = screen(new Vector3d(0, 0, 0));
        Vector2d screenBottomRight = screen(new Vector3d(1, 1, 0));
        return new Vector2d(screenX, screenY)
                .sub(screenTopLeft)
                .div(screenBottomRight.sub(screenTopLeft));
    }

    public void apply(Matrix4d transform) {
        // Note: These operations will be applied in reverse order to the vector
        //       when transforming with the matrix

        transform.scale(1, -1, 1);  // flip y

        // OpenGL uses [-1, 1] as default coordinate system
        // -> map coordinates from [-0.5, 0.5] to [-1, 1]
        transform.scale(2, 2, 1);

        // quad
        if (windowWidth > windowHeight) {
            // fit width
            transform.scale(windowHeight / windowWidth, 1, 1);
        } else if (windowHeight > windowWidth) {
            // fit height
            transform.scale(1, windowWidth / windowHeight, 1);
        }

        transform.scale(1 / camSize);
        transform.translate(-camPos.x, -camPos.y, 0);
    }

    // SETTER METHODS

    Coordinates mouseShift(Vector2d mouseBefore, Vector2d mouseAfter) {
        Vector2d w1 = world(mouseBefore.x, mouseBefore.y);
        Vector2d w2 = world(mouseAfter.x, mouseAfter.y);
        Vector2d delta = w2.sub(w1);
        camPos.sub(delta);
        return this;
    }

    /**
     * Change the cam size while keeping the world position fixed on a certain point.
     */
    Coordinates changeCamSizeFixed(Vector2d fixedWorldPos, double newCamSize) {
        camPos
                .sub(fixedWorldPos)
                .mul(newCamSize / camSize)
                .add(fixedWorldPos);
        camSize = newCamSize;
        return this;
    }
}
