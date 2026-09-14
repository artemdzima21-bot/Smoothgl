package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.PulseCallScope;
import dev.smoothgl.pulsevulkan.ShaderFallback;
import org.lwjgl.opengl.GL20C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Direct GL20C interception for LWJGL wrapper overloads that bypass GL20 mixins.
 * Only Pulse-originated calls are swallowed so vanilla/VulkanMod rendering stays intact.
 */
@Mixin(value = GL20C.class, priority = 910)
public abstract class GL20CCompatMixin {
    @Inject(method = "glUniform1fv(ILjava/nio/FloatBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform1fv(int location, FloatBuffer value, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        ShaderFallback.uniform(location, "float[" + value.remaining() + "]");
        ci.cancel();
    }

    @Inject(method = "glUniform2fv(ILjava/nio/FloatBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform2fv(int location, FloatBuffer value, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        ShaderFallback.uniform(location, "vec2[" + value.remaining() + "]");
        ci.cancel();
    }

    @Inject(method = "glUniform3fv(ILjava/nio/FloatBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform3fv(int location, FloatBuffer value, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        ShaderFallback.uniform(location, "vec3[" + value.remaining() + "]");
        ci.cancel();
    }

    @Inject(method = "glUniform4fv(ILjava/nio/FloatBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform4fv(int location, FloatBuffer value, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        ShaderFallback.uniform(location, "vec4[" + value.remaining() + "]");
        ci.cancel();
    }

    @Inject(method = "glUniform1iv(ILjava/nio/IntBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform1iv(int location, IntBuffer value, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        ShaderFallback.uniform(location, "int[" + value.remaining() + "]");
        ci.cancel();
    }

    @Inject(method = "glUniform2iv(ILjava/nio/IntBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform2iv(int location, IntBuffer value, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        ShaderFallback.uniform(location, "ivec2[" + value.remaining() + "]");
        ci.cancel();
    }

    @Inject(method = "glUniform3iv(ILjava/nio/IntBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform3iv(int location, IntBuffer value, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        ShaderFallback.uniform(location, "ivec3[" + value.remaining() + "]");
        ci.cancel();
    }

    @Inject(method = "glUniform4iv(ILjava/nio/IntBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform4iv(int location, IntBuffer value, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        ShaderFallback.uniform(location, "ivec4[" + value.remaining() + "]");
        ci.cancel();
    }

    @Inject(method = "glUniformMatrix2fv(IZLjava/nio/FloatBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$matrix2(int location, boolean transpose, FloatBuffer value, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        ShaderFallback.uniform(location, "mat2[" + value.remaining() + "] transpose=" + transpose);
        ci.cancel();
    }

    @Inject(method = "glUniformMatrix3fv(IZLjava/nio/FloatBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$matrix3(int location, boolean transpose, FloatBuffer value, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        ShaderFallback.uniform(location, "mat3[" + value.remaining() + "] transpose=" + transpose);
        ci.cancel();
    }

    @Inject(method = "glUniformMatrix4fv(IZLjava/nio/FloatBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$matrix4(int location, boolean transpose, FloatBuffer value, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        ShaderFallback.uniformMatrix4(location, transpose, value);
        ci.cancel();
    }

    @Inject(method = "glGetUniformLocation(ILjava/lang/CharSequence;)I", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$getUniformLocation(int program, CharSequence name, CallbackInfoReturnable<Integer> cir) {
        if (!PulseCallScope.isPulseCall()) return;
        cir.setReturnValue(ShaderFallback.uniformLocation(program, name));
    }

    @Inject(method = "glGetAttribLocation(ILjava/lang/CharSequence;)I", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$getAttribLocation(int program, CharSequence name, CallbackInfoReturnable<Integer> cir) {
        if (!PulseCallScope.isPulseCall()) return;
        cir.setReturnValue(ShaderFallback.attribLocation(program, name));
    }
}
