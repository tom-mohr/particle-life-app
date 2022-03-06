package com.particle_life.app;

import com.particle_life.Particle;
import com.particle_life.PhysicsSettings;
import com.particle_life.ThreadUtility;

class PhysicsSnapshot {

    private static final int PREFERRED_NUMBER_OF_THREADS = 12;

    double[] positions;
    double[] velocities;
    int[] types;

    PhysicsSettings settings;
    int particleCount;
    int[] typeCount;

    /**
     * unix timestamp from when this snapshot was taken (milliseconds)
     */
    long snapshotTime;

    void take(ExtendedPhysics p) {

        write(p.particles);

        settings = p.settings.deepCopy();

        particleCount = p.particles.length;
        typeCount = p.getTypeCount();

        snapshotTime = System.currentTimeMillis();
    }

    private void write(Particle[] particles) {

        //todo: only write types if necessary!

        int n = particles.length;

        if (types == null || types.length != n) {
            positions = new double[n * 3];
            velocities = new double[n * 3];
            types = new int[n];
        }

        ThreadUtility.distributeLoadEvenly(n, PREFERRED_NUMBER_OF_THREADS, i -> {
            Particle p = particles[i];

            final int i3 = 3 * i;

            positions[i3] = p.position.x;
            positions[i3 + 1] = p.position.y;
            positions[i3 + 2] = p.position.z;

            velocities[i3] = p.velocity.x;
            velocities[i3 + 1] = p.velocity.y;
            velocities[i3 + 2] = p.velocity.z;

            types[i] = p.type;

            return true;
        });
    }
}
