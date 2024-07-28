package com.particle_life.app.cursors;

import com.particle_life.Particle;
import com.particle_life.app.shaders.CursorShader;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Cursor {

    public Vector3d position = new Vector3d(0, 0, 0);
    public double size = 0.1;
    public CursorShape shape;

    private final CursorShader cursorShader = new CursorShader();

    public Cursor() throws IOException {
    }

    public boolean isInside(Particle particle, boolean wrap) {
        if (size == 0.0) return false;

        Vector3d delta = new Vector3d(particle.position).sub(position);

        if (wrap) {
            // wrapping the connection gives us the shortest possible distance
            // assume periodic boundaries [-1, 1)
            for (int i = 0; i < 3; i++) {
                double val = delta.get(i);
                if (val >= 1.0) delta.setComponent(i, val - 2.0);
                else if (val < -1.0) delta.setComponent(i, val + 2.0);
            }
        }

        Vector3d deltaNormalized = delta.div(size);  // relative to cursor size

        return shape.isInside(deltaNormalized);
    }

    public List<Particle> getSelection(Particle[] particles, boolean wrap) {
        List<Particle> selectedParticles = new ArrayList<>();
        for (Particle particle : particles) {
            if (isInside(particle, wrap)) selectedParticles.add(particle);
        }
        return selectedParticles;
    }

    public int countSelection(Particle[] particles, boolean wrap) {
        int count = 0;
        for (Particle particle : particles) {
            if (isInside(particle, wrap)) count++;
        }
        return count;
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

    public Cursor copy() throws IOException {
        Cursor c = new Cursor();
        c.position.set(position);
        c.size = size;
        c.shape = shape.copy();
        return c;
    }
}
