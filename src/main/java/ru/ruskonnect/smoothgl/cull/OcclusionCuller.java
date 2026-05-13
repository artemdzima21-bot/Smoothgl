package ru.ruskonnect.smoothgl.cull;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cheap line-of-sight occlusion culling.
 *
 * <p>For each candidate entity we step along the line from the camera to
 * the entity's centre using a half-block DDA. If any sampled block is an
 * opaque full cube ({@code isOpaqueFullCube}), we declare the entity
 * occluded.</p>
 *
 * <p>Transparent blocks — glass, stained glass, panes, iron bars, fences,
 * fancy leaves — return {@code false} from {@code isOpaqueFullCube} and
 * therefore do not occlude. This is the exact same predicate Minecraft
 * itself uses for chunk face culling, so the result matches the player's
 * intuition: if you can see through it, mobs/particles behind it stay
 * visible.</p>
 *
 * <p>Per-entity TTL cache (default 100 ms = 10 Hz refresh) keeps raycast
 * cost bounded even at 1000 FPS — at most a few hundred raycasts per
 * second, each on the order of a few microseconds.</p>
 */
public final class OcclusionCuller {

    /** TTL of a cached visibility decision per entity, in nanoseconds. */
    private static final long CACHE_TTL_NS = 100L * 1_000_000L;
    /** Entries older than this are pruned. */
    private static final long PRUNE_AFTER_NS = 5L * 1_000_000_000L;
    /** Maximum ray length (blocks). Beyond this the entity is already distance-culled. */
    private static final double MAX_RAY_BLOCKS = 128.0;
    /** Don't bother with raycast if entity is closer than this — too cheap to skip. */
    private static final double MIN_RAY_BLOCKS = 8.0;
    /** Sampling step. 0.5 catches every 1-block-thick wall except at extreme grazing angles. */
    private static final double STEP = 0.5;

    private static final ConcurrentHashMap<UUID, Entry> CACHE = new ConcurrentHashMap<>();
    private static long lastPruneNs;

    private OcclusionCuller() {}

    private static final class Entry {
        long lastCheckedNs;
        long lastSeenNs;
        boolean occluded;
    }

    /**
     * @return true if the entity is hidden behind opaque geometry and may be culled.
     */
    public static boolean isEntityOccluded(Entity entity, Vec3d cam, BlockView world) {
        if (entity == null || world == null || cam == null) return false;

        // Compute distance once; bail out cheaply for close or absurdly far entities.
        Vec3d target = new Vec3d(entity.getX(), entity.getY() + entity.getHeight() * 0.5, entity.getZ());
        double distSq = target.squaredDistanceTo(cam);
        if (distSq < MIN_RAY_BLOCKS * MIN_RAY_BLOCKS) return false;
        if (distSq > MAX_RAY_BLOCKS * MAX_RAY_BLOCKS) return false; // distance cull handles it

        long now = System.nanoTime();
        UUID id = entity.getUuid();
        Entry e = CACHE.get(id);
        if (e == null) {
            e = new Entry();
            CACHE.put(id, e);
        }
        e.lastSeenNs = now;
        if (now - e.lastCheckedNs < CACHE_TTL_NS) {
            return e.occluded;
        }

        e.occluded = raycastBlocked(cam, target, world);
        e.lastCheckedNs = now;

        // Cheap periodic prune; amortised cost ~O(cache_size) once per 5 s.
        if (now - lastPruneNs > 5L * 1_000_000_000L) {
            prune(now);
            lastPruneNs = now;
        }
        return e.occluded;
    }

    /**
     * Particles have no stable identity and short lifetimes, so we don't
     * cache per-particle. Instead, this is a direct raycast with the same
     * predicate. Caller is responsible for gating by distance to keep
     * total raycasts bounded.
     */
    public static boolean isPointOccluded(double tx, double ty, double tz, Vec3d cam, BlockView world) {
        if (world == null || cam == null) return false;
        Vec3d target = new Vec3d(tx, ty, tz);
        double distSq = target.squaredDistanceTo(cam);
        if (distSq < MIN_RAY_BLOCKS * MIN_RAY_BLOCKS) return false;
        if (distSq > MAX_RAY_BLOCKS * MAX_RAY_BLOCKS) return false;
        return raycastBlocked(cam, target, world);
    }

    /** Core DDA: returns true if any sample along the ray sits inside an opaque full cube. */
    private static boolean raycastBlocked(Vec3d from, Vec3d to, BlockView world) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0) return false;

        // Don't sample inside the start (camera) or end (entity centre) blocks.
        int steps = (int) Math.ceil(len / STEP) - 1;
        if (steps < 1) return false;
        double invSteps = 1.0 / (steps + 1);
        BlockPos.Mutable pos = new BlockPos.Mutable();

        int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;
        for (int i = 1; i <= steps; i++) {
            double t = i * invSteps;
            int bx = MathHelper.floor(from.x + dx * t);
            int by = MathHelper.floor(from.y + dy * t);
            int bz = MathHelper.floor(from.z + dz * t);
            // Skip block lookups when we're still inside the previous sampled block.
            if (bx == lastX && by == lastY && bz == lastZ) continue;
            lastX = bx; lastY = by; lastZ = bz;
            pos.set(bx, by, bz);
            BlockState state;
            try {
                state = world.getBlockState(pos);
            } catch (Throwable t2) {
                return false; // unloaded chunk etc. — be conservative, don't cull.
            }
            if (state == null) continue;
            if (state.isOpaqueFullCube()) return true;
        }
        return false;
    }

    private static void prune(long now) {
        Iterator<Map.Entry<UUID, Entry>> it = CACHE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Entry> me = it.next();
            if (now - me.getValue().lastSeenNs > PRUNE_AFTER_NS) it.remove();
        }
    }

    public static void clearCache() { CACHE.clear(); }
    public static int cacheSize() { return CACHE.size(); }
}
