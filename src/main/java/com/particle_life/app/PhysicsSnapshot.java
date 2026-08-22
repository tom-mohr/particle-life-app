package com.particle_life.app;

import com.particle_life.backend.LoadDistributor;
import com.particle_life.backend.Particle;
import com.particle_life.backend.PhysicsSettings;

public class PhysicsSnapshot {

    private static final int PREFERRED_NUMBER_OF_THREADS = 12;

    public double[] positions;
    public double[] velocities;
    public int[] types;

    public PhysicsSettings settings;
    public int particleCount;
    public int[] typeCount;

    /**
     * unix timestamp from when this snapshot was taken (milliseconds)
     */
    public long snapshotTime;

    void take(ExtendedPhysics p, LoadDistributor loadDistributor) {

        write(p.particles, loadDistributor);

        settings = p.settings.deepCopy();

        particleCount = p.particles.length;
        typeCount = p.getTypeCount();

        snapshotTime = System.currentTimeMillis();
    }

    private void write(Particle[] particles, LoadDistributor loadDistributor) {
        int n = particles.length;

        if (types == null || types.length != n) {
            positions = new double[n * 3];
            velocities = new double[n * 3];
            types = new int[n];
        }

        loadDistributor.distributeLoadEvenly(n, PREFERRED_NUMBER_OF_THREADS, i -> {
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
