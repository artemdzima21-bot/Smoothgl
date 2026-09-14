package dev.smoothgl.pulsevulkan.mixin;

import net.vulkanmod.vulkan.device.DeviceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DeviceManager.class, remap = false)
public abstract class DeviceManagerDiagnosticsMixin {
    @Inject(method = "pickPhysicalDevice", at = @At("TAIL"), remap = false)
    private static void smoothgl$reportSelectedGpu(CallbackInfo ci) {
        if (DeviceManager.device != null) {
            System.out.println("[SmoothGL/Vulkan] Selected GPU: " + DeviceManager.device.deviceName);
        }
    }
}
