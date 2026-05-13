package ru.ruskonnect.smoothgl.profiler;

import java.util.Arrays;

/**
 * Per-stage frame time accumulator.
 *
 * <p>Driven by mixins that wrap well-known render stages (world, GUI)
 * and by {@link ru.ruskonnect.smoothgl.pacing.FramePacer} for the
 * pacer-wait stage. Times are accumulated within a single frame
 * (handles re-entry — e.g. nested render calls — via simple summation
 * of disjoint intervals), then snapshotted into a rolling window on
 * frame end.</p>
 *
 * <p>"Other" is computed at read time as
 * {@code totalFrameMs − (world + gui + pacerWait)} and represents
 * everything we don't measure: post-fx, screenshot capture, screen
 * (menus), buffer uploads, etc.</p>
 *
 * <p>Single-threaded — only touched from the render thread.</p>
 */
public final class FrameBreakdown {

    public enum Stage {
        WORLD,
        GUI,
        PACER_WAIT;

        static final Stage[] VALUES = values();
    }

    private static final int WINDOW = 240;

    private final long[] stageStartNs = new long[Stage.VALUES.length];
    private final int[]  stageDepth   = new int[Stage.VALUES.length]; // re-entry guard
    private final long[] frameAccumNs = new long[Stage.VALUES.length];

    private final long[][] window = new long[Stage.VALUES.length][WINDOW];
    private int frameIdx;
    private int frameCount;

    public void onFrameStart() {
        Arrays.fill(frameAccumNs, 0L);
        Arrays.fill(stageStartNs, 0L);
        Arrays.fill(stageDepth, 0);
    }

    public void stageStart(Stage s) {
        int i = s.ordinal();
        if (stageDepth[i]++ == 0) {
            stageStartNs[i] = System.nanoTime();
        }
    }

    public void stageEnd(Stage s) {
        int i = s.ordinal();
        if (stageDepth[i] == 0) return;
        if (--stageDepth[i] == 0) {
            long start = stageStartNs[i];
            if (start != 0L) {
                frameAccumNs[i] += System.nanoTime() - start;
                stageStartNs[i] = 0L;
            }
        }
    }

    /** Direct contribution from external sources (e.g. FramePacer reports its wait). */
    public void addNs(Stage s, long ns) {
        if (ns <= 0) return;
        frameAccumNs[s.ordinal()] += ns;
    }

    public void onFrameEnd() {
        for (Stage s : Stage.VALUES) {
            window[s.ordinal()][frameIdx] = frameAccumNs[s.ordinal()];
        }
        frameIdx = (frameIdx + 1) % WINDOW;
        if (frameCount < WINDOW) frameCount++;
    }

    public double avgMs(Stage s) {
        int n = frameCount;
        if (n == 0) return 0.0;
        long sum = 0L;
        long[] data = window[s.ordinal()];
        for (int i = 0; i < n; i++) sum += data[i];
        return (sum / (double) n) / 1_000_000.0;
    }

    public double maxMs(Stage s) {
        int n = frameCount;
        if (n == 0) return 0.0;
        long max = 0L;
        long[] data = window[s.ordinal()];
        for (int i = 0; i < n; i++) if (data[i] > max) max = data[i];
        return max / 1_000_000.0;
    }

    public int sampleCount() { return frameCount; }
}
