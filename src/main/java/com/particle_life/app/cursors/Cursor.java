package com.particle_life.app.cursors;

import com.particle_life.backend.Particle;
import org.joml.Vector3d;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Cursor {

    public Vector3d position = new Vector3d(0, 0, 0);
    public double size = 0.1;
    public CursorShape shape;

  public boolean isInside(Particle particle, boolean wrap) {
    return isInside(particle.position, wrap);
  }

  public boolean isInside(Vector3d particlePosition, boolean wrap) {
    if (size == 0.0) return false;

    Vector3d delta = new Vector3d(particlePosition).sub(position);

    if (wrap) {
      // wrapping the connection gives us the shortest possible distance
      // assume periodic boundaries [0, 1)
      // -> wrap connection on [-0.5, 0.5)
      for (int i = 0; i < 3; i++) {
        double val = delta.get(i);
        val -= Math.floor(val + 0.5);
        delta.setComponent(i, val);
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

    /**
     * Count particles under the cursor using snapshot position data (thread-safe for the render thread).
     */
    public int countSelection(double[] positions, int particleCount, boolean wrap) {
        int count = 0;
        for (int i = 0; i < particleCount; i++) {
            int i3 = i * 3;
            Vector3d pos = new Vector3d(positions[i3], positions[i3 + 1], positions[i3 + 2]);
            if (isInside(pos, wrap)) count++;
        }
        return count;
    }

    public void draw() {
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
