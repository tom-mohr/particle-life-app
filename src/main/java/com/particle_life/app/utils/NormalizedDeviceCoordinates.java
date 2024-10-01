package com.particle_life.app.utils;

import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

/** A utility class for mapping world coordinates <-> normalized device coordinates [-1, 1]. */
public class NormalizedDeviceCoordinates {

    public Vector2d camPos;
    public Vector2d camDimensions;

    public NormalizedDeviceCoordinates(Vector2d camPos, Vector2d camDimensions) {
        this.camPos = camPos;
        this.camDimensions = camDimensions;
    }

    /** maps world coordinates to normalized device coordinates [-1, 1] */
    public Vector2d map(Vector3d world) {
        Vector2d x = new Vector2d(world.x, world.y);
        x.sub(camPos);
        x.div(camDimensions).mul(2);  // divide by (camSize * 0.5)
        return x;
    }

    /** maps normalized device coordinates [-1, 1] to world coordinates */
    public Vector3d invert(Vector2d ndc) {
        Vector2d x = new Vector2d(ndc);
        x.div(2).mul(camDimensions);
        x.add(camPos);
        return new Vector3d(x, 0);
    }

    /** Returns the transformation matrix from world coordinates to normalized device coordinates. */
    public Matrix4d getMatrix() {
        Matrix4d mat = new Matrix4d();
        getMatrix(mat);
        return mat;
    }

    /** Fills the given matrix with the transformation from world coordinates to normalized device coordinates. */
    public void getMatrix(Matrix4d mat) {
        mat.identity();
        // Note: These operations will be applied in reverse order to the vector
        //       when transforming with the matrix
        mat.scale(2 / camDimensions.x, 2 / camDimensions.y, 1);  // divide by (camSize * 0.5)
        mat.translate(-camPos.x, -camPos.y, 0);
    }
}
