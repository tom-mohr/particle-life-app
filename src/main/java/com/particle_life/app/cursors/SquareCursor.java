package com.particle_life.app.cursors;

import com.particle_life.Particle;
import com.particle_life.Physics;
import org.joml.Vector3d;

import static org.lwjgl.opengl.GL11.*;

public class SquareCursor extends Cursor {
    @Override
    public boolean isInside(Physics physics, Particle particle) {
        Vector3d diff = physics.connection(position, particle.position).absolute();
        return diff.x <= size && diff.y <= size;
    }

    @Override
    public void draw() {
        glBegin(GL_LINE_LOOP);
        glVertex2d(position.x - size, position.y - size);
        glVertex2d(position.x + size, position.y - size);
        glVertex2d(position.x + size, position.y + size);
        glVertex2d(position.x - size, position.y + size);
        glEnd();
    }
}
