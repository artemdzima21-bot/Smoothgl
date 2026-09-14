package dev.smoothgl.pulsevulkan.mixin;

import net.vulkanmod.Initializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Initializer.class, remap = false)
public abstract class VulkanPerformanceMixin {
    private static final int DEFAULT_MAX_FPS_QUEUE = 4;

    @Inject(method = "onInitializeClient", at = @At("TAIL"), remap = false)
    private void smoothgl$applyPerformanceDefaults(CallbackInfo ci) {
        if (Boolean.getBoolean("smoothgl.vulkan.disableMaxFpsTuning")) {
            System.out.println("[SmoothGL/Vulkan] Max-FPS tuning disabled by JVM property");
            return;
        }

        int target = parseQueue(System.getProperty("smoothgl.vulkan.frameQueue"));
        if (target < 2 || target > 5) {
            target = DEFAULT_MAX_FPS_QUEUE;
        }

        int previous = Initializer.CONFIG.frameQueueSize;
        if (previous != target) {
            Initializer.CONFIG.frameQueueSize = target;
            System.out.println("[SmoothGL/Vulkan] Frame queue tuned from " + previous + " to " + target + " for maximum throughput");
        } else {
            System.out.println("[SmoothGL/Vulkan] Frame queue already set to " + target);
        }
    }

    private static int parseQueue(String value) {
        if (value == null || value.isBlank()) return -1;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
