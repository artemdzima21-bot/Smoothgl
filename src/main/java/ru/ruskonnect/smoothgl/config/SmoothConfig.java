package ru.ruskonnect.smoothgl.config;

import net.fabricmc.loader.api.FabricLoader;
import ru.ruskonnect.smoothgl.SmoothGL;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tiny zero-dependency JSON-ish config. Stored as a flat key=value file
 * to avoid pulling in another JSON lib for a config of ~6 booleans.
 *
 * <p>Path: {@code <gameDir>/config/smoothgl.properties}</p>
 */
public final class SmoothConfig {

    public boolean enableProfiler = true;
    public boolean enableJitWarmup = true;
    public boolean enableGcWatcher = true;
    public boolean enableBufferPreTouch = true;
    /** GPU fence-based frame pacer. Caps frames-in-flight via {@code glFenceSync}. */
    public boolean enableFramePacer = true;
    /** Maximum frames the CPU is allowed to be ahead of the GPU. 1 = strictest pacing / lowest latency. */
    public int maxFramesInFlight = 1;

    /** Skip entity render past this distance (in blocks). 0 disables. */
    public boolean enableEntityCull = true;
    public double entityCullDistance = 64.0;
    /** Skip block entity render past this distance. Strict bound, since BEs are expensive. */
    public boolean enableBlockEntityCull = true;
    public double blockEntityCullDistance = 48.0;

    /** Distance-cull individual particles before they reach buildGeometry. */
    public boolean enableParticleCull = true;
    public double particleCullDistance = 32.0;
    /** Multiplier on particle cull distance based on size: tiny crit/damage particles get cut closer. */
    public boolean enableParticleSizeWeighting = true;

    /** Skip armor/elytra/cape layer rendering past this distance (entity silhouette stays). */
    public boolean enableLayerCull = true;
    public double layerCullDistance = 32.0;

    /** Cull entities/particles hidden behind opaque blocks (glass/panes do NOT occlude). */
    public boolean enableOcclusionCull = true;

    /** Adaptive auto-tuner: shrinks cull distances when p99 frame time exceeds target. */
    public boolean enableAdaptive = true;
    /** Target p99 frame time in ms (16.6 ≈ 60 FPS, 8.3 ≈ 120 FPS). */
    public double adaptiveTargetMs = 16.6;
    /** Tolerance band — only react when |p99 - target| > this. */
    public double adaptiveDeadbandMs = 2.0;
    /** Per-step shrink/grow factor (0.05 = ±5% per adjustment). */
    public double adaptiveStepRatio = 0.05;
    /** Don't shrink any distance below this (in blocks). */
    public double adaptiveMinDistance = 12.0;
    /** Adjust once per N frames (slower = more stable). */
    public int adaptiveAdjustEvery = 60;

    /** EXPERIMENTAL: socket-level network tuning. Off by default — opt-in.
     *  Sets TCP_NODELAY, larger socket buffers, IP_TOS=LOWDELAY, raises
     *  Netty event-loop thread priority. Cannot reduce raw network latency
     *  (that's physics) but can reduce perceived input lag by 5–30 ms in
     *  some scenarios. May trigger anti-cheat on strict servers — use at
     *  your own risk. */
    public boolean enableNetworkTuning = false;
    /** EXPERIMENTAL: drain the packet queue every render frame, not just every
     *  game tick. On 200+ FPS clients this saves up to ~45 ms perceived input
     *  lag because server packets stop waiting at the 20 Hz tick boundary.
     *  Vanilla's main-thread invariant is preserved (we call from the same
     *  thread, just more often). Safe on most servers; the rendering of
     *  effects/projectiles may appear slightly earlier than vanilla expects. */
    public boolean enablePacketPump = true;
    /** Mark outgoing packets with IPTOS_LOWDELAY (DSCP 0x10). Most ISPs ignore this. */
    public boolean networkLowDelayTos = true;
    /** Raise Netty event-loop thread to MAX_PRIORITY for snappier packet handling. */
    public boolean networkRaisePriority = true;
    /** Socket send/receive buffer size in KiB. 256 = 256 KiB, vanilla default is ~64. */
    public int networkBufferKiB = 256;

    /** Frames slower than median * this multiplier are flagged as stutters. */
    public double stutterRatio = 1.5d;
    /** Absolute floor (ms). Frames under this never count as stutter even if slow ratio. */
    public double stutterFloorMs = 8.0d;
    /** How many MiB of off-heap direct memory to pre-touch on startup. */
    public int preTouchMiB = 64;

    public static SmoothConfig load() {
        SmoothConfig cfg = new SmoothConfig();
        Path path = configPath();
        if (!Files.exists(path)) {
            cfg.save();
            return cfg;
        }
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int eq = trimmed.indexOf('=');
                if (eq <= 0) continue;
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                cfg.applyKv(key, value);
            }
        } catch (IOException e) {
            SmoothGL.LOGGER.warn("Failed to read config, using defaults", e);
        }
        return cfg;
    }

    public void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            Map<String, String> kv = new LinkedHashMap<>();
            kv.put("enableProfiler", Boolean.toString(enableProfiler));
            kv.put("enableJitWarmup", Boolean.toString(enableJitWarmup));
            kv.put("enableGcWatcher", Boolean.toString(enableGcWatcher));
            kv.put("enableBufferPreTouch", Boolean.toString(enableBufferPreTouch));
            kv.put("enableFramePacer", Boolean.toString(enableFramePacer));
            kv.put("maxFramesInFlight", Integer.toString(maxFramesInFlight));
            kv.put("enableEntityCull", Boolean.toString(enableEntityCull));
            kv.put("entityCullDistance", Double.toString(entityCullDistance));
            kv.put("enableBlockEntityCull", Boolean.toString(enableBlockEntityCull));
            kv.put("blockEntityCullDistance", Double.toString(blockEntityCullDistance));
            kv.put("enableParticleCull", Boolean.toString(enableParticleCull));
            kv.put("particleCullDistance", Double.toString(particleCullDistance));
            kv.put("enableParticleSizeWeighting", Boolean.toString(enableParticleSizeWeighting));
            kv.put("enableLayerCull", Boolean.toString(enableLayerCull));
            kv.put("layerCullDistance", Double.toString(layerCullDistance));
            kv.put("enableOcclusionCull", Boolean.toString(enableOcclusionCull));
            kv.put("enableAdaptive", Boolean.toString(enableAdaptive));
            kv.put("adaptiveTargetMs", Double.toString(adaptiveTargetMs));
            kv.put("adaptiveDeadbandMs", Double.toString(adaptiveDeadbandMs));
            kv.put("adaptiveStepRatio", Double.toString(adaptiveStepRatio));
            kv.put("adaptiveMinDistance", Double.toString(adaptiveMinDistance));
            kv.put("adaptiveAdjustEvery", Integer.toString(adaptiveAdjustEvery));
            kv.put("enableNetworkTuning", Boolean.toString(enableNetworkTuning));
            kv.put("enablePacketPump", Boolean.toString(enablePacketPump));
            kv.put("networkLowDelayTos", Boolean.toString(networkLowDelayTos));
            kv.put("networkRaisePriority", Boolean.toString(networkRaisePriority));
            kv.put("networkBufferKiB", Integer.toString(networkBufferKiB));
            kv.put("stutterRatio", Double.toString(stutterRatio));
            kv.put("stutterFloorMs", Double.toString(stutterFloorMs));
            kv.put("preTouchMiB", Integer.toString(preTouchMiB));

            StringBuilder sb = new StringBuilder();
            sb.append("# SmoothGL configuration\n");
            sb.append("# Each entry can be toggled at runtime via /smoothgl <key> <value>.\n");
            for (Map.Entry<String, String> e : kv.entrySet()) {
                sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
            }
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            SmoothGL.LOGGER.warn("Failed to write config", e);
        }
    }

    private void applyKv(String key, String value) {
        try {
            switch (key) {
                case "enableProfiler" -> enableProfiler = Boolean.parseBoolean(value);
                case "enableJitWarmup" -> enableJitWarmup = Boolean.parseBoolean(value);
                case "enableGcWatcher" -> enableGcWatcher = Boolean.parseBoolean(value);
                case "enableBufferPreTouch" -> enableBufferPreTouch = Boolean.parseBoolean(value);
                case "enableFramePacer" -> enableFramePacer = Boolean.parseBoolean(value);
                case "maxFramesInFlight" -> maxFramesInFlight = Integer.parseInt(value);
                case "enableEntityCull" -> enableEntityCull = Boolean.parseBoolean(value);
                case "entityCullDistance" -> entityCullDistance = Double.parseDouble(value);
                case "enableBlockEntityCull" -> enableBlockEntityCull = Boolean.parseBoolean(value);
                case "blockEntityCullDistance" -> blockEntityCullDistance = Double.parseDouble(value);
                case "enableParticleCull" -> enableParticleCull = Boolean.parseBoolean(value);
                case "particleCullDistance" -> particleCullDistance = Double.parseDouble(value);
                case "enableParticleSizeWeighting" -> enableParticleSizeWeighting = Boolean.parseBoolean(value);
                case "enableLayerCull" -> enableLayerCull = Boolean.parseBoolean(value);
                case "layerCullDistance" -> layerCullDistance = Double.parseDouble(value);
                case "enableOcclusionCull" -> enableOcclusionCull = Boolean.parseBoolean(value);
                case "enableAdaptive" -> enableAdaptive = Boolean.parseBoolean(value);
                case "adaptiveTargetMs" -> adaptiveTargetMs = Double.parseDouble(value);
                case "adaptiveDeadbandMs" -> adaptiveDeadbandMs = Double.parseDouble(value);
                case "adaptiveStepRatio" -> adaptiveStepRatio = Double.parseDouble(value);
                case "adaptiveMinDistance" -> adaptiveMinDistance = Double.parseDouble(value);
                case "adaptiveAdjustEvery" -> adaptiveAdjustEvery = Integer.parseInt(value);
                case "enableNetworkTuning" -> enableNetworkTuning = Boolean.parseBoolean(value);
                case "enablePacketPump" -> enablePacketPump = Boolean.parseBoolean(value);
                case "networkLowDelayTos" -> networkLowDelayTos = Boolean.parseBoolean(value);
                case "networkRaisePriority" -> networkRaisePriority = Boolean.parseBoolean(value);
                case "networkBufferKiB" -> networkBufferKiB = Integer.parseInt(value);
                case "stutterRatio" -> stutterRatio = Double.parseDouble(value);
                case "stutterFloorMs" -> stutterFloorMs = Double.parseDouble(value);
                case "preTouchMiB" -> preTouchMiB = Integer.parseInt(value);
                default -> SmoothGL.LOGGER.warn("Unknown config key: {}", key);
            }
        } catch (RuntimeException ex) {
            SmoothGL.LOGGER.warn("Invalid config value '{}' for '{}'", value, key);
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(SmoothGL.MOD_ID + ".properties");
    }
}
