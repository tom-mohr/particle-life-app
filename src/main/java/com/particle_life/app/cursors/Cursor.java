package com.particle_life.app.cursors;

import com.particle_life.Particle;
import com.particle_life.Physics;
import com.particle_life.app.shaders.CursorShader;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class Cursor {

    public Vector3d position = new Vector3d(0, 0, 0);
    public double size = 0.1;
    public CursorShape shape;

    private final CursorShader cursorShader = new CursorShader();

    public boolean isInside(Particle particle, Physics physics) {
        if (size == 0.0) return false;
        return shape.isInside(physics.connection(position, particle.position).div(size));
    }

    public List<Particle> getSelection(Physics physics) {
        List<Particle> selectedParticles = new ArrayList<>();
        for (Particle particle : physics.particles) {
            if (isInside(particle, physics)) selectedParticles.add(particle);
        }
        return selectedParticles;
    }

    public void draw(Matrix4d transform) {
        cursorShader.use();
        cursorShader.setTransform(transform
                .translate(position)
                .scale(size)
        );
        if (!shape.isInitialized()) shape.initialize();  // lazy initialize shapes (register VBOs etc. for drawing)
        shape.draw();
    }

    public Vector3d sampleRandomPoint() {
        return shape.sampleRandomPoint().mul(size).add(position);
    }

    public Cursor copy() {
        Cursor c = new Cursor();
        c.position.set(position);
        c.size = size;
        c.shape = shape.copy();
        return c;
    }
}
