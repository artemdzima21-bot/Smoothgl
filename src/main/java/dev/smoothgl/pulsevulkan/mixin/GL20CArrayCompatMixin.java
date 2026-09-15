package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.PulseCallScope;
import dev.smoothgl.pulsevulkan.PulseDiagnostics;
import dev.smoothgl.pulsevulkan.ShaderFallback;
import org.lwjgl.opengl.GL20C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Last-line Pulse-only interception for GL20C primitive-array uniform overloads. */
@Mixin(value = GL20C.class, priority = 1250)
public abstract class GL20CArrayCompatMixin {
    private static boolean smoothgl$pulse() { return PulseCallScope.isPulseCall(); }
    private static void smoothgl$mark() { PulseDiagnostics.infoOnce("gl20c-array-uniform-fallback", "GL20C primitive-array uniform compatibility enabled with value capture"); }

    @Inject(method = "glUniform1fv(I[F)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform1fv(int location, float[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniformFloats(location, value == null ? new float[0] : value); ci.cancel(); } }
    @Inject(method = "glUniform2fv(I[F)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform2fv(int location, float[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniformFloats(location, value == null ? new float[0] : value); ci.cancel(); } }
    @Inject(method = "glUniform3fv(I[F)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform3fv(int location, float[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniformFloats(location, value == null ? new float[0] : value); ci.cancel(); } }
    @Inject(method = "glUniform4fv(I[F)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform4fv(int location, float[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniformFloats(location, value == null ? new float[0] : value); ci.cancel(); } }
    @Inject(method = "glUniform1iv(I[I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform1iv(int location, int[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniformInts(location, value == null ? new int[0] : value); ci.cancel(); } }
    @Inject(method = "glUniform2iv(I[I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform2iv(int location, int[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniformInts(location, value == null ? new int[0] : value); ci.cancel(); } }
    @Inject(method = "glUniform3iv(I[I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform3iv(int location, int[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniformInts(location, value == null ? new int[0] : value); ci.cancel(); } }
    @Inject(method = "glUniform4iv(I[I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform4iv(int location, int[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniformInts(location, value == null ? new int[0] : value); ci.cancel(); } }
    @Inject(method = "glUniformMatrix2fv(IZ[F)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$matrix2(int location, boolean transpose, float[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniformMatrix(location, 2, transpose, value == null ? new float[0] : value); ci.cancel(); } }
    @Inject(method = "glUniformMatrix3fv(IZ[F)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$matrix3(int location, boolean transpose, float[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniformMatrix(location, 3, transpose, value == null ? new float[0] : value); ci.cancel(); } }
    @Inject(method = "glUniformMatrix4fv(IZ[F)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$matrix4(int location, boolean transpose, float[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniformMatrix4(location, transpose, value == null ? new float[0] : value); ci.cancel(); } }
}
