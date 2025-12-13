package com.particle_life.backend;

import org.joml.Vector3d;

/**
 * Provides functions for assuring that the coordinates of particles are in [0, 1].
 * <p>Two approaches are possible:
 * <ol>
 *     <li>{@link #clamp(Vector3d) Range.clamp(x)}<p>
 *         Leaves coordinates inside [0, 1] untouched.
 *         All other coordinates are clamped between 0 and 1.
 *         <p>Example:
 *         <pre>
 *             x = new Vector3d(0.4, -1.3, 2.0);
 *             Range.clamp(x);
 *             // x is now (0.4, 0.0, 1.0).
 *         </pre>
 *     </li>
 *     <li>{@link #wrap(Vector3d) Range.wrap(x)}<p>
 *         Leaves coordinates inside [0, 1) untouched.
 *         All other coordinates are modified by repeatedly adding or subtracting 1 until they are in [0, 1).
 *         <p>Example:
 *         <pre>
 *             x = new Vector3d(0.4, -0.3, 2.0);
 *             Range.wrap(x);
 *             // x is now (0.4, 0.7, 0.0).
 *         </pre>
 *     </li>
 * </ol>
 */
class Range {

    /**
     * Wraps the coordinates of <code>x</code> to [0, 1).
     * <p>Coordinates are modified by repeatedly adding or subtracting 1 until they are in [0, 1).
     * This will make the algorithm take longer for coordinates that are further away from [0, 1).
     *
     * @param x vector with coordinates in (-inf, +inf)
     */
    public static void wrap(Vector3d x) {
        x.x = wrap(x.x);
        x.y = wrap(x.y);
        x.z = 0;  //todo 3D
    }

    private static double wrap(double value) {
        if (value < 0) {
            do {
                value += 1;
            } while (value < 0);
            return value;
        }
        while (value >= 1) {
            value -= 1;
        }
        return value;
    }

    /**
     * Assumes that the coordinates of <code>x</code> are in (-1, 1),
     * so that the wrapping algorithm can be simplified.
     * This is the case for connections between points
     * with coordinates in [0, 1).
     * Performs a single wind wrap on the interval [-0.5, 0.5).
     *
     * @param x vector with coordinates in (-1, 1)
     */
    public static void wrapConnection(Vector3d x) {
        x.x = wrapConnection(x.x);
        x.y = wrapConnection(x.y);
        x.z = 0;  //todo 3D
    }

    private static double wrapConnection(double value) {
        if (value < -0.5) {
            return value + 1;
        } else if (value >= 0.5) {
            return value - 1;
        }
        return value;
    }

    public static void clamp(Vector3d x) {
        x.x = clamp(x.x);
        x.y = clamp(x.y);
        x.z = 0;  // todo 3D
    }

    private static double clamp(double val) {
        if (val < 0) {
            return 0;
        } else if (val > 1) {
            return 1;
        }
        return val;
    }
}
