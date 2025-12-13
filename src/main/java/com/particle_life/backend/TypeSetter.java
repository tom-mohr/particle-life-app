package com.particle_life.backend;

import org.joml.Vector3d;

public interface TypeSetter {
    /**
     *
     * @param position
     * @param velocity
     * @param type   the previous type of the given particle
     * @param nTypes
     * @return the new type
     */
    int getType(Vector3d position, Vector3d velocity, int type, int nTypes);
}
