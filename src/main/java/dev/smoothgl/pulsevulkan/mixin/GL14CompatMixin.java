package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.PulseCallScope;
import dev.smoothgl.pulsevulkan.PulseDiagnostics;
import dev.smoothgl.pulsevulkan.PulseRenderApi;
import dev.smoothgl.pulsevulkan.VulkanGlCompat;
import org.lwjgl.opengl.GL14;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GL14.class, priority = 900)
public abstract class GL14CompatMixin {
    @Inject(method = "glBlendFuncSeparate(IIII)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$blendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        PulseRenderApi.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        ci.cancel();
    }

    @Inject(method = "glBlendEquation(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$blendEquation(int mode, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        VulkanGlCompat.blendEquation(mode);
        ci.cancel();
    }

    @Inject(method = "glBlendColor(FFFF)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$blendColor(float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        PulseDiagnostics.infoOnce("blend-color-unsupported",
                "Pulse glBlendColor requested; VulkanMod 0.5.4 has no constant blend-color state in its public pipeline model");
        ci.cancel();
    }
}
