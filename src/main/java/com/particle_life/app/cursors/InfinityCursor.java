package com.particle_life.app.cursors;

import com.particle_life.Particle;
import com.particle_life.Physics;
import org.joml.Vector3d;

import static org.lwjgl.opengl.GL11.*;

public class InfinityCursor extends Cursor {
    @Override
    public boolean isInside(Physics physics, Particle particle) {
        return true;
    }

    @Override
    public void draw() {
    }
}
