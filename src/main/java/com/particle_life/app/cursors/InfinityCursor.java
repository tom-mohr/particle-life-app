package com.particle_life.app.cursors;

import com.particle_life.Particle;
import com.particle_life.Physics;
import org.joml.Vector3d;

public class InfinityCursor extends Cursor {
    @Override
    public boolean isInside(Physics physics, Particle particle) {
        return true;
    }

    @Override
    public void draw() {
    }

    @Override
    public Vector3d sampleRandomPoint() {
        return new Vector3d(2 * Math.random() - 1, 2 * Math.random() - 1, 0)
                .add(position);
    }
}
