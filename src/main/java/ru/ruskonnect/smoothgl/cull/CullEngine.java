package ru.ruskonnect.smoothgl.cull;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

/**
 * Cheap render-time culling decisions for entities and block entities.
 *
 * <p>The vanilla Minecraft client does perform per-entity frustum culling
 * via {@code EntityRenderer.shouldRender}, but it does <b>not</b> apply a
 * configurable maximum render distance independent of the chunk render
 * distance — so on a server with simulation distance 12 you get every
 * entity in 12 chunks rendered every frame, whether you can see them
 * meaningfully or not.</p>
 *
 * <p>For block entities, vanilla iterates a per-frame list compiled by
 * the {@code WorldRenderer}; long-distance signs / item frames / chests
 * still go through their entity renderers every frame. A distance cap
 * is the cheapest way to cut that.</p>
 *
 * <p>All distance comparisons are done squared to avoid {@code sqrt}.</p>
 */
public final class CullEngine {

    private CullEngine() {}

    /**
     * @return true if the entity should be skipped (i.e. cull it).
     */
    public static boolean shouldCullEntity(Entity entity, double maxDistance) {
        if (entity == null || maxDistance <= 0) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.gameRenderer == null) return false;
        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null) return false;
        // Never cull the local player or the entity the camera is attached to —
        // hiding them would break first/third-person rendering.
        if (entity == mc.player) return false;
        if (cam.getFocusedEntity() == entity) return false;

        double dx = entity.getX() - cam.getPos().x;
        double dy = entity.getY() - cam.getPos().y;
        double dz = entity.getZ() - cam.getPos().z;
        double distSq = dx * dx + dy * dy + dz * dz;
        double maxSq = maxDistance * maxDistance;
        return distSq > maxSq;
    }

    /**
     * Distance check for a particle, given its world coordinates.
     * Particles don't have a "focused" relationship with the camera,
     * so a plain squared-distance check is fine.
     */
    public static boolean shouldCullParticle(double px, double py, double pz, double maxDistance) {
        if (maxDistance <= 0) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.gameRenderer == null) return false;
        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null) return false;
        double dx = px - cam.getPos().x;
        double dy = py - cam.getPos().y;
        double dz = pz - cam.getPos().z;
        double distSq = dx * dx + dy * dy + dz * dz;
        double maxSq = maxDistance * maxDistance;
        return distSq > maxSq;
    }

    /**
     * @return true if the block entity at {@code pos} should be skipped.
     */
    public static boolean shouldCullBlockEntity(BlockPos pos, double maxDistance) {
        if (pos == null || maxDistance <= 0) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.gameRenderer == null) return false;
        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null) return false;

        // Centre on the block itself, not its corner — half a block matters
        // at short distances.
        double dx = (pos.getX() + 0.5) - cam.getPos().x;
        double dy = (pos.getY() + 0.5) - cam.getPos().y;
        double dz = (pos.getZ() + 0.5) - cam.getPos().z;
        double distSq = dx * dx + dy * dy + dz * dz;
        double maxSq = maxDistance * maxDistance;
        return distSq > maxSq;
    }
}
