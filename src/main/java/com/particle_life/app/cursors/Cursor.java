package com.particle_life.app.cursors;

import com.particle_life.Particle;
import com.particle_life.Physics;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Cursor for manipulating particles.
 */
public abstract class Cursor {

    protected Vector3d position = new Vector3d(0, 0, 0);
    protected double size = 0.1;

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public void setPosition(double x, double y) {
        position.set(x, y, 0);
    }

    public double x() {
        return position.x;
    }

    public double y() {
        return position.y;
    }

    public abstract boolean isInside(Physics physics, Particle particle);

    public List<Particle> getSelection(Physics physics) {
        List<Particle> selectedParticles = new ArrayList<>();
        for (Particle particle : physics.particles) {
            if (isInside(physics, particle)) selectedParticles.add(particle);
        }
        return selectedParticles;
    }

    public abstract void draw();

    public abstract Vector3d sampleRandomPoint();

    public Cursor copy() {
        Cursor c = makeNewInstance();
        c.position.set(position);
        c.size = size;
        return c;
    }

    private Cursor makeNewInstance() {
        try {
            // assume this has a default constructor that takes no arguments
            return (Cursor) this.getClass().getConstructors()[0].newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
