package dev.smoothgl.pulsevulkan;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * Runs before Minecraft/LWJGL renderer initialization and increases the
 * thread-local LWJGL MemoryStack capacity used during Vulkan capability
 * discovery. LWJGL defaults to 64 KiB, which can be insufficient on systems
 * exposing many Vulkan device extensions (for example hybrid Intel + NVIDIA
 * laptops).
 */
public final class LwjglStackBootstrap implements PreLaunchEntrypoint {
    private static final String STACK_SIZE_PROPERTY = "org.lwjgl.system.stackSize";
    private static final int MIN_STACK_KIB = 512;

    @Override
    public void onPreLaunch() {
        String configured = System.getProperty(STACK_SIZE_PROPERTY);
        int current = parsePositiveInt(configured);

        if (current < MIN_STACK_KIB) {
            System.setProperty(STACK_SIZE_PROPERTY, Integer.toString(MIN_STACK_KIB));
            System.out.println("[PulseVulkanBridge] Raised LWJGL MemoryStack from "
                    + (current > 0 ? current + " KiB" : "default 64 KiB")
                    + " to " + MIN_STACK_KIB + " KiB for Vulkan device extension discovery");
        } else {
            System.out.println("[PulseVulkanBridge] LWJGL MemoryStack already configured to "
                    + current + " KiB");
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
