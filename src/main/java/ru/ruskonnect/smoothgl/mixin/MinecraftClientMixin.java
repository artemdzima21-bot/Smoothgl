package ru.ruskonnect.smoothgl.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.ruskonnect.smoothgl.SmoothGLClient;
import ru.ruskonnect.smoothgl.net.PacketPump;
import ru.ruskonnect.smoothgl.profiler.StutterProfiler;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void smoothgl$frameStart(boolean tick, CallbackInfo ci) {
        // Drain the incoming packet queue once per render frame. Vanilla only
        // does this in MinecraftClient.tick() at 20 Hz; on a 200+ FPS client
        // that wastes up to ~50 ms of perceived latency. Same thread, just
        // called more often — no thread-safety regression.
        PacketPump.pump();

        if (SmoothGLClient.config() == null || !SmoothGLClient.config().enableProfiler) return;
        StutterProfiler p = SmoothGLClient.profiler();
        if (p != null) p.onFrameStart();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void smoothgl$frameEnd(boolean tick, CallbackInfo ci) {
        if (SmoothGLClient.config() == null || !SmoothGLClient.config().enableProfiler) return;
        StutterProfiler p = SmoothGLClient.profiler();
        if (p != null) p.onFrameEnd(Thread.currentThread());
    }
}
