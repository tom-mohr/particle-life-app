package com.particle_life.app;

import com.particle_life.Particle;
import com.particle_life.PhysicsSettings;
import com.particle_life.multithreading.ThreadUtility;

class PhysicsSnapshot {

    private static final int PREFERRED_NUMBER_OF_THREADS = 12;

    double[] x;
    double[] v;
    int[] types;

    PhysicsSettings settings;
    double actualDt;
    double avgFramerate;
    boolean pause;
    int[] typeCount;

    /**
     * unix timestamp from when this snapshot was taken (milliseconds)
     */
    long snapshotTime;

    void take(ExtendedPhysics p) {

        write(p.particles);

        settings = p.settings.deepCopy();

        actualDt = p.getActualDt();
        avgFramerate = p.getAvgFramerate();
        pause = p.pause;

        typeCount = p.getTypeCount();

        snapshotTime = System.currentTimeMillis();
    }

    private void write(Particle[] particles) {

        //todo: only write types if necessary!

        int n = particles.length;

        if (types == null || types.length != n) {
            x = new double[n * 3];
            v = new double[n * 3];
            types = new int[n];
        }

        ThreadUtility.distributeLoadEvenly(n, PREFERRED_NUMBER_OF_THREADS, i -> {
            Particle p = particles[i];

            final int i3 = 3 * i;

            x[i3] = p.x.x;
            x[i3 + 1] = p.x.y;
            x[i3 + 2] = p.x.z;

            v[i3] = p.v.x;
            v[i3 + 1] = p.v.y;
            v[i3 + 2] = p.v.z;

            types[i] = p.type;

            return true;
        });
    }
}
