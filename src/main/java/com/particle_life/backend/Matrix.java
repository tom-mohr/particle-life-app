package com.particle_life.backend;

public interface Matrix {
    int size();
    double get(int i, int j);
    void set(int i, int j, double value);
    Matrix deepCopy();
}
