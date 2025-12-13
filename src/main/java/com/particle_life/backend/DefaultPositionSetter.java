package com.particle_life.backend;

import org.joml.Vector3d;

public class DefaultPositionSetter implements PositionSetter {

    @Override
    public void set(Vector3d position, int type, int nTypes) {
        position.set(
                Math.random(),
                Math.random(),
                0
        );
    }
}
