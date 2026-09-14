package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.PulseCallScope;
import dev.smoothgl.pulsevulkan.VulkanGlCompat;
import org.lwjgl.opengl.GL13;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GL13.class, priority = 900)
public abstract class GL13CompatMixin {
    @Inject(method = "glActiveTexture(I)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void smoothgl$activeTexture(int texture, CallbackInfo ci) {
        if (!PulseCallScope.isPulseCall()) return;
        VulkanGlCompat.activeTexture(texture);
        ci.cancel();
    }
}
