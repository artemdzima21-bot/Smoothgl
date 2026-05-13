package ru.ruskonnect.smoothgl.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.ruskonnect.smoothgl.SmoothGLClient;
import ru.ruskonnect.smoothgl.config.SmoothConfig;
import ru.ruskonnect.smoothgl.cull.CullEngine;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {

    @Inject(
        method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void smoothgl$cullDistantBlockEntity(BlockEntity be, float tickDelta,
                                                  net.minecraft.client.util.math.MatrixStack matrices,
                                                  net.minecraft.client.render.VertexConsumerProvider vcp,
                                                  CallbackInfo ci) {
        SmoothConfig cfg = SmoothGLClient.config();
        if (cfg == null || !cfg.enableBlockEntityCull || be == null) return;
        double dist = SmoothGLClient.adaptiveTuner() != null
            ? SmoothGLClient.adaptiveTuner().effectiveBlockEntityDistance()
            : cfg.blockEntityCullDistance;
        if (CullEngine.shouldCullBlockEntity(be.getPos(), dist)) {
            SmoothGLClient.cullStats().onBlockEntityCulled();
            ci.cancel();
        }
    }
}
