package com.particle_life.app;

import com.particle_life.backend.Accelerator;

class DefaultAccelerator {

    static Accelerator create() {
        return (a, pos) -> {
            double beta = 0.3;
            double dist = pos.length();
            double force = dist < beta ? (dist / beta - 1) : a * (1 - Math.abs(1 + beta - 2 * dist) / (1 - beta));
            return pos.mul(force / dist);
        };
    }
}
