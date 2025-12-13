package com.particle_life.backend;

public class PhysicsSettings {

    /**
     * Allows particles to move and interact across the world's borders (-1.0, +1.0).
     */
    public boolean wrap = true;

    /**
     * no interaction between particles that are further apart than rmax
     */
    public double rmax = 0.02;

    /**
     * Friction coefficient.
     * All particles' velocities are multiplied with this value in each update step to simulate friction.
     * A value of 1.0 means no friction.
     * A value of 0.0 means that the particle will come to a stop immediately.
     * <p>
     *     The given friction coefficient is based on 60 fps.
     *     To account for this normalization, the actual friction factor is calculated
     *     during each update step according to the following formula:
     *     <code>actualFriction = pow(friction, 60 / fps)</code>
     * </p>
     */
    public double friction = 0.85;

    /**
     * Scaled force by an arbitrary factor.
     */
    public double force = 1.0f;

    /**
     * Time that is assumed to have passed between each simulation step, in seconds.
     */
    public double dt = 0.02f;
    public Matrix matrix = new DefaultMatrix(6);

    public PhysicsSettings() {
    }

    public PhysicsSettings deepCopy() {
        PhysicsSettings p = new PhysicsSettings();

        p.wrap = wrap;
        p.rmax = rmax;
        p.friction = friction;
        p.force = force;
        p.dt = dt;
        p.matrix = matrix.deepCopy();

        return p;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PhysicsSettings s) {

            if (s.wrap != wrap) return false;
            if (s.rmax != rmax) return false;
            if (s.friction != friction) return false;
            if (s.force != force) return false;
            if (s.dt != dt) return false;
            if (!s.matrix.equals(matrix)) return false;

            return true;
        } else {
            return false;
        }
    }
}
