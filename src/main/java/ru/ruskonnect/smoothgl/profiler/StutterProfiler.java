package ru.ruskonnect.smoothgl.profiler;

import ru.ruskonnect.smoothgl.SmoothGL;
import ru.ruskonnect.smoothgl.config.SmoothConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/**
 * Per-frame stutter profiler.
 *
 * <p>Records main-thread render durations into a fixed-size ring buffer,
 * computes a rolling median, and flags frames that exceed
 * {@code max(medianMs * stutterRatio, stutterFloorMs)} as micro-stutters.</p>
 *
 * <p>Records the wall-clock duration only. We deliberately avoid expensive
 * per-frame stack traces; on a stutter event we capture the main-thread
 * stack ONCE so the cost of the profiler itself does not add stutter.</p>
 */
public final class StutterProfiler {

    private static final int WINDOW = 240; // ~4s @ 60fps
    private static final int MAX_EVENTS = 64;

    private final SmoothConfig config;
    private final FrameBreakdown breakdown = new FrameBreakdown();
    private final long[] frameNs = new long[WINDOW];
    private int frameCount;
    private int frameIdx;
    private long frameStartNs;

    private final Deque<StutterEvent> events = new ArrayDeque<>();

    private long totalFrames;
    private long totalStutters;

    public StutterProfiler(SmoothConfig config) {
        this.config = config;
    }

    public void onFrameStart() {
        frameStartNs = System.nanoTime();
        breakdown.onFrameStart();
        // Snapshot last frame's cull counters for stable HUD display.
        ru.ruskonnect.smoothgl.SmoothGLClient.cullStats().rotate();
    }

    public FrameBreakdown breakdown() { return breakdown; }

    public void onFrameEnd(Thread mainThread) {
        if (frameStartNs == 0L) return;
        long durNs = System.nanoTime() - frameStartNs;
        frameStartNs = 0L;
        breakdown.onFrameEnd();

        frameNs[frameIdx] = durNs;
        frameIdx = (frameIdx + 1) % WINDOW;
        if (frameCount < WINDOW) frameCount++;
        totalFrames++;

        // Need a meaningful sample before flagging stutters.
        if (frameCount < 30) return;

        double medianMs = currentMedianMs();
        double durMs = durNs / 1_000_000.0;
        double thresholdMs = Math.max(medianMs * config.stutterRatio, config.stutterFloorMs);

        if (durMs >= thresholdMs && durMs >= 4.0) {
            totalStutters++;
            recordEvent(durMs, medianMs, mainThread);
        }

        // Adaptive tuner: cheap, runs every frame but actually adjusts only every N frames.
        var tuner = ru.ruskonnect.smoothgl.SmoothGLClient.adaptiveTuner();
        if (tuner != null) tuner.onFrame();
    }

    private void recordEvent(double durMs, double medianMs, Thread mainThread) {
        StackTraceElement[] trace;
        try {
            trace = mainThread.getStackTrace();
        } catch (SecurityException e) {
            trace = new StackTraceElement[0];
        }
        StutterEvent ev = new StutterEvent(System.currentTimeMillis(), durMs, medianMs, trace);
        synchronized (events) {
            if (events.size() >= MAX_EVENTS) events.removeFirst();
            events.addLast(ev);
        }
        SmoothGL.LOGGER.warn("[stutter] frame {}ms (median {}ms) thread={}",
            String.format("%.2f", durMs),
            String.format("%.2f", medianMs),
            mainThread.getName());
    }

    public double currentMedianMs() {
        int n = frameCount;
        if (n == 0) return 0.0;
        long[] copy = Arrays.copyOf(frameNs, n);
        Arrays.sort(copy);
        long midNs = copy[n / 2];
        return midNs / 1_000_000.0;
    }

    public double currentP99Ms() {
        int n = frameCount;
        if (n == 0) return 0.0;
        long[] copy = Arrays.copyOf(frameNs, n);
        Arrays.sort(copy);
        int idx = Math.min(n - 1, (int) Math.ceil(n * 0.99) - 1);
        return copy[idx] / 1_000_000.0;
    }

    public long totalFrames() { return totalFrames; }
    public long totalStutters() { return totalStutters; }

    /**
     * Returns the most recent frame times in chronological order (oldest → newest).
     * Used by the telemetry graph. Reads into a caller-supplied array to avoid
     * per-call allocation in the render loop.
     *
     * @param out destination array; length determines how many samples to return
     * @return number of valid samples written (may be less than out.length on cold start)
     */
    public int copyRecentFrameMs(float[] out) {
        int n = Math.min(frameCount, out.length);
        // The ring buffer's newest slot is (frameIdx - 1) mod WINDOW. Walk backwards.
        for (int i = 0; i < n; i++) {
            int src = (frameIdx - 1 - i + WINDOW) % WINDOW;
            // Reverse so out[0] is oldest, out[n-1] is newest.
            out[n - 1 - i] = (float) (frameNs[src] / 1_000_000.0);
        }
        return n;
    }

    public List<StutterEvent> snapshotEvents() {
        synchronized (events) {
            List<StutterEvent> list = new ArrayList<>(events);
            list.sort(Comparator.comparingDouble((StutterEvent e) -> e.durationMs).reversed());
            return list;
        }
    }

    public record StutterEvent(long timestampMs, double durationMs, double medianMs, StackTraceElement[] mainStack) {}
}
