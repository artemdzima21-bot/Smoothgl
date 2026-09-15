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
    private static boolean smoothgl$pulse() { return PulseCallScope.isPulseCall(); }

    @Inject(method = "glUniform1i(II)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform1i(int location, int value, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformInts(location, value); ci.cancel(); } }
    @Inject(method = "glUniform2i(III)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform2i(int location, int x, int y, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformInts(location, x, y); ci.cancel(); } }
    @Inject(method = "glUniform3i(IIII)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform3i(int location, int x, int y, int z, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformInts(location, x, y, z); ci.cancel(); } }
    @Inject(method = "glUniform4i(IIIII)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform4i(int location, int x, int y, int z, int w, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformInts(location, x, y, z, w); ci.cancel(); } }

    @Inject(method = "glUniform1f(IF)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform1f(int location, float value, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformFloats(location, value); ci.cancel(); } }
    @Inject(method = "glUniform2f(IFF)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform2f(int location, float x, float y, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformFloats(location, x, y); ci.cancel(); } }
    @Inject(method = "glUniform3f(IFFF)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform3f(int location, float x, float y, float z, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformFloats(location, x, y, z); ci.cancel(); } }
    @Inject(method = "glUniform4f(IFFFF)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform4f(int location, float x, float y, float z, float w, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformFloats(location, x, y, z, w); ci.cancel(); } }

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
    @Inject(method = "glUniformMatrix4fv(IZLjava/nio/FloatBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$matrix4(int location, boolean transpose, FloatBuffer value, CallbackInfo ci) { if (smoothgl$pulse()) { ShaderFallback.uniformMatrix4(location, transpose, value); ci.cancel(); } }

    @Inject(method = "glGetUniformLocation(ILjava/lang/CharSequence;)I", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$getUniformLocation(int program, CharSequence name, CallbackInfoReturnable<Integer> cir) { if (smoothgl$pulse()) cir.setReturnValue(ShaderFallback.uniformLocation(program, name)); }
    @Inject(method = "glGetAttribLocation(ILjava/lang/CharSequence;)I", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$getAttribLocation(int program, CharSequence name, CallbackInfoReturnable<Integer> cir) { if (smoothgl$pulse()) cir.setReturnValue(ShaderFallback.attribLocation(program, name)); }
}
