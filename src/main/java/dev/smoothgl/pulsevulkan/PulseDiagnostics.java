package dev.smoothgl.pulsevulkan;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class PulseDiagnostics {
    private static final Set<String> ONCE = ConcurrentHashMap.newKeySet();
    private static final AtomicLong FALLBACK_CALLS = new AtomicLong();
    private static final boolean TRACE = Boolean.getBoolean("smoothgl.pulse.trace");

    private PulseDiagnostics() {}

    public static void infoOnce(String key, String message) {
        if (ONCE.add("info:" + key)) {
            System.out.println("[PulseVulkanBridge] " + message);
        }
    }

    public static void fallback(String operation) {
        FALLBACK_CALLS.incrementAndGet();
        if (ONCE.add("fallback:" + operation)) {
            System.out.println("[PulseVulkanBridge] Compatibility fallback: " + operation);
        } else if (TRACE) {
            System.out.println("[PulseVulkanBridge] fallback: " + operation);
        }
    }

    public static long fallbackCalls() {
        return FALLBACK_CALLS.get();
    }
}
