package dev.smoothgl.pulsevulkan;

import java.lang.StackWalker.Option;

/**
 * Keeps invasive OpenGL compatibility fallbacks from affecting vanilla Minecraft,
 * VulkanMod or unrelated mods.
 *
 * Stack walking is only used to enter the Pulse path. Once a synthetic Pulse
 * program is bound, the render thread is marked active and hot uniform/draw/state
 * calls use an O(1) ThreadLocal check until glUseProgram(0).
 */
public final class PulseCallScope {
    private static final StackWalker WALKER = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private PulseCallScope() {}

    public static boolean isPulseCall() {
        if (ACTIVE.get()) return true;

        return WALKER.walk(frames -> frames
                .limit(12)
                .anyMatch(frame -> frame.getClassName().startsWith("pulse.")));
    }

    public static void setProgramActive(boolean active) {
        ACTIVE.set(active);
    }

    public static boolean isProgramActive() {
        return ACTIVE.get();
    }
}
