package com.particle_life.app.cursors;

import com.particle_life.Particle;
import com.particle_life.Physics;

import static org.lwjgl.opengl.GL11.*;

public class VelocityCursor extends CircleCursor {

    private static final double VELOCITY_THRESHOLD = 0.01;

    @Override
    public boolean isInside(Physics physics, Particle particle) {
        return super.isInside(physics, particle)&& particle.velocity.length() > VELOCITY_THRESHOLD;
    }
}