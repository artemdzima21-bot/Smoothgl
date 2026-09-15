package dev.smoothgl.pulsevulkan;

import net.fabricmc.api.ClientModInitializer;

public final class PulseVulkanBridge implements ClientModInitializer {
    private static final String BUILD_ID = "0.4.0-pulse-shader-color";

    @Override
    public void onInitializeClient() {
        PulseDiagnostics.infoOnce("build-id", "Bridge build " + BUILD_ID);

        boolean pulseDetected = PulseVisualsDetector.isPresent();
        boolean forced = Boolean.getBoolean("smoothgl.pulse.force");

        if (!pulseDetected && !forced) {
            PulseDiagnostics.infoOnce("inactive", "Pulse Visuals was not detected; compatibility hooks stay inactive");
            return;
        }

        PulseDiagnostics.init();

        if (!VulkanDispatch.bootstrap()) {
            PulseDiagnostics.unsupported("Vulkan backend bootstrap failed; bridge could not link VulkanMod 0.5.4 backend");
            PulseDiagnostics.flushReport();
            System.err.println("[PulseVulkanBridge] Vulkan backend could not be linked; bridge is disabled");
            return;
        }

        PulseDiagnostics.infoOnce("ready", "Pulse Visuals -> VulkanMod bridge is active on Minecraft 1.21.4");
    }
}
