package com.particle_life.app.cursors;

import com.particle_life.Particle;
import com.particle_life.Physics;
import org.joml.Vector3d;

import static org.lwjgl.opengl.GL11.*;

public class CircleCursor extends Cursor {

    private static final int NUM_SEGMENTS = 96;

    @Override
    public boolean isInside(Physics physics, Particle particle) {
        return physics.distance(position, particle.position) <= size;
    }

    @Override
    public void draw() {
        glBegin(GL_LINE_LOOP);
        for (int i = 0; i < NUM_SEGMENTS; i++) {
            double angle = 2 * Math.PI * i / (float) NUM_SEGMENTS;
            glVertex2d(position.x + size * Math.cos(angle),
                    position.y + size * Math.sin(angle));
        }
        glEnd();
    }

    @Override
    public Vector3d sampleRandomPoint() {
        double angle = Math.random() * 2 * Math.PI;
        return new Vector3d(Math.cos(angle), Math.sin(angle), 0)
                .mul(Math.sqrt(Math.random()) * size)
                .add(position);
    }
}