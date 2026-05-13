package ru.ruskonnect.smoothgl.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.ruskonnect.smoothgl.SmoothGLClient;
import ru.ruskonnect.smoothgl.profiler.FrameBreakdown;
import ru.ruskonnect.smoothgl.profiler.StutterProfiler;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void smoothgl$guiStart(CallbackInfo ci) {
        if (!isEnabled()) return;
        StutterProfiler p = SmoothGLClient.profiler();
        if (p != null) p.breakdown().stageStart(FrameBreakdown.Stage.GUI);
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void smoothgl$guiEnd(CallbackInfo ci) {
        if (!isEnabled()) return;
        StutterProfiler p = SmoothGLClient.profiler();
        if (p != null) p.breakdown().stageEnd(FrameBreakdown.Stage.GUI);
    }

    private static boolean isEnabled() {
        return SmoothGLClient.config() != null && SmoothGLClient.config().enableProfiler;
    }
}
