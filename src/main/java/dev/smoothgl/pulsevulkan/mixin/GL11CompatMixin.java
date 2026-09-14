package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.PulseDiagnostics;
import dev.smoothgl.pulsevulkan.PulseRenderApi;
import dev.smoothgl.pulsevulkan.ShaderFallback;
import dev.smoothgl.pulsevulkan.VulkanGlCompat;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GL11.class, priority = 900)
public abstract class GL11CompatMixin {
    private static final int GL_CULL_FACE = 0x0B44;
    private static final int GL_DEPTH_TEST = 0x0B71;
    private static final int GL_BLEND = 0x0BE2;

    @Inject(method = "glEnable(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$enable(int cap, CallbackInfo ci) {
        switch (cap) {
            case GL_BLEND -> PulseRenderApi.enableBlend();
            case GL_DEPTH_TEST -> PulseRenderApi.enableDepth();
            case GL_CULL_FACE -> PulseRenderApi.enableCull();
            default -> {
                return;
            }
        }
        ci.cancel();
    }

    @Inject(method = "glDisable(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$disable(int cap, CallbackInfo ci) {
        switch (cap) {
            case GL_BLEND -> PulseRenderApi.disableBlend();
            case GL_DEPTH_TEST -> PulseRenderApi.disableDepth();
            case GL_CULL_FACE -> PulseRenderApi.disableCull();
            default -> {
                return;
            }
        }
        ci.cancel();
    }

    @Inject(method = "glBlendFunc(II)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$blendFunc(int src, int dst, CallbackInfo ci) {
        PulseRenderApi.blendFunc(src, dst);
        ci.cancel();
    }

    @Inject(method = "glDepthFunc(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$depthFunc(int func, CallbackInfo ci) {
        VulkanGlCompat.depthFunc(func);
        ci.cancel();
    }

    @Inject(method = "glColorMask(ZZZZ)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$colorMask(boolean red, boolean green, boolean blue, boolean alpha, CallbackInfo ci) {
        VulkanGlCompat.colorMask(red, green, blue, alpha);
        ci.cancel();
    }

    @Inject(method = "glDrawArrays(III)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$drawArrays(int mode, int first, int count, CallbackInfo ci) {
        ShaderFallback.unsupportedDraw("glDrawArrays(mode=" + mode + ", count=" + count + ")");
        ci.cancel();
    }

    @Inject(method = "glDrawElements(IIIJ)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$drawElements(int mode, int count, int type, long indices, CallbackInfo ci) {
        ShaderFallback.unsupportedDraw("glDrawElements(mode=" + mode + ", count=" + count + ")");
        ci.cancel();
    }

    @Inject(method = "glReadBuffer(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$readBuffer(int mode, CallbackInfo ci) {
        PulseDiagnostics.fallback("glReadBuffer handled as Vulkan compatibility no-op");
        ci.cancel();
    }
}
