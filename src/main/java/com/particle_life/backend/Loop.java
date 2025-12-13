package com.particle_life.backend;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class for starting a thread that repeatedly calls a given method.<br>
 * Example:
 * <pre>
 * Loop loop = new Loop();
 * loop.start(dt -> {
 *     // do something expensive
 * });
 * </pre>
 * <p>
 * To start the thread, call {@link #start(Callback)}.<br>
 * To stop the thread, call {@link #stop(long millis)} or {@link #kill()}.<br>
 * To execute code synchronously with the loop, use {@link #enqueue(Runnable)}.
 * Example:
 * <pre>
 * loop.enqueue(() -> {
 *     // run code here that should not run
 *     // in parallel with the code in the loop
 * });
 * </pre>
 */
public class Loop {

    /**
     * Upper limit for time step, in seconds.
     * <p>If this is negative (e.g. -1.0), there will be no limit.
     * <p>This won't have any effect on the actual framerate returned by {@link #getActualDt()}.
     */
    public double maxDt = 1.0 / 20.0; // min. 20 fps

    /**
     * If this is <code>true</code>, the callback won't be called in the loop.
     */
    public boolean pause = false;
    private final Clock clock = new Clock(60);

    private Thread loopThread = null;
    private final AtomicBoolean loopShouldRun = new AtomicBoolean(false);

    private final LinkedBlockingDeque<Runnable> commandQueue = new LinkedBlockingDeque<>();
    private final AtomicReference<Runnable> once = new AtomicReference<>(null);

    /**
     * Will be invoked repeatedly by the loop started with {@link #start(Callback)}
     * until {@link #stop(long)} or {@link #kill()} is called.
     */
    public interface Callback {
        /**
         * Will be invoked repeatedly by the loop started with {@link #start(Callback)}
         * until {@link #stop(long)} is called.
         * The time for each iteration of the loop is measured and passed to the callback.
         *
         * @param dt The time passed in the iteration of the loop, limited by {@link #maxDt}.
         */
        void call(double dt);
    }

    /**
     * The passed command will be added to the queue and will be processed
     * in the next iteration of the loop thread.<br>
     * The commands will be executed in the order they were added via this method.
     *
     * @param cmd the command to be executed in the loop thread
     */
    public void enqueue(Runnable cmd) {
        //todo: debug print if some GUI elements spam commands
        commandQueue.addLast(cmd);
    }

    /**
     * The passed command will be executed in the next iteration of the loop thread.
     * If this method is called again before the next iteration of the loop thread,
     * the previous command will be replaced by this one.<br>
     * This is helpful if your loop may take longer, and you don't want expensive commands
     * to pile up in the meantime (as this could cause the loop to take even longer).
     *
     * @param cmd the command to be executed in the loop thread
     */
    public void doOnce(Runnable cmd) {
        once.set(cmd);
    }

    private void processCommandQueue() {
        Runnable cmd;
        while ((cmd = commandQueue.pollFirst()) != null) {
            cmd.run();
        }
    }

    public synchronized void start(Callback loop) {

        if (loopThread != null) throw new IllegalStateException("Loop thread didn't finish properly (wasn't null).");

        loopShouldRun.set(true);

        loopThread = new Thread(() -> {
            while (loopShouldRun.get()) {
                loop(loop);
            }
        });
        loopThread.start();
    }

    private void loop(Callback loop) {

        clock.tick();

        processCommandQueue();
        Runnable onceCommand = once.getAndSet(null);
        if (onceCommand != null) onceCommand.run();

        if (!pause) {
            loop.call(computeDt());
        }
    }

    /**
     * Blocks until the current iteration has finished, then stops the loop.
     * If the waiting takes longer than the given time, the loop won't be stopped
     * and everything continues just as before.
     * <p>
     * The return value indicates whether the loop could be stopped.
     * </p>
     * <p>
     * A timeout of 0 means to wait forever.
     * </p>
     *
     * @param millis the time to wait for the loop to finish in milliseconds
     * @return whether the loop could be stopped
     */
    public synchronized boolean stop(long millis) {
        if (loopThread == null) return true;  // already dead or never started
        if (!loopThread.isAlive()) { // already dead
            loopThread = null;
            return true;
        }
        boolean oldValue = loopShouldRun.getAndSet(false);
        try {
            loopThread.join(millis);  // A timeout of 0 means to wait forever.
        } catch (InterruptedException e) {
            // ignore
        }
        if (loopThread.isAlive()) {
            // iteration didn't finish in time
            loopShouldRun.set(oldValue); // pretend nothing happened
            return false;
        } else {
            // iteration finished in time
            loopThread = null;
            return true;
        }
    }

    /**
     * Blocks until the current iteration has finished, then stops the loop.
     * Shorthand for {@link #stop(long)} with a timeout of 0.
     *
     * @return
     */
    public synchronized boolean stop() {
        return stop(0);
    }

    /**
     * Aborts the loop immediately without waiting for the current iteration to finish.
     */
    public synchronized void kill() {
        if (loopThread == null) return;  // already dead or never started
        if (!loopThread.isAlive()) { // already dead
            loopThread = null;
            return;
        }
        loopShouldRun.set(false);
        loopThread.interrupt();
        loopThread = null;
    }

    private double computeDt() {
        double dt = clock.getDtMillis() / 1000.0;
        return Math.min(maxDt, dt);
    }

    /**
     * Returns how much time passed between the last two iterations of the loop, in seconds.
     * Unlike the value given to the callback in {@link #start(Callback)}, this value is not limited by {@link #maxDt}.
     * Note that therefore the return value of this method can be very small if <code>{@link #pause} == true</code>,
     * as there is no work to be done in the loop.
     *
     * @return how much time passed between the last two iterations of the loop, in seconds.
     */
    public double getActualDt() {
        return clock.getDtMillis() / 1000.0;
    }

    /**
     * Average framerate over the last couple of frames.
     * Unlike the value given to the callback in {@link #start(Callback)}, this value is not limited by {@link #maxDt}.
     * Note that therefore the return value of this method can be very high if <code>{@link #pause} == true</code>,
     * as there is no work to be done in the loop.
     *
     * @return average framerate in frames per second.
     * @see #getActualDt()
     */
    public double getAvgFramerate() {
        return clock.getAvgFramerate();
    }
}
