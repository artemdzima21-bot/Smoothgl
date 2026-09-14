package dev.smoothgl.pulsevulkan;

import net.fabricmc.api.ClientModInitializer;

public final class PulseVulkanBridge implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PulseVisualsDetector.isPresent();
        VulkanDispatch.bootstrap();
    }
}
