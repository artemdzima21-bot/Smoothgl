package ru.ruskonnect.smoothgl.buffers;

import ru.ruskonnect.smoothgl.SmoothGL;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Allocates and touches a chunk of direct off-heap memory at startup.
 *
 * <p>The first time a Minecraft client allocates a large direct
 * {@link ByteBuffer} (typically when chunk meshing kicks in), the OS may
 * have to zero-fill and commit fresh pages, which shows up as a 5-50ms
 * stall on the main thread. By forcing the OS to commit a sizable arena
 * up-front on a background thread, we eat that cost before it can hit
 * the render loop.</p>
 *
 * <p>The allocation is intentionally retained as a static field so the
 * GC cannot reclaim it; otherwise the OS may decommit the pages and we'd
 * be back to square one.</p>
 */
public final class BufferPreTouch {

    @SuppressWarnings("unused") // retained on purpose
    private static ByteBuffer retained;

    public static void preTouchAsync(int mib) {
        if (mib <= 0) return;
        Thread t = new Thread(() -> preTouch(mib), "SmoothGL-PreTouch");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private static void preTouch(int mib) {
        long start = System.nanoTime();
        int bytes = mib * 1024 * 1024;
        ByteBuffer buf;
        try {
            buf = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
        } catch (OutOfMemoryError e) {
            SmoothGL.LOGGER.warn("Pre-touch failed: not enough direct memory for {} MiB", mib);
            return;
        }
        // Touch every OS page (4 KiB) to force commit.
        final int pageSize = 4096;
        for (int i = 0; i < bytes; i += pageSize) {
            buf.put(i, (byte) 0);
        }
        retained = buf;
        long ms = (System.nanoTime() - start) / 1_000_000L;
        SmoothGL.LOGGER.info("Pre-touched {} MiB of direct memory in {}ms", mib, ms);
    }
}
