package ru.ruskonnect.smoothgl.mixin;

import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.ruskonnect.smoothgl.SmoothGLClient;
import ru.ruskonnect.smoothgl.config.SmoothConfig;
import ru.ruskonnect.smoothgl.cull.CullEngine;

/**
 * Distance-cull individual particles right before they emit geometry.
 *
 * <p>Targets {@link BillboardParticle} rather than {@link Particle} because
 * {@code Particle#render} is <b>abstract</b> — injecting at HEAD into an
 * abstract method crashes Mixin ({@code insnNode is null}).
 * {@code BillboardParticle} is the concrete parent of the vast majority of
 * particles (smoke, flame, explosion, sweep, redstone, water, lava, etc.),
 * so culling it covers ~95% of cases. The few non-billboard particles
 * (e.g. firework explosions) are left to vanilla.</p>
 */
@Mixin(BillboardParticle.class)
public abstract class ParticleMixin {

    @Inject(
        method = "render(Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/Camera;F)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void smoothgl$cullDistantParticle(CallbackInfo ci) {
        SmoothConfig cfg = SmoothGLClient.config();
        if (cfg == null || !cfg.enableParticleCull) return;
        Particle self = (Particle) (Object) this;
        var bb = self.getBoundingBox();
        double cx = (bb.minX + bb.maxX) * 0.5;
        double cy = (bb.minY + bb.maxY) * 0.5;
        double cz = (bb.minZ + bb.maxZ) * 0.5;

        double baseDist = SmoothGLClient.adaptiveTuner() != null
            ? SmoothGLClient.adaptiveTuner().effectiveParticleDistance()
            : cfg.particleCullDistance;

        // Size-aware: tiny particles (crit, damage indicator ~0.05 size) become
        // visually irrelevant much closer than big particles (explosion ~1.0).
        // Scale the cull distance by particle size, clamped to [0.4, 1.0]×.
        double effectiveDist = baseDist;
        if (cfg.enableParticleSizeWeighting) {
            double bboxMax = Math.max(Math.max(bb.maxX - bb.minX, bb.maxY - bb.minY), bb.maxZ - bb.minZ);
            // Reference size 0.25 ≈ vanilla smoke/flame. Smaller → tighter cull.
            double sizeFactor = Math.max(0.4, Math.min(1.0, bboxMax / 0.25));
            effectiveDist = baseDist * sizeFactor;
        }

        if (CullEngine.shouldCullParticle(cx, cy, cz, effectiveDist)) {
            SmoothGLClient.cullStats().onParticleCulled();
            ci.cancel();
            return;
        }

        // Occlusion cull for particles: only check moderately-far ones; close
        // particles are cheap and the raycast cost would dominate.
        if (cfg.enableOcclusionCull) {
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc != null && mc.gameRenderer != null && mc.world != null) {
                var cam = mc.gameRenderer.getCamera().getPos();
                if (ru.ruskonnect.smoothgl.cull.OcclusionCuller.isPointOccluded(cx, cy, cz, cam, mc.world)) {
                    SmoothGLClient.cullStats().onParticleCulled();
                    ci.cancel();
                }
            }
        }
    }
}
