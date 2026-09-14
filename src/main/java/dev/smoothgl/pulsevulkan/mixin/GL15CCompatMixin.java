package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.PulseBufferFallback;
import dev.smoothgl.pulsevulkan.PulseCallScope;
import org.lwjgl.opengl.GL15C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

@Mixin(value = GL15C.class, priority = 1200)
public abstract class GL15CCompatMixin {
    private static boolean smoothgl$handles(int target) {
        return PulseCallScope.isPulseCall() && PulseBufferFallback.handles(target);
    }

    @Inject(method = "glBufferData(ILjava/nio/ByteBuffer;I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bytes(int target, ByteBuffer data, int usage, CallbackInfo ci) { if (smoothgl$handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(ILjava/nio/ShortBuffer;I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$shorts(int target, ShortBuffer data, int usage, CallbackInfo ci) { if (smoothgl$handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(ILjava/nio/IntBuffer;I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$ints(int target, IntBuffer data, int usage, CallbackInfo ci) { if (smoothgl$handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(ILjava/nio/LongBuffer;I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$longs(int target, LongBuffer data, int usage, CallbackInfo ci) { if (smoothgl$handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(ILjava/nio/FloatBuffer;I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$floats(int target, FloatBuffer data, int usage, CallbackInfo ci) { if (smoothgl$handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(ILjava/nio/DoubleBuffer;I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$doubles(int target, DoubleBuffer data, int usage, CallbackInfo ci) { if (smoothgl$handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(I[SI)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$shortArray(int target, short[] data, int usage, CallbackInfo ci) { if (smoothgl$handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(I[II)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$intArray(int target, int[] data, int usage, CallbackInfo ci) { if (smoothgl$handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(I[JI)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$longArray(int target, long[] data, int usage, CallbackInfo ci) { if (smoothgl$handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(I[FI)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$floatArray(int target, float[] data, int usage, CallbackInfo ci) { if (smoothgl$handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(I[DI)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$doubleArray(int target, double[] data, int usage, CallbackInfo ci) { if (smoothgl$handles(target)) { PulseBufferFallback.bufferData(target, data, usage); ci.cancel(); } }
    @Inject(method = "glBufferData(IJI)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$size(int target, long size, int usage, CallbackInfo ci) { if (smoothgl$handles(target)) { PulseBufferFallback.bufferData(target, size, usage); ci.cancel(); } }
}
