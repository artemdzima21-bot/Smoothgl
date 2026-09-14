package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.ShaderFallback;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.FloatBuffer;

@Mixin(value = GL20.class, priority = 900)
public abstract class GL20CompatMixin {
    @Inject(method = "glCreateShader(I)I", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$createShader(int type, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ShaderFallback.createShader(type));
    }

    @Inject(method = "glShaderSource(ILjava/lang/CharSequence;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$shaderSource(int shader, CharSequence source, CallbackInfo ci) {
        ShaderFallback.shaderSource(shader, source);
        ci.cancel();
    }

    @Inject(method = "glCompileShader(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$compileShader(int shader, CallbackInfo ci) {
        ShaderFallback.compileShader(shader);
        ci.cancel();
    }

    @Inject(method = "glGetShaderi(II)I", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$getShaderi(int shader, int pname, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ShaderFallback.getShaderInt(shader, pname));
    }

    @Inject(method = "glGetShaderInfoLog(I)Ljava/lang/String;", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$getShaderInfoLog(int shader, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(ShaderFallback.shaderInfoLog(shader));
    }

    @Inject(method = "glGetShaderInfoLog(II)Ljava/lang/String;", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$getShaderInfoLogLimited(int shader, int maxLength, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(ShaderFallback.shaderInfoLog(shader));
    }

    @Inject(method = "glCreateProgram()I", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$createProgram(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ShaderFallback.createProgram());
    }

    @Inject(method = "glAttachShader(II)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$attachShader(int program, int shader, CallbackInfo ci) {
        ShaderFallback.attachShader(program, shader);
        ci.cancel();
    }

    @Inject(method = "glDetachShader(II)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$detachShader(int program, int shader, CallbackInfo ci) {
        ShaderFallback.detachShader(program, shader);
        ci.cancel();
    }

    @Inject(method = "glLinkProgram(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$linkProgram(int program, CallbackInfo ci) {
        ShaderFallback.linkProgram(program);
        ci.cancel();
    }

    @Inject(method = "glValidateProgram(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$validateProgram(int program, CallbackInfo ci) {
        ShaderFallback.validateProgram(program);
        ci.cancel();
    }

    @Inject(method = "glGetProgrami(II)I", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$getProgrami(int program, int pname, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ShaderFallback.getProgramInt(program, pname));
    }

    @Inject(method = "glGetProgramInfoLog(I)Ljava/lang/String;", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$getProgramInfoLog(int program, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(ShaderFallback.programInfoLog(program));
    }

    @Inject(method = "glGetProgramInfoLog(II)Ljava/lang/String;", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$getProgramInfoLogLimited(int program, int maxLength, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(ShaderFallback.programInfoLog(program));
    }

    @Inject(method = "glUseProgram(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$useProgram(int program, CallbackInfo ci) {
        ShaderFallback.useProgram(program);
        ci.cancel();
    }

    @Inject(method = "glDeleteShader(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$deleteShader(int shader, CallbackInfo ci) {
        ShaderFallback.deleteShader(shader);
        ci.cancel();
    }

    @Inject(method = "glDeleteProgram(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$deleteProgram(int program, CallbackInfo ci) {
        ShaderFallback.deleteProgram(program);
        ci.cancel();
    }

    @Inject(method = "glGetUniformLocation(ILjava/lang/CharSequence;)I", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$getUniformLocation(int program, CharSequence name, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ShaderFallback.uniformLocation(program, name));
    }

    @Inject(method = "glUniform1i(II)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform1i(int location, int value, CallbackInfo ci) {
        ShaderFallback.uniform(location, value);
        ci.cancel();
    }

    @Inject(method = "glUniform1f(IF)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform1f(int location, float value, CallbackInfo ci) {
        ShaderFallback.uniform(location, value);
        ci.cancel();
    }

    @Inject(method = "glUniform2f(IFF)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform2f(int location, float x, float y, CallbackInfo ci) {
        ShaderFallback.uniform(location, x + "," + y);
        ci.cancel();
    }

    @Inject(method = "glUniform3f(IFFF)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform3f(int location, float x, float y, float z, CallbackInfo ci) {
        ShaderFallback.uniform(location, x + "," + y + "," + z);
        ci.cancel();
    }

    @Inject(method = "glUniform4f(IFFFF)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniform4f(int location, float x, float y, float z, float w, CallbackInfo ci) {
        ShaderFallback.uniform(location, x + "," + y + "," + z + "," + w);
        ci.cancel();
    }

    @Inject(method = "glUniformMatrix4fv(IZLjava/nio/FloatBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$uniformMatrix4fv(int location, boolean transpose, FloatBuffer matrix, CallbackInfo ci) {
        ShaderFallback.uniformMatrix4(location, transpose, matrix);
        ci.cancel();
    }

    @Inject(method = "glGetAttribLocation(ILjava/lang/CharSequence;)I", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$getAttribLocation(int program, CharSequence name, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ShaderFallback.attribLocation(program, name));
    }

    @Inject(method = "glBindAttribLocation(IILjava/lang/CharSequence;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bindAttribLocation(int program, int index, CharSequence name, CallbackInfo ci) {
        ShaderFallback.bindAttribLocation(program, index, name);
        ci.cancel();
    }

    @Inject(method = "glEnableVertexAttribArray(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$enableVertexAttribArray(int index, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "glDisableVertexAttribArray(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$disableVertexAttribArray(int index, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "glVertexAttribPointer(IIIZIJ)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer, CallbackInfo ci) {
        ci.cancel();
    }
}
