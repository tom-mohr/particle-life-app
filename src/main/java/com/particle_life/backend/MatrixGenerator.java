package com.particle_life.backend;

public interface MatrixGenerator {

    /**
     * Must return Matrix of the given size.
     * @param size
     * @return Matrix of the given size
     */
    Matrix makeMatrix(int size);
}
