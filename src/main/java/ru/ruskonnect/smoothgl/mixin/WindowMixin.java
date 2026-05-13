package ru.ruskonnect.smoothgl.mixin;

import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.ruskonnect.smoothgl.SmoothGLClient;
import ru.ruskonnect.smoothgl.pacing.FramePacer;

@Mixin(Window.class)
public abstract class WindowMixin {

    @Inject(method = "swapBuffers", at = @At("TAIL"))
    private void smoothgl$afterSwap(CallbackInfo ci) {
        if (SmoothGLClient.config() == null || !SmoothGLClient.config().enableFramePacer) return;
        FramePacer pacer = SmoothGLClient.framePacer();
        if (pacer != null) pacer.afterSwapBuffers();
    }
}
