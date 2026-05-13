package ru.ruskonnect.smoothgl.pacing;

import org.lwjgl.opengl.GL32;
import ru.ruskonnect.smoothgl.SmoothGL;
import ru.ruskonnect.smoothgl.SmoothGLClient;
import ru.ruskonnect.smoothgl.profiler.FrameBreakdown;
import ru.ruskonnect.smoothgl.profiler.StutterProfiler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GPU frame pacing via {@code GL_ARB_sync} fences.
 *
 * <p><b>The problem.</b> By default the OpenGL driver allows the CPU to
 * submit 2–3 frames ahead of what the GPU has actually finished rendering.
 * When the CPU is faster than the GPU (or vice versa), this manifests as:</p>
 *
 * <ul>
 *   <li>Input lag (your click is rendered 2–3 frames later);</li>
 *   <li>Uneven frame pacing — the famous "OpenGL gives you a new frame
 *       while the previous one isn't done yet" issue, where wall-clock
 *       deltas between displayed frames are not uniform even at locked FPS;</li>
 *   <li>Spiky CPU-side frame times, because every Nth submission stalls
 *       on the driver's internal queue.</li>
 * </ul>
 *
 * <p><b>The fix.</b> After every {@code SwapBuffers}, we insert a GPU
 * fence. On the next frame, before doing any GL work, we wait on the
 * fence from {@code N} frames ago. This caps frames-in-flight to a known
 * value (1 by default = lowest latency, evenly paced).</p>
 *
 * <p>Unlike {@code glFinish()}, which forces the entire pipeline to
 * drain every frame and kills throughput, this only enforces a soft
 * upper bound on how far CPU can run ahead of GPU.</p>
 *
 * <p>If the platform/driver doesn't support {@code GL_ARB_sync}
 * (essentially every GL 3.2+ driver does, but we still guard), the pacer
 * disables itself silently.</p>
 */
public final class FramePacer {

    /** Hard cap to avoid runaway memory if config is abused. */
    private static final int MAX_PIPELINE_DEPTH = 4;
    /** Wait timeout in ns for {@code glClientWaitSync}. 100 ms — generous; in practice waits ~0–8 ms. */
    private static final long WAIT_TIMEOUT_NS = 100_000_000L;

    private final Deque<Long> pendingFences = new ArrayDeque<>();
    private final AtomicLong waitCount = new AtomicLong();
    private final AtomicLong waitTotalNs = new AtomicLong();
    private final AtomicLong waitMaxNs = new AtomicLong();
    private volatile int maxFramesInFlight;
    private volatile boolean disabled;

    public FramePacer(int maxFramesInFlight) {
        this.maxFramesInFlight = clamp(maxFramesInFlight);
    }

    public void setMaxFramesInFlight(int v) {
        this.maxFramesInFlight = clamp(v);
    }

    public int maxFramesInFlight() { return maxFramesInFlight; }

    /**
     * Called immediately AFTER {@code glfwSwapBuffers} on the render thread
     * (so GL context is current). We:
     * <ol>
     *   <li>Insert a fence for THIS frame's submission;</li>
     *   <li>If the queue exceeds the cap, block on the oldest fence —
     *       that's the moment we throttle CPU to GPU's pace.</li>
     * </ol>
     */
    public void afterSwapBuffers() {
        if (disabled) return;
        long fence;
        try {
            fence = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        } catch (Throwable t) {
            disable("glFenceSync threw: " + t);
            return;
        }
        if (fence == 0L) {
            // Driver returned NULL sync — bail out, can't pace safely.
            disable("glFenceSync returned 0");
            return;
        }
        pendingFences.addLast(fence);

        while (pendingFences.size() > maxFramesInFlight) {
            long oldest = pendingFences.pollFirst();
            waitOn(oldest);
            try {
                GL32.glDeleteSync(oldest);
            } catch (Throwable ignored) {}
        }
    }

    private void waitOn(long fence) {
        long start = System.nanoTime();
        // Flush so the GPU is actually told about the fence; without
        // GL_SYNC_FLUSH_COMMANDS_BIT the wait could deadlock if nothing
        // forces a flush before the wait.
        int result;
        try {
            result = GL32.glClientWaitSync(fence, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, WAIT_TIMEOUT_NS);
        } catch (Throwable t) {
            disable("glClientWaitSync threw: " + t);
            return;
        }
        long elapsed = System.nanoTime() - start;
        waitCount.incrementAndGet();
        waitTotalNs.addAndGet(elapsed);
        // Report into the per-frame breakdown so /smoothgl breakdown
        // shows how much of the frame was spent waiting on the GPU.
        StutterProfiler p = SmoothGLClient.profiler();
        if (p != null) p.breakdown().addNs(FrameBreakdown.Stage.PACER_WAIT, elapsed);
        // Track max with simple CAS-loop.
        long curMax;
        do {
            curMax = waitMaxNs.get();
            if (elapsed <= curMax) break;
        } while (!waitMaxNs.compareAndSet(curMax, elapsed));

        if (result == GL32.GL_TIMEOUT_EXPIRED) {
            // Very unlikely under normal load — would mean GPU is hung for >100ms.
            SmoothGL.LOGGER.warn("[pacer] GPU fence wait timed out (>{}ms)", WAIT_TIMEOUT_NS / 1_000_000L);
        }
    }

    private void disable(String reason) {
        if (disabled) return;
        disabled = true;
        SmoothGL.LOGGER.warn("[pacer] disabled: {}", reason);
        // Drop any leftover fences without waiting.
        for (Long f : pendingFences) {
            try { GL32.glDeleteSync(f); } catch (Throwable ignored) {}
        }
        pendingFences.clear();
    }

    public boolean isDisabled() { return disabled; }
    public long waitCount() { return waitCount.get(); }
    public double avgWaitMs() {
        long n = waitCount.get();
        if (n == 0) return 0.0;
        return (waitTotalNs.get() / 1_000_000.0) / n;
    }
    public double maxWaitMs() { return waitMaxNs.get() / 1_000_000.0; }

    private static int clamp(int v) {
        if (v < 1) return 1;
        if (v > MAX_PIPELINE_DEPTH) return MAX_PIPELINE_DEPTH;
        return v;
    }
}
