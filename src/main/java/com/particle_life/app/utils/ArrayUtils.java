package com.particle_life.app.utils;

public final class ArrayUtils {

    public static void swap(Object[] array, int i, int j) {//todo move to utility class
        Object h = array[i];
        array[i] = array[j];
        array[j] = h;
    }

    /**
     * Returns the first index <code>i</code>, where <code>a[i] < b[i]</code>, or -1.
     * Arrays must be of the same size.
     */
    public static int findFirstIndexWithLess(int[] a, int[] b) {
        assert a.length == b.length;
        for (int i = 0; i < a.length; i++) {
            if (a[i] < b[i]) {
                return i;
            }
        }
        return -1;
    }
}
