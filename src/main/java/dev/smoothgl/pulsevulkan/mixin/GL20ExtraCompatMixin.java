package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.PulseCallScope;
import dev.smoothgl.pulsevulkan.PulseDiagnostics;
import dev.smoothgl.pulsevulkan.ShaderFallback;
import dev.smoothgl.pulsevulkan.VulkanGlCompat;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

@Mixin(value = GL20.class, priority = 899)
public abstract class GL20ExtraCompatMixin {
    private static boolean smoothgl$pulse() { return PulseCallScope.isPulseCall(); }

    @Inject(method = "glUniform2i(III)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform2i(int location, int x, int y, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformInts(location, x, y); ci.cancel(); } }
    @Inject(method = "glUniform3i(IIII)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform3i(int location, int x, int y, int z, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformInts(location, x, y, z); ci.cancel(); } }
    @Inject(method = "glUniform4i(IIIII)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform4i(int location, int x, int y, int z, int w, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformInts(location, x, y, z, w); ci.cancel(); } }

    @Inject(method = "glUniform1fv(ILjava/nio/FloatBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform1fv(int location, FloatBuffer value, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformFloats(location, value); ci.cancel(); } }
    @Inject(method = "glUniform2fv(ILjava/nio/FloatBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform2fv(int location, FloatBuffer value, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformFloats(location, value); ci.cancel(); } }
    @Inject(method = "glUniform3fv(ILjava/nio/FloatBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform3fv(int location, FloatBuffer value, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformFloats(location, value); ci.cancel(); } }
    @Inject(method = "glUniform4fv(ILjava/nio/FloatBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform4fv(int location, FloatBuffer value, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformFloats(location, value); ci.cancel(); } }

    @Inject(method = "glUniform1iv(ILjava/nio/IntBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform1iv(int location, IntBuffer value, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformInts(location, value); ci.cancel(); } }
    @Inject(method = "glUniform2iv(ILjava/nio/IntBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform2iv(int location, IntBuffer value, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformInts(location, value); ci.cancel(); } }
    @Inject(method = "glUniform3iv(ILjava/nio/IntBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform3iv(int location, IntBuffer value, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformInts(location, value); ci.cancel(); } }
    @Inject(method = "glUniform4iv(ILjava/nio/IntBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform4iv(int location, IntBuffer value, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformInts(location, value); ci.cancel(); } }

    @Inject(method = "glUniformMatrix2fv(IZLjava/nio/FloatBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$matrix2(int location, boolean transpose, FloatBuffer value, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformMatrix(location, 2, transpose, value); ci.cancel(); } }
    @Inject(method = "glUniformMatrix3fv(IZLjava/nio/FloatBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$matrix3(int location, boolean transpose, FloatBuffer value, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformMatrix(location, 3, transpose, value); ci.cancel(); } }

    @Inject(method = "glDrawBuffers(Ljava/nio/IntBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$drawBuffers(IntBuffer buffers, CallbackInfo ci) { if (smoothgl$pulse()) { PulseDiagnostics.fallback("glDrawBuffers uses VulkanMod attachment routing"); ci.cancel(); } }

    @Inject(method = "glBlendEquationSeparate(II)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$blendEquationSeparate(int rgb, int alpha, CallbackInfo ci) {
        if (!smoothgl$pulse()) return;
        if (rgb == alpha) {
            VulkanGlCompat.blendEquation(rgb);
        } else {
            VulkanGlCompat.blendEquation(rgb);
            PulseDiagnostics.infoOnce("blend-equation-separate-different",
                    "Pulse requested different RGB/alpha blend equations; VulkanMod 0.5.4 exposes one blend op, using RGB op");
        }
        ci.cancel();
    }
}
