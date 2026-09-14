package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.PulseDiagnostics;
import dev.smoothgl.pulsevulkan.PulseRenderApi;
import org.lwjgl.opengl.GL14;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GL14.class, priority = 900)
public abstract class GL14CompatMixin {
    @Inject(method = "glBlendFuncSeparate(IIII)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$blendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha, CallbackInfo ci) {
        PulseRenderApi.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        ci.cancel();
    }

    @Inject(method = "glBlendColor(FFFF)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$blendColor(float red, float green, float blue, float alpha, CallbackInfo ci) {
        PulseDiagnostics.fallback("glBlendColor is ignored by VulkanMod 0.5.4 fallback");
        ci.cancel();
    }
}
