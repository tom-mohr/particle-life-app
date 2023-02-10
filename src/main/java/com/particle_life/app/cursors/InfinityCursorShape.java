package com.particle_life.app.cursors;

import org.joml.Vector3d;

import java.util.Random;

public class InfinityCursorShape extends CursorShape {

    Random random = new Random();

    @Override
    boolean isInside(Vector3d connection) {
        return true;
    }

    @Override
    void draw() {
    }

    @Override
    Vector3d sampleRandomPoint() {
        return new Vector3d(
                random.nextGaussian(),
                random.nextGaussian(),
                0
        );
    }
}
