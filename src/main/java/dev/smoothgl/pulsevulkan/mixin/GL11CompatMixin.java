package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.GlIntegerQueryFallback;
import dev.smoothgl.pulsevulkan.PulseCallScope;
import dev.smoothgl.pulsevulkan.PulseDiagnostics;
import dev.smoothgl.pulsevulkan.PulseNativeDraw;
import dev.smoothgl.pulsevulkan.PulseRenderApi;
import dev.smoothgl.pulsevulkan.ShaderFallback;
import dev.smoothgl.pulsevulkan.VulkanGlCompat;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.IntBuffer;

@Mixin(value = GL11.class, priority = 900)
public abstract class GL11CompatMixin {
    private static final int GL_CULL_FACE = 0x0B44;
    private static final int GL_DEPTH_TEST = 0x0B71;
    private static final int GL_BLEND = 0x0BE2;
    private static final int GL_VENDOR = 0x1F00;
    private static final int GL_RENDERER = 0x1F01;
    private static final int GL_VERSION = 0x1F02;
    private static final int GL_SHADING_LANGUAGE_VERSION = 0x8B8C;

    @Inject(method = "glEnable(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$enable(int cap, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        switch (cap) {
            case GL_BLEND -> PulseRenderApi.enableBlend();
            case GL_DEPTH_TEST -> PulseRenderApi.enableDepth();
            case GL_CULL_FACE -> PulseRenderApi.enableCull();
            default -> { return; }
        }
        ci.cancel();
    }

    @Inject(method = "glDisable(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$disable(int cap, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        switch (cap) {
            case GL_BLEND -> PulseRenderApi.disableBlend();
            case GL_DEPTH_TEST -> PulseRenderApi.disableDepth();
            case GL_CULL_FACE -> PulseRenderApi.disableCull();
            default -> { return; }
        }
        ci.cancel();
    }

    @Inject(method = "glBlendFunc(II)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$blendFunc(int src, int dst, CallbackInfo ci) { if (PulseCallScope.isPulseCall()) { PulseRenderApi.blendFunc(src, dst); ci.cancel(); } }
    @Inject(method = "glDepthFunc(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$depthFunc(int func, CallbackInfo ci) { if (PulseCallScope.isPulseCall()) { VulkanGlCompat.depthFunc(func); ci.cancel(); } }
    @Inject(method = "glColorMask(ZZZZ)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$colorMask(boolean red, boolean green, boolean blue, boolean alpha, CallbackInfo ci) { if (PulseCallScope.isPulseCall()) { VulkanGlCompat.colorMask(red, green, blue, alpha); ci.cancel(); } }

    @Inject(method = "glGetInteger(I)I", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$getInteger(int pname, CallbackInfoReturnable<Integer> cir) { if (PulseCallScope.isPulseCall()) cir.setReturnValue(GlIntegerQueryFallback.scalar(pname)); }
    @Inject(method = "glGetIntegerv(I[I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$getIntegersArray(int pname, int[] params, CallbackInfo ci) { if (PulseCallScope.isPulseCall()) { GlIntegerQueryFallback.fill(pname, params); ci.cancel(); } }
    @Inject(method = "glGetIntegerv(ILjava/nio/IntBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$getIntegersBuffer(int pname, IntBuffer params, CallbackInfo ci) { if (PulseCallScope.isPulseCall()) { GlIntegerQueryFallback.fill(pname, params); ci.cancel(); } }

    @Inject(method = "glGetString(I)Ljava/lang/String;", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$getString(int name, CallbackInfoReturnable<String> cir) {
        if (!PulseCallScope.isPulseCall()) return;
        switch (name) {
            case GL_VENDOR -> cir.setReturnValue("VulkanMod / SmoothGL");
            case GL_RENDERER -> cir.setReturnValue("Vulkan renderer via VulkanMod");
            case GL_VERSION -> cir.setReturnValue("4.6 SmoothGL compatibility");
            case GL_SHADING_LANGUAGE_VERSION -> cir.setReturnValue("4.60 compatibility fallback");
            default -> { return; }
        }
    }

    @Inject(method = "glDrawArrays(III)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$drawArrays(int mode, int first, int count, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        if (!PulseNativeDraw.tryDrawArrays(mode, first, count)) {
            ShaderFallback.unsupportedDraw("glDrawArrays(mode=" + mode + ", count=" + count + ")");
        }
        ci.cancel();
    }

    @Inject(method = "glDrawElements(IIIJ)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$drawElements(int mode, int count, int type, long indices, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        ShaderFallback.unsupportedDraw("glDrawElements(mode=" + mode + ", count=" + count + ")");
        ci.cancel();
    }

    @Inject(method = "glReadBuffer(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$readBuffer(int mode, CallbackInfo ci) { if (PulseCallScope.isPulseCall()) { PulseDiagnostics.fallback("glReadBuffer handled as Vulkan compatibility no-op"); ci.cancel(); } }
}
