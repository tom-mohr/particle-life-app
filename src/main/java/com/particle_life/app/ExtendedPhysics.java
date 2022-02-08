package com.particle_life.app;

import com.particle_life.*;

import java.util.Arrays;
import java.util.Collections;

/**
 * Provides additional functionality for the Physics class
 * that is optional or too arbitrary to be included in the actual physics package.
 */
class ExtendedPhysics extends Physics {

    ExtendedPhysics(Accelerator accelerator, PositionSetter positionSetter, MatrixGenerator matrixGenerator, TypeSetter typeSetter) {
        super(accelerator, positionSetter, matrixGenerator, typeSetter);
    }

    public int[] getTypeCount() {
        int[] typeCount = new int[settings.matrix.size()];
        Arrays.fill(typeCount, 0);
        for (Particle p : particles) {
            typeCount[p.type]++;
        }
        return typeCount;
    }

    public void setTypeCountEqual() {

        int nTypes = settings.matrix.size();
        if (nTypes < 2) return;

        int[] idealTypeCount = new int[nTypes];
        int count = (int) Math.ceil(particles.length / (double) nTypes);
        Arrays.fill(idealTypeCount, 0, nTypes - 1, count);
        idealTypeCount[nTypes - 1] = particles.length - (nTypes - 1) * (count);

        setTypeCount(idealTypeCount);
    }

    public void setTypeCount(int[] typeCount) {

        int nTypes = settings.matrix.size();
        if (nTypes < 2) return;

        if (typeCount.length != nTypes) {
            throw new IllegalArgumentException(String.format(
                    "Got array of length %d, but current matrix size is %d. Maybe you should change the matrix size before doing this.",
                    typeCount.length, nTypes));
        }

        // randomly shuffle particles first
        // (otherwise, the container layout becomes visible)
        shuffleParticles();

        int newCount = Arrays.stream(typeCount).sum();
        if (newCount != particles.length) {

            settings.n = newCount;

            Particle[] newParticles = new Particle[settings.n];

            int[] actualTypeCount = new int[nTypes];
            Arrays.fill(actualTypeCount, 0);

            // sort all unusable particles to the end
            int i = 0;
            int j = particles.length - 1;
            while (i < j) {

                int type = particles[i].type;
                if (actualTypeCount[type] < typeCount[type]) {
                    // need more of this type -> leave it in front
                    actualTypeCount[type]++;
                    i++;
                } else {
                    // have enough of this type -> swap to back
                    ArrayUtils.swap(particles, i, j);
                    j--;
                }
            }
            // now i points at the end (exclusive) of the reusable particles

            // copy as much as possible
            int copyLength = Math.min(settings.n, particles.length);
            int k = 0;
            while (k < copyLength) {
                newParticles[k] = particles[k];
                k++;
            }

            // if necessary, fill up rest with new particles
            while (k < settings.n) {
                newParticles[k] = new Particle();
                k++;
            }

            // change types of all particles that couldn't be reused
            while (i < settings.n) {

                // find type that has too few particles
                int type = ArrayUtils.findFirstIndexWithLess(actualTypeCount, typeCount);  // need more of this type

                Particle p = newParticles[i];
                p.type = type;
                setPosition(p);  // possible that position setter is based on type

                actualTypeCount[type]++;

                i++;
            }

            particles = newParticles;

        } else {

            int[] actualTypeCount = getTypeCount();

            for (Particle p : particles) {
                if (actualTypeCount[p.type] > typeCount[p.type]) {
                    // need fewer of this type

                    // find type that has too few particles
                    int type = ArrayUtils.findFirstIndexWithLess(actualTypeCount, typeCount);  // need more of this type

                    // change type
                    actualTypeCount[p.type]--;
                    p.type = type;
                    actualTypeCount[type]++;
                }
            }
        }
    }

    /**
     * Use this to avoid the container pattern showing
     * (i.e. if particles are treated differently depending on their position in the array).
     */
    private void shuffleParticles() {
        Collections.shuffle(Arrays.asList(particles));
    }
}
