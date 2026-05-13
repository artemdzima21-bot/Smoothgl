package ru.ruskonnect.smoothgl.adaptive;

import ru.ruskonnect.smoothgl.SmoothGL;
import ru.ruskonnect.smoothgl.config.SmoothConfig;
import ru.ruskonnect.smoothgl.profiler.StutterProfiler;

/**
 * Closed-loop tuner: watches the profiler's p99 and shrinks/expands the
 * various cull distances to keep the frame budget under a target.
 *
 * <p>Algorithm: every {@code adjustEvery} frames, sample p99. If it's
 * above the target by more than the deadband, ramp distances DOWN by
 * {@code stepRatio}. If well below the target (median + headroom), ramp
 * UP. Distances clamp at user-defined {@code adaptiveMinDistance} /
 * {@code adaptiveMaxDistance} so we never go absurd.</p>
 *
 * <p>This is intentionally slow (one step per ~60 frames = ~1 second).
 * Fast tuning oscillates and feels worse than a stable wrong value.</p>
 */
public final class AdaptiveTuner {

    private final SmoothConfig cfg;
    private final StutterProfiler profiler;
    private int frameCounter;

    /** Active (live) cull distances. Static config values are upper bounds. */
    private double liveEntityDist;
    private double liveBlockEntityDist;
    private double liveParticleDist;

    public AdaptiveTuner(SmoothConfig cfg, StutterProfiler profiler) {
        this.cfg = cfg;
        this.profiler = profiler;
        this.liveEntityDist = cfg.entityCullDistance;
        this.liveBlockEntityDist = cfg.blockEntityCullDistance;
        this.liveParticleDist = cfg.particleCullDistance;
    }

    /** Called every frame end. Cheap unless it's an "adjust frame". */
    public void onFrame() {
        if (!cfg.enableAdaptive) return;
        if (++frameCounter < cfg.adaptiveAdjustEvery) return;
        frameCounter = 0;

        double p99 = profiler.currentP99Ms();
        if (p99 <= 0) return; // not enough samples yet

        double target = cfg.adaptiveTargetMs;
        double upper = target + cfg.adaptiveDeadbandMs;
        double lower = Math.max(1.0, target - cfg.adaptiveDeadbandMs * 2.0);

        if (p99 > upper) {
            // Frame budget exceeded → tighten cull distances.
            liveEntityDist = scaleDown(liveEntityDist);
            liveBlockEntityDist = scaleDown(liveBlockEntityDist);
            liveParticleDist = scaleDown(liveParticleDist);
            SmoothGL.LOGGER.debug("[Adaptive] p99={}ms > {} ms → tighten E={} B={} P={}",
                fmt(p99), fmt(upper), fmt(liveEntityDist), fmt(liveBlockEntityDist), fmt(liveParticleDist));
        } else if (p99 < lower) {
            // Plenty of headroom → relax back toward the user's configured limits.
            liveEntityDist = scaleUp(liveEntityDist, cfg.entityCullDistance);
            liveBlockEntityDist = scaleUp(liveBlockEntityDist, cfg.blockEntityCullDistance);
            liveParticleDist = scaleUp(liveParticleDist, cfg.particleCullDistance);
        }
    }

    private double scaleDown(double current) {
        double next = current * (1.0 - cfg.adaptiveStepRatio);
        return Math.max(next, cfg.adaptiveMinDistance);
    }

    private double scaleUp(double current, double userMax) {
        double next = current * (1.0 + cfg.adaptiveStepRatio);
        return Math.min(next, userMax);
    }

    public double effectiveEntityDistance() {
        return cfg.enableAdaptive ? liveEntityDist : cfg.entityCullDistance;
    }

    public double effectiveBlockEntityDistance() {
        return cfg.enableAdaptive ? liveBlockEntityDist : cfg.blockEntityCullDistance;
    }

    public double effectiveParticleDistance() {
        return cfg.enableAdaptive ? liveParticleDist : cfg.particleCullDistance;
    }

    public boolean isAdjusting() {
        return cfg.enableAdaptive
            && (Math.abs(liveEntityDist - cfg.entityCullDistance) > 0.5
                || Math.abs(liveBlockEntityDist - cfg.blockEntityCullDistance) > 0.5
                || Math.abs(liveParticleDist - cfg.particleCullDistance) > 0.5);
    }

    private static String fmt(double d) { return String.format("%.1f", d); }
}
