package com.particle_life.backend;

import org.joml.Vector3d;

public class DefaultTypeSetter implements TypeSetter {

    @Override
    public int getType(Vector3d position, Vector3d velocity, int type, int nTypes) {
        return (int) Math.floor(Math.random() * nTypes);
    }
}
