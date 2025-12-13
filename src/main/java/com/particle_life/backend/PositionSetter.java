package com.particle_life.backend;

import org.joml.Vector3d;

public interface PositionSetter {
    void set(Vector3d position, int type, int nTypes);
}
