package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.PulseDiagnostics;
import dev.smoothgl.pulsevulkan.VulkanGlCompat;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(value = GL30.class, priority = 900)
public abstract class GL30CompatMixin {
    @Unique
    private static final AtomicInteger smoothgl$nextVao = new AtomicInteger(300_000);
    @Unique
    private static final Set<Integer> smoothgl$vaos = ConcurrentHashMap.newKeySet();
    @Unique
    private static volatile int smoothgl$boundVao;

    @Inject(method = "glFramebufferRenderbuffer(IIII)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$framebufferRenderbuffer(int target, int attachment, int renderbufferTarget, int renderbuffer, CallbackInfo ci) {
        VulkanGlCompat.framebufferRenderbuffer(target, attachment, renderbufferTarget, renderbuffer);
        ci.cancel();
    }

    @Inject(method = "glGenVertexArrays()I", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$genVertexArray(CallbackInfoReturnable<Integer> cir) {
        int id = smoothgl$nextVao.getAndIncrement();
        smoothgl$vaos.add(id);
        PulseDiagnostics.fallback("OpenGL VAO emulation enabled");
        cir.setReturnValue(id);
    }

    @Inject(method = "glBindVertexArray(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$bindVertexArray(int array, CallbackInfo ci) {
        if (array == 0 || smoothgl$vaos.contains(array)) smoothgl$boundVao = array;
        ci.cancel();
    }

    @Inject(method = "glDeleteVertexArrays(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$deleteVertexArray(int array, CallbackInfo ci) {
        smoothgl$vaos.remove(array);
        if (smoothgl$boundVao == array) smoothgl$boundVao = 0;
        ci.cancel();
    }

    @Inject(method = "glGenerateMipmap(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$generateMipmap(int target, CallbackInfo ci) {
        VulkanGlCompat.generateMipmap(target);
        ci.cancel();
    }
}
