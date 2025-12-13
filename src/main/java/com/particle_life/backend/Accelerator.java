package com.particle_life.backend;

import org.joml.Vector3d;

public interface Accelerator {

    /**
     * Implementations of this interface are allowed to modify <code>pos</code>.
     * So, instead of allocating a new vector, they can modify and return <code>pos</code>.
     * @param a   The entry of the matrix at index (i, j), where i is the type of the regarded particle and j the type of the other particle.
     * @param pos Position of the neighbor relative to the particle's own position,
     *            with its length divided by rmax, so this vector will always have a length <= 1.
     * @return The acceleration that should be applied to the particle's velocity.
     *         This is also interpreted as relative to rmax, that is, it will be scaled by rmax before it is applied to the particle.
     */
    Vector3d accelerate(double a, Vector3d pos);
}
