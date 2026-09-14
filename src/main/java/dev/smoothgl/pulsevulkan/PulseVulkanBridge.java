package dev.smoothgl.pulsevulkan;

import net.fabricmc.api.ClientModInitializer;

public final class PulseVulkanBridge implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        boolean pulseDetected = PulseVisualsDetector.isPresent();
        boolean forced = Boolean.getBoolean("smoothgl.pulse.force");

        if (!pulseDetected && !forced) {
            PulseDiagnostics.infoOnce("inactive", "Pulse Visuals was not detected; compatibility hooks stay inactive");
            return;
        }

        if (!VulkanDispatch.bootstrap()) {
            System.err.println("[PulseVulkanBridge] Vulkan backend could not be linked; bridge is disabled");
            return;
        }

        PulseDiagnostics.infoOnce("ready", "Pulse Visuals -> VulkanMod bridge is active on Minecraft 1.21.4");
    }
}
