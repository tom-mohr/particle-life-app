package com.particle_life.backend;

import java.util.Arrays;

public class Clock {

    private long inTime = -1;
    private final double[] lastTimes;
    private int currentTimeIndex = -1;
    private double dt = 0.0;

    /**
     * Average passed time over the last <code>n</code> times {@link #tick()} or {@link #in()} / {@link #out()} was called.
     */
    private double avgDt = 0;

    private double dtVariance = 0;

    /**
     * @param n over how many values should the average be calculated? (default is 20)
     */
    public Clock(int n) {
        lastTimes = new double[n];
        Arrays.fill(lastTimes, 0);
    }

    /**
     * Shortcut for out() and in().
     * Use this if you simply want to measure how much time passes between two tick() calls.
     */
    public void tick() {
        if (inTime != -1) {
            out();
        }
        in();
    }

    /**
     * Use in() and out() if you want to time certain intervals instead of your whole loop.
     */
    public void in() {
        inTime = System.nanoTime();
    }

    public void out() {

        if (inTime == -1) {
            // must call in() before out()
            throw new RuntimeException("Clock.out() was called even though Clock.in() was never called before");
        }

        dt = (System.nanoTime() - inTime) / 1000000.0;

        final int n = lastTimes.length;  // for convenience

        // step to next index in array
        currentTimeIndex++;
        if (currentTimeIndex >= n) {
            currentTimeIndex = 0;
        }

        // put value in array
        lastTimes[currentTimeIndex] = dt;

        // calc average and variance of array
        if (n < 2) {
            avgDt = lastTimes[0];
            dtVariance = 0;
        } else {
            double sum = 0;
            double squareSum = 0;
            for (double t : lastTimes) {
                sum += t;
                squareSum += t * t;
            }
            avgDt = sum / n;
            dtVariance = (squareSum - n * avgDt * avgDt) / (n - 1);
        }
    }

    /**
     * @return average passed time in milliseconds.
     */
    public double getAvgDtMillis() {
        return avgDt;
    }

    public double getAvgFramerate() {
        if (avgDt == 0) return 0.0;
        return 1000.0 / avgDt;
    }

    public double getStandardDeviation() {
        return Math.sqrt(dtVariance);
    }

    public double getDtMillis() {
        return dt;
    }

    public double getFramerate() {
        if (dt == 0) return 0.0;
        return 1000.0 / lastTimes[currentTimeIndex];
    }
}
