package com.particle_life.app.utils;

public final class MathUtils {

    public static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        } else {
            return value;
        }
    }

    public static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        } else {
            return value;
        }
    }

    public static int modulo(int a, int b) {
        if (a < 0) {
            do {
                a += b;
            } while (a < 0);
            return a;
        } else if (a >= b) {
            do {
                a -= b;
            } while (a >= b);
            return a;
        }
        return a;
    }

    public static double lerp(double a, double b, double f) {
        return a + (b - a) * f;
    }

    /**
     * Returns <code>Math.round(value)</code> instead of <code>Math.floor(value)</code>
     * if <code>value</code> is closer to the next integer than <code>threshold</code>.
     *
     * @param value
     * @param threshold some positive value like 0.001
     * @return an integer
     */
    public static double tolerantFloor(double value, double threshold) {
        double x = Math.round(value);
        if (Math.abs(x - value) < threshold) {
            return x;
        }
        return Math.floor(value);
    }

    /**
     * See comment on {@link #tolerantFloor(double, double)}.
     *
     * @param value
     * @param threshold
     * @return
     */
    public static double tolerantCeil(double value, double threshold) {
        double x = Math.round(value);
        if (Math.abs(x - value) < threshold) return x;
        return Math.ceil(value);
    }
}
