package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.GlIntegerQueryFallback;
import org.lwjgl.opengl.GL11C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.IntBuffer;

@Mixin(value = GL11C.class, priority = 1100)
public abstract class GL11CCompatMixin {
    @Inject(method = "glGetIntegerv(I[I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$getIntegersArray(int pname, int[] params, CallbackInfo ci) {
        GlIntegerQueryFallback.fill(pname, params);
        ci.cancel();
    }

    @Inject(method = "glGetIntegerv(ILjava/nio/IntBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$getIntegersBuffer(int pname, IntBuffer params, CallbackInfo ci) {
        GlIntegerQueryFallback.fill(pname, params);
        ci.cancel();
    }
}
