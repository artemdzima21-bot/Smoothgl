package ru.ruskonnect.smoothgl.jit;

import ru.ruskonnect.smoothgl.SmoothGL;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Triggers HotSpot tier-up on the hot render-path classes BEFORE they are
 * called for the first time in the actual render loop.
 *
 * <p>Why this matters: vanilla MC's first ~10-30 seconds after world load
 * are full of micro-stutters caused by C2 compiling methods that suddenly
 * see hot loops (matrix math, vertex format encode, frustum culling, etc.).
 * If we run those methods in tight loops on a background thread early,
 * HotSpot will tier them up before the user is in-game.</p>
 *
 * <p>This is NOT a hack — it just makes the JIT do its job earlier.
 * If the warmup loop fails (class not found in some MC version), we log
 * and skip; we never block the game thread.</p>
 */
public final class JitWarmup {

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    /** Class names that benefit most from early compilation in the OpenGL pipeline. */
    private static final String[] WARMUP_CLASSES = {
        "org.joml.Matrix4f",
        "org.joml.Matrix3f",
        "org.joml.Vector3f",
        "org.joml.Vector4f",
        "org.joml.Quaternionf",
        "net.minecraft.client.util.math.MatrixStack",
        "net.minecraft.client.render.BufferBuilder",
        "net.minecraft.client.render.VertexFormat",
        "net.minecraft.client.render.VertexFormatElement",
        "net.minecraft.client.render.Frustum",
        "net.minecraft.util.math.MathHelper",
        "net.minecraft.util.math.Box",
        "net.minecraft.util.math.Vec3d",
        "com.mojang.blaze3d.systems.RenderSystem",
        "com.mojang.blaze3d.platform.GlStateManager"
    };

    public static void scheduleAsync() {
        if (!STARTED.compareAndSet(false, true)) return;
        Thread t = new Thread(JitWarmup::runWarmup, "SmoothGL-JIT-Warmup");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private static void runWarmup() {
        long start = System.nanoTime();
        int loaded = 0;
        for (String cls : WARMUP_CLASSES) {
            try {
                Class.forName(cls, true, JitWarmup.class.getClassLoader());
                loaded++;
            } catch (Throwable ignored) {
                // Class layout may differ across MC/yarn versions — skip silently.
            }
        }

        // Hot loop on JOML to push Matrix4f / Vector3f past the C2 threshold (~10k invocations).
        try {
            org.joml.Matrix4f m = new org.joml.Matrix4f();
            org.joml.Vector3f v = new org.joml.Vector3f(1f, 2f, 3f);
            float acc = 0f;
            for (int i = 0; i < 20_000; i++) {
                m.identity()
                 .translate(i * 0.001f, i * 0.002f, i * 0.003f)
                 .rotateY(i * 0.01f)
                 .scale(1.0f + (i % 7) * 0.001f);
                m.transformPosition(v);
                acc += v.x + v.y + v.z;
            }
            // Prevent dead-code elimination.
            if (Float.isNaN(acc)) SmoothGL.LOGGER.info("warmup nan");
        } catch (Throwable t) {
            SmoothGL.LOGGER.debug("JOML warmup skipped: {}", t.toString());
        }

        long ms = (System.nanoTime() - start) / 1_000_000L;
        SmoothGL.LOGGER.info("JIT warmup complete: {}/{} classes pre-loaded in {}ms",
            loaded, WARMUP_CLASSES.length, ms);
    }
}
