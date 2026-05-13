package ru.ruskonnect.smoothgl.mixin;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.ruskonnect.smoothgl.SmoothGLClient;
import ru.ruskonnect.smoothgl.config.SmoothConfig;
import ru.ruskonnect.smoothgl.cull.CullEngine;
import ru.ruskonnect.smoothgl.cull.OcclusionCuller;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(
        method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void smoothgl$cullDistantEntity(Entity entity, double x, double y, double z,
                                             float tickDelta,
                                             net.minecraft.client.util.math.MatrixStack matrices,
                                             net.minecraft.client.render.VertexConsumerProvider vcp,
                                             int light, CallbackInfo ci) {
        SmoothConfig cfg = SmoothGLClient.config();
        if (cfg == null || !cfg.enableEntityCull) return;
        double dist = SmoothGLClient.adaptiveTuner() != null
            ? SmoothGLClient.adaptiveTuner().effectiveEntityDistance()
            : cfg.entityCullDistance;
        if (CullEngine.shouldCullEntity(entity, dist)) {
            SmoothGLClient.cullStats().onEntityCulled();
            ci.cancel();
            return;
        }

        // Occlusion cull: if entity is behind opaque geometry, skip render.
        // Transparent blocks (glass, panes, bars, fancy leaves) don't occlude.
        if (cfg.enableOcclusionCull) {
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc != null && mc.gameRenderer != null && mc.world != null) {
                var cam = mc.gameRenderer.getCamera().getPos();
                if (OcclusionCuller.isEntityOccluded(entity, cam, mc.world)) {
                    SmoothGLClient.cullStats().onEntityCulled();
                    ci.cancel();
                }
            }
        }
    }
}
