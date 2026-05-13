package ru.ruskonnect.smoothgl.mixin;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.ruskonnect.smoothgl.SmoothGLClient;
import ru.ruskonnect.smoothgl.profiler.FrameBreakdown;
import ru.ruskonnect.smoothgl.profiler.StutterProfiler;

/**
 * Targets WorldRenderer#render by simple name. Yarn signatures change
 * across MC versions; using just the method name keeps this resilient
 * as long as there is exactly one {@code render} method in the class
 * (true for 1.21.x). {@code require = 0} guards against ambiguity —
 * if the target moves we get a soft skip, not a crash.
 */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void smoothgl$worldStart(CallbackInfo ci) {
        if (!isEnabled()) return;
        StutterProfiler p = SmoothGLClient.profiler();
        if (p != null) p.breakdown().stageStart(FrameBreakdown.Stage.WORLD);
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void smoothgl$worldEnd(CallbackInfo ci) {
        if (!isEnabled()) return;
        StutterProfiler p = SmoothGLClient.profiler();
        if (p != null) p.breakdown().stageEnd(FrameBreakdown.Stage.WORLD);
    }

    private static boolean isEnabled() {
        return SmoothGLClient.config() != null && SmoothGLClient.config().enableProfiler;
    }
}
