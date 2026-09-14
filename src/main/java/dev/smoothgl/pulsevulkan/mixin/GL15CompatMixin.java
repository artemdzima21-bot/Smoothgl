package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.PulseBufferFallback;
import org.lwjgl.opengl.GL15;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

@Mixin(value = GL15.class, priority = 900)
public abstract class GL15CompatMixin {
    @Inject(method = "glBindBuffer(II)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bindBuffer(int target, int buffer, CallbackInfo ci) {
        if (!PulseBufferFallback.handles(target)) return;
        PulseBufferFallback.bind(target, buffer);
        ci.cancel();
    }

    @Inject(method = "glBufferData(ILjava/nio/ByteBuffer;I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bufferDataBytes(int target, ByteBuffer data, int usage, CallbackInfo ci) { if (PulseBufferFallback.handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(ILjava/nio/ShortBuffer;I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bufferDataShorts(int target, ShortBuffer data, int usage, CallbackInfo ci) { if (PulseBufferFallback.handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(ILjava/nio/IntBuffer;I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bufferDataInts(int target, IntBuffer data, int usage, CallbackInfo ci) { if (PulseBufferFallback.handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(ILjava/nio/LongBuffer;I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bufferDataLongs(int target, LongBuffer data, int usage, CallbackInfo ci) { if (PulseBufferFallback.handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(ILjava/nio/FloatBuffer;I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bufferDataFloats(int target, FloatBuffer data, int usage, CallbackInfo ci) { if (PulseBufferFallback.handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(ILjava/nio/DoubleBuffer;I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bufferDataDoubles(int target, DoubleBuffer data, int usage, CallbackInfo ci) { if (PulseBufferFallback.handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }

    @Inject(method = "glBufferData(I[SI)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bufferDataShortArray(int target, short[] data, int usage, CallbackInfo ci) { if (PulseBufferFallback.handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(I[II)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bufferDataIntArray(int target, int[] data, int usage, CallbackInfo ci) { if (PulseBufferFallback.handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(I[JI)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bufferDataLongArray(int target, long[] data, int usage, CallbackInfo ci) { if (PulseBufferFallback.handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(I[FI)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bufferDataFloatArray(int target, float[] data, int usage, CallbackInfo ci) { if (PulseBufferFallback.handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(I[DI)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bufferDataDoubleArray(int target, double[] data, int usage, CallbackInfo ci) { if (PulseBufferFallback.handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }

    @Inject(method = "glBufferData(IJI)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bufferDataSize(int target, long size, int usage, CallbackInfo ci) { if (PulseBufferFallback.handles(target)) { PulseBufferFallback.bufferData(target, size, usage); ci.cancel(); } }

    @Inject(method = "glMapBuffer(II)Ljava/nio/ByteBuffer;", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$mapBuffer(int target, int access, CallbackInfoReturnable<ByteBuffer> cir) { if (PulseBufferFallback.handles(target)) cir.setReturnValue(PulseBufferFallback.mapBuffer(target)); }
    @Inject(method = "glMapBuffer(IIJLjava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$mapBufferSized(int target, int access, long length, ByteBuffer oldBuffer, CallbackInfoReturnable<ByteBuffer> cir) { if (PulseBufferFallback.handles(target)) cir.setReturnValue(PulseBufferFallback.mapBuffer(target)); }
    @Inject(method = "glUnmapBuffer(I)Z", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$unmapBuffer(int target, CallbackInfoReturnable<Boolean> cir) { if (PulseBufferFallback.handles(target)) cir.setReturnValue(true); }
    @Inject(method = "glDeleteBuffers(I)V", at = @At("HEAD"), remap = false, require = 0)
    private static void smoothgl$deleteBuffer(int buffer, CallbackInfo ci) { PulseBufferFallback.delete(buffer); }
}
