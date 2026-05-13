package ru.ruskonnect.smoothgl.cull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-frame cull counters. Reset by the profiler at frame start so the
 * F7 HUD can display "this frame we skipped X entities and Y block
 * entities". Cumulative totals are also retained for the chat status
 * command.
 */
public final class CullStats {

    private final AtomicInteger entitiesCulledFrame = new AtomicInteger();
    private final AtomicInteger blockEntitiesCulledFrame = new AtomicInteger();
    private final AtomicInteger particlesCulledFrame = new AtomicInteger();
    private final AtomicLong entitiesCulledTotal = new AtomicLong();
    private final AtomicLong blockEntitiesCulledTotal = new AtomicLong();
    private final AtomicLong particlesCulledTotal = new AtomicLong();

    /** Snapshot of the last completed frame's counters (for stable HUD display). */
    private volatile int lastFrameEntities;
    private volatile int lastFrameBlockEntities;
    private volatile int lastFrameParticles;

    public void onEntityCulled() {
        entitiesCulledFrame.incrementAndGet();
        entitiesCulledTotal.incrementAndGet();
    }

    public void onBlockEntityCulled() {
        blockEntitiesCulledFrame.incrementAndGet();
        blockEntitiesCulledTotal.incrementAndGet();
    }

    public void onParticleCulled() {
        particlesCulledFrame.incrementAndGet();
        particlesCulledTotal.incrementAndGet();
    }

    /** Called by StutterProfiler at frame start: snapshot then reset. */
    public void rotate() {
        lastFrameEntities = entitiesCulledFrame.getAndSet(0);
        lastFrameBlockEntities = blockEntitiesCulledFrame.getAndSet(0);
        lastFrameParticles = particlesCulledFrame.getAndSet(0);
    }

    public int lastFrameEntitiesCulled() { return lastFrameEntities; }
    public int lastFrameBlockEntitiesCulled() { return lastFrameBlockEntities; }
    public int lastFrameParticlesCulled() { return lastFrameParticles; }
    public long totalEntitiesCulled() { return entitiesCulledTotal.get(); }
    public long totalBlockEntitiesCulled() { return blockEntitiesCulledTotal.get(); }
    public long totalParticlesCulled() { return particlesCulledTotal.get(); }
}
