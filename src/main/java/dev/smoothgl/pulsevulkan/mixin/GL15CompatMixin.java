package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.PulseBufferFallback;
import org.lwjgl.opengl.GL15;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;

@Mixin(value = GL15.class, priority = 900)
public abstract class GL15CompatMixin {
    @Inject(method = "glBindBuffer(II)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bindBuffer(int target, int buffer, CallbackInfo ci) {
        if (!PulseBufferFallback.handles(target)) return;
        PulseBufferFallback.bind(target, buffer);
        ci.cancel();
    }

    @Inject(method = "glBufferData(ILjava/nio/ByteBuffer;I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bufferData(int target, ByteBuffer data, int usage, CallbackInfo ci) {
        if (!PulseBufferFallback.handles(target)) return;
        PulseBufferFallback.bufferData(target, data, usage);
        ci.cancel();
    }

    @Inject(method = "glBufferData(IJI)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bufferDataSize(int target, long size, int usage, CallbackInfo ci) {
        if (!PulseBufferFallback.handles(target)) return;
        PulseBufferFallback.bufferData(target, size, usage);
        ci.cancel();
    }

    @Inject(method = "glMapBuffer(II)Ljava/nio/ByteBuffer;", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$mapBuffer(int target, int access, CallbackInfoReturnable<ByteBuffer> cir) {
        if (!PulseBufferFallback.handles(target)) return;
        cir.setReturnValue(PulseBufferFallback.mapBuffer(target));
    }

    @Inject(method = "glMapBuffer(IIJLjava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$mapBufferSized(int target, int access, long length, ByteBuffer oldBuffer, CallbackInfoReturnable<ByteBuffer> cir) {
        if (!PulseBufferFallback.handles(target)) return;
        cir.setReturnValue(PulseBufferFallback.mapBuffer(target));
    }

    @Inject(method = "glUnmapBuffer(I)Z", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$unmapBuffer(int target, CallbackInfoReturnable<Boolean> cir) {
        if (!PulseBufferFallback.handles(target)) return;
        cir.setReturnValue(true);
    }

    @Inject(method = "glDeleteBuffers(I)V", at = @At("HEAD"), remap = false, require = 0)
    private static void smoothgl$deleteBuffer(int buffer, CallbackInfo ci) {
        PulseBufferFallback.delete(buffer);
    }
}
