package ru.ruskonnect.smoothgl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.ruskonnect.smoothgl.SmoothGLClient;
import ru.ruskonnect.smoothgl.config.SmoothConfig;
import ru.ruskonnect.smoothgl.cull.OcclusionCuller;

/**
 * Custom in-world geometry — our own vertex submissions for debug
 * visualisation. Drawn via Fabric's {@code WorldRenderEvents.LAST} so we
 * sit on top of vanilla's terrain/entity/particle layers but below the
 * vanilla F3 debug overlay.
 *
 * <p>Two pieces of geometry, both built on the fly each frame:</p>
 * <ol>
 *   <li><b>Cull boundary ring</b> — a horizontal circle at camera Y with
 *       radius equal to the active entity cull distance (respects adaptive
 *       tuner). Makes "where rendering stops" visible in 3D.</li>
 *   <li><b>Occlusion rays</b> — a thin line from the camera to each
 *       client-world entity within cull distance. Green = visible,
 *       red = occluded by our raycast. Watch in real-time whether the
 *       occlusion cache is working.</li>
 * </ol>
 *
 * <p>We push our own matrix translated by {@code -cameraPos} so all
 * vertex coordinates are world-space. Shader is the vanilla
 * position-colour pipeline — no custom GLSL.</p>
 */
public final class DebugRenderer {

    private DebugRenderer() {}

    public static void onRender(WorldRenderContext ctx) {
        var world = ctx.world();
        var camera = ctx.camera();
        if (world == null || camera == null) return;

        Vec3d camPos = camera.getPos();
        MatrixStack matrices = ctx.matrixStack();
        if (matrices == null) return;

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.disableCull();
        // No depth-test → lines visible through terrain; comment out for opaque variant.
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(2.0f);

        SmoothConfig cfg = SmoothGLClient.config();
        drawCullBoundary(mat, camPos, cfg);
        drawOcclusionRays(mat, camPos, cfg, world);

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.lineWidth(1.0f);
        matrices.pop();
    }

    private static void drawCullBoundary(Matrix4f mat, Vec3d camPos, SmoothConfig cfg) {
        var tuner = SmoothGLClient.adaptiveTuner();
        double radius = tuner != null ? tuner.effectiveEntityDistance() : cfg.entityCullDistance;
        if (radius < 4.0) return;

        // 64-segment horizontal circle at camera Y; one DEBUG_LINE_STRIP-style loop
        // emitted as DEBUG_LINES pairs (start, end) for each segment.
        final int segments = 64;
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float y = (float) camPos.y;
        float cx = (float) camPos.x;
        float cz = (float) camPos.z;
        for (int i = 0; i < segments; i++) {
            double a0 = (i       / (double) segments) * Math.PI * 2.0;
            double a1 = ((i + 1) / (double) segments) * Math.PI * 2.0;
            float x0 = cx + (float) (Math.cos(a0) * radius);
            float z0 = cz + (float) (Math.sin(a0) * radius);
            float x1 = cx + (float) (Math.cos(a1) * radius);
            float z1 = cz + (float) (Math.sin(a1) * radius);
            // Yellow-orange tinted; alpha picks up adaptive vs configured difference.
            int alpha = (tuner != null && tuner.isAdjusting()) ? 0xCC : 0x88;
            int colour = (alpha << 24) | 0xFFB060;
            bb.vertex(mat, x0, y, z0).color(colour);
            bb.vertex(mat, x1, y, z1).color(colour);
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    private static void drawOcclusionRays(Matrix4f mat, Vec3d camPos, SmoothConfig cfg, net.minecraft.world.World world) {
        if (!cfg.enableOcclusionCull) return;
        var mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return;

        var tuner = SmoothGLClient.adaptiveTuner();
        double maxDist = tuner != null ? tuner.effectiveEntityDistance() : cfg.entityCullDistance;
        double maxDistSq = maxDist * maxDist;

        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        boolean any = false;

        // Iterate all client-side entities; cap how many rays we emit so a packed
        // mob farm doesn't blow up our vertex buffer.
        int budget = 256;
        for (Entity e : mc.world.getEntities()) {
            if (e == null || e == mc.cameraEntity) continue;
            if (budget-- <= 0) break;
            Vec3d centre = new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ());
            double dSq = centre.squaredDistanceTo(camPos);
            if (dSq > maxDistSq) continue;

            boolean occluded = OcclusionCuller.isEntityOccluded(e, camPos, world);
            int colour = occluded ? 0x80FF4040 /* red, alpha 50% */ : 0x6040FF40 /* green, alpha 37% */;
            // Start the line a tiny bit in front of the camera so it isn't hidden by near-plane.
            bb.vertex(mat, (float) camPos.x, (float) (camPos.y - 0.2), (float) camPos.z).color(colour);
            bb.vertex(mat, (float) centre.x, (float) centre.y, (float) centre.z).color(colour);
            any = true;
        }
        if (any) {
            BufferRenderer.drawWithGlobalProgram(bb.end());
        } else {
            bb.end().close();
        }
    }
}
