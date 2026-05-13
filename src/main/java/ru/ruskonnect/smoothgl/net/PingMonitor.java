package ru.ruskonnect.smoothgl.net;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.util.Arrays;
import java.util.UUID;

/**
 * Polls Minecraft's own latency reading (from the vanilla KeepAlive round-trip)
 * once per game tick and maintains a rolling window of samples for HUD display.
 *
 * <p>Vanilla updates the latency field only when a KeepAlive packet is
 * acknowledged — by default once every ~15 seconds. So same value is
 * observed across many ticks; we de-duplicate so the rolling buffer
 * stores actual <em>distinct</em> RTT samples, not 300 copies of the same
 * one. This gives meaningful median/p99/jitter on a useful timescale
 * (last ~5 minutes at vanilla cadence).</p>
 *
 * <p>This module never sends any custom packets — pure read-only. Safe on
 * any server, invisible to anti-cheat.</p>
 */
public final class PingMonitor {

    private static final int WINDOW = 64; // ~16 minutes at one keepalive per 15s
    private final int[] samples = new int[WINDOW];
    private int count;
    private int idx;
    private int lastObserved = -1;

    /** Called once per client tick. Cheap, ~O(1) amortized. */
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        ClientPlayNetworkHandler net = mc.getNetworkHandler();
        if (net == null) {
            lastObserved = -1;
            return;
        }
        UUID self;
        try {
            self = mc.getSession().getUuidOrNull();
        } catch (Throwable t) {
            return;
        }
        if (self == null) return;
        PlayerListEntry entry = net.getPlayerListEntry(self);
        if (entry == null) return;
        int latency = entry.getLatency();
        if (latency <= 0) return;
        // De-duplicate: only push when the underlying value actually changed.
        if (latency == lastObserved) return;
        lastObserved = latency;
        samples[idx] = latency;
        idx = (idx + 1) % WINDOW;
        if (count < WINDOW) count++;
    }

    public boolean hasData() { return count > 0; }
    public int currentMs() { return lastObserved; }

    /**
     * Copies recent ping samples in chronological order (oldest → newest)
     * for telemetry rendering. Returns number of valid entries written.
     */
    public int copyRecentSamples(int[] out) {
        int n = Math.min(count, out.length);
        for (int i = 0; i < n; i++) {
            int src = (idx - 1 - i + WINDOW) % WINDOW;
            out[n - 1 - i] = samples[src];
        }
        return n;
    }

    public int medianMs() { return percentile(50); }
    public int p99Ms() { return percentile(99); }

    /** Standard deviation of the rolling window, in ms. */
    public double jitterMs() {
        if (count < 2) return 0.0;
        double mean = 0.0;
        for (int i = 0; i < count; i++) mean += samples[i];
        mean /= count;
        double var = 0.0;
        for (int i = 0; i < count; i++) {
            double d = samples[i] - mean;
            var += d * d;
        }
        return Math.sqrt(var / (count - 1));
    }

    private int percentile(int p) {
        if (count == 0) return 0;
        int[] copy = Arrays.copyOf(samples, count);
        Arrays.sort(copy);
        int rank = (int) Math.min(count - 1, Math.max(0, Math.round((p / 100.0) * (count - 1))));
        return copy[rank];
    }
}
