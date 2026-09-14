package dev.smoothgl.pulsevulkan;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * Raises LWJGL's thread-local native MemoryStack before Vulkan capability
 * discovery. LWJGL defaults to 64 KiB, which can be too small on systems that
 * expose many Vulkan device extensions (especially hybrid Intel + NVIDIA
 * laptops).
 */
public final class LwjglStackBootstrap implements PreLaunchEntrypoint {
    private static final String STACK_SIZE_PROPERTY = "org.lwjgl.system.stackSize";
    private static final int MIN_STACK_KIB = 512;
    private static volatile boolean reported;

    @Override
    public void onPreLaunch() {
        ensureConfigured();
    }

    public static void ensureConfigured() {
        String configured = System.getProperty(STACK_SIZE_PROPERTY);
        int current = parsePositiveInt(configured);

        if (current < MIN_STACK_KIB) {
            System.setProperty(STACK_SIZE_PROPERTY, Integer.toString(MIN_STACK_KIB));
            if (!reported) {
                reported = true;
                System.out.println("[PulseVulkanBridge] Raised LWJGL MemoryStack from "
                        + (current > 0 ? current + " KiB" : "default 64 KiB")
                        + " to " + MIN_STACK_KIB + " KiB for Vulkan device extension discovery");
            }
        }
    }

    private static int parsePositiveInt(String value) {
        if (value == null || value.isBlank()) return -1;
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
