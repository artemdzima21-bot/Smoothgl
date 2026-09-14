package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.PulseCallScope;
import dev.smoothgl.pulsevulkan.PulseDiagnostics;
import dev.smoothgl.pulsevulkan.ShaderFallback;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pulse-only interception for LWJGL GL20 primitive-array uniform overloads. */
@Mixin(value = GL20.class, priority = 950)
public abstract class GL20ArrayCompatMixin {
    private static boolean smoothgl$pulse() { return PulseCallScope.isPulseCall(); }
    private static void smoothgl$mark() { PulseDiagnostics.infoOnce("gl20-array-uniform-fallback", "GL20 primitive-array uniform compatibility enabled"); }

    @Inject(method = "glUniform1fv(I[F)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform1fv(int location, float[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniform(location, "float[" + (value == null ? 0 : value.length) + "]"); ci.cancel(); } }
    @Inject(method = "glUniform2fv(I[F)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform2fv(int location, float[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniform(location, "vec2[" + (value == null ? 0 : value.length) + "]"); ci.cancel(); } }
    @Inject(method = "glUniform3fv(I[F)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform3fv(int location, float[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniform(location, "vec3[" + (value == null ? 0 : value.length) + "]"); ci.cancel(); } }
    @Inject(method = "glUniform4fv(I[F)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform4fv(int location, float[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniform(location, "vec4[" + (value == null ? 0 : value.length) + "]"); ci.cancel(); } }
    @Inject(method = "glUniform1iv(I[I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform1iv(int location, int[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniform(location, "int[" + (value == null ? 0 : value.length) + "]"); ci.cancel(); } }
    @Inject(method = "glUniform2iv(I[I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform2iv(int location, int[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniform(location, "ivec2[" + (value == null ? 0 : value.length) + "]"); ci.cancel(); } }
    @Inject(method = "glUniform3iv(I[I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform3iv(int location, int[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniform(location, "ivec3[" + (value == null ? 0 : value.length) + "]"); ci.cancel(); } }
    @Inject(method = "glUniform4iv(I[I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform4iv(int location, int[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniform(location, "ivec4[" + (value == null ? 0 : value.length) + "]"); ci.cancel(); } }
    @Inject(method = "glUniformMatrix2fv(IZ[F)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$matrix2(int location, boolean transpose, float[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniform(location, "mat2[" + (value == null ? 0 : value.length) + "] transpose=" + transpose); ci.cancel(); } }
    @Inject(method = "glUniformMatrix3fv(IZ[F)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$matrix3(int location, boolean transpose, float[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniform(location, "mat3[" + (value == null ? 0 : value.length) + "] transpose=" + transpose); ci.cancel(); } }
    @Inject(method = "glUniformMatrix4fv(IZ[F)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$matrix4(int location, boolean transpose, float[] value, CallbackInfo ci) { if (smoothgl$pulse()) { smoothgl$mark(); ShaderFallback.uniformMatrix4(location, transpose, value); ci.cancel(); } }
}
