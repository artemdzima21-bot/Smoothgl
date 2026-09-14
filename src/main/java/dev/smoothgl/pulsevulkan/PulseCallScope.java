package dev.smoothgl.pulsevulkan;

/**
 * Keeps invasive OpenGL compatibility fallbacks from affecting vanilla Minecraft,
 * VulkanMod or unrelated mods. The bridge should only swallow calls originating
 * from Pulse's own render path.
 */
public final class PulseCallScope {
    private static final StackWalker WALKER = StackWalker.getInstance();

    private PulseCallScope() {}

    public static boolean isPulseCall() {
        return WALKER.walk(frames -> frames
                .limit(32)
                .map(StackWalker.StackFrame::getClassName)
                .anyMatch(name -> name.startsWith("pulse.")));
    }
}
