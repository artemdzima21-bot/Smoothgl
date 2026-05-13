package ru.ruskonnect.smoothgl;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.ruskonnect.smoothgl.adaptive.AdaptiveTuner;
import ru.ruskonnect.smoothgl.buffers.BufferPreTouch;
import ru.ruskonnect.smoothgl.cull.CullStats;
import ru.ruskonnect.smoothgl.net.PingMonitor;
import ru.ruskonnect.smoothgl.hud.BreakdownHud;
import ru.ruskonnect.smoothgl.config.SmoothConfig;
import ru.ruskonnect.smoothgl.gc.GcWatcher;
import ru.ruskonnect.smoothgl.jit.JitWarmup;
import ru.ruskonnect.smoothgl.pacing.FramePacer;
import ru.ruskonnect.smoothgl.profiler.FrameBreakdown;
import ru.ruskonnect.smoothgl.profiler.StutterProfiler;

public final class SmoothGLClient implements ClientModInitializer {

    private static SmoothConfig config;
    private static StutterProfiler profiler;
    private static GcWatcher gcWatcher;
    private static FramePacer framePacer;
    private static AdaptiveTuner adaptiveTuner;
    private static final PingMonitor pingMonitor = new PingMonitor();
    private static final CullStats cullStats = new CullStats();
    private static KeyBinding toggleHudKey;

    @Override
    public void onInitializeClient() {
        config = SmoothConfig.load();
        profiler = new StutterProfiler(config);
        adaptiveTuner = new AdaptiveTuner(config, profiler);

        if (config.enableGcWatcher) {
            gcWatcher = new GcWatcher();
            gcWatcher.install();
        }
        if (config.enableJitWarmup) {
            JitWarmup.scheduleAsync();
        }
        if (config.enableBufferPreTouch) {
            BufferPreTouch.preTouchAsync(config.preTouchMiB);
        }
        framePacer = new FramePacer(config.maxFramesInFlight);

        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.smoothgl.toggleHud",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F7,
            "category.smoothgl"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            pingMonitor.tick();
            while (toggleHudKey.wasPressed()) {
                BreakdownHud.toggle();
            }
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            BreakdownHud.render(ctx, net.minecraft.client.MinecraftClient.getInstance());
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("smoothgl")
                .executes(ctx -> {
                    sendStatus(ctx.getSource());
                    return 1;
                })
                .then(ClientCommandManager.literal("dump").executes(ctx -> {
                    dumpEvents(ctx.getSource());
                    return 1;
                }))
                .then(ClientCommandManager.literal("breakdown").executes(ctx -> {
                    sendBreakdown(ctx.getSource());
                    return 1;
                }))
                .then(ClientCommandManager.literal("reset").executes(ctx -> {
                    profiler = new StutterProfiler(config);
                    ctx.getSource().sendFeedback(Text.literal("[SmoothGL] profiler reset"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("set")
                    .then(ClientCommandManager.literal("enableProfiler")
                        .then(ClientCommandManager.argument("v", BoolArgumentType.bool())
                            .executes(ctx -> { config.enableProfiler = BoolArgumentType.getBool(ctx, "v"); config.save(); reply(ctx.getSource(), "enableProfiler=" + config.enableProfiler); return 1; })))
                    .then(ClientCommandManager.literal("stutterRatio")
                        .then(ClientCommandManager.argument("v", DoubleArgumentType.doubleArg(1.0, 10.0))
                            .executes(ctx -> { config.stutterRatio = DoubleArgumentType.getDouble(ctx, "v"); config.save(); reply(ctx.getSource(), "stutterRatio=" + config.stutterRatio); return 1; })))
                    .then(ClientCommandManager.literal("stutterFloorMs")
                        .then(ClientCommandManager.argument("v", DoubleArgumentType.doubleArg(0.0, 1000.0))
                            .executes(ctx -> { config.stutterFloorMs = DoubleArgumentType.getDouble(ctx, "v"); config.save(); reply(ctx.getSource(), "stutterFloorMs=" + config.stutterFloorMs); return 1; })))
                    .then(ClientCommandManager.literal("preTouchMiB")
                        .then(ClientCommandManager.argument("v", IntegerArgumentType.integer(0, 4096))
                            .executes(ctx -> { config.preTouchMiB = IntegerArgumentType.getInteger(ctx, "v"); config.save(); reply(ctx.getSource(), "preTouchMiB=" + config.preTouchMiB + " (restart to apply)"); return 1; })))
                    .then(ClientCommandManager.literal("enableFramePacer")
                        .then(ClientCommandManager.argument("v", BoolArgumentType.bool())
                            .executes(ctx -> { config.enableFramePacer = BoolArgumentType.getBool(ctx, "v"); config.save(); reply(ctx.getSource(), "enableFramePacer=" + config.enableFramePacer); return 1; })))
                    .then(ClientCommandManager.literal("maxFramesInFlight")
                        .then(ClientCommandManager.argument("v", IntegerArgumentType.integer(1, 4))
                            .executes(ctx -> {
                                config.maxFramesInFlight = IntegerArgumentType.getInteger(ctx, "v");
                                config.save();
                                if (framePacer != null) framePacer.setMaxFramesInFlight(config.maxFramesInFlight);
                                reply(ctx.getSource(), "maxFramesInFlight=" + config.maxFramesInFlight);
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("enableEntityCull")
                        .then(ClientCommandManager.argument("v", BoolArgumentType.bool())
                            .executes(ctx -> { config.enableEntityCull = BoolArgumentType.getBool(ctx, "v"); config.save(); reply(ctx.getSource(), "enableEntityCull=" + config.enableEntityCull); return 1; })))
                    .then(ClientCommandManager.literal("entityCullDistance")
                        .then(ClientCommandManager.argument("v", DoubleArgumentType.doubleArg(0.0, 512.0))
                            .executes(ctx -> { config.entityCullDistance = DoubleArgumentType.getDouble(ctx, "v"); config.save(); reply(ctx.getSource(), "entityCullDistance=" + config.entityCullDistance); return 1; })))
                    .then(ClientCommandManager.literal("enableBlockEntityCull")
                        .then(ClientCommandManager.argument("v", BoolArgumentType.bool())
                            .executes(ctx -> { config.enableBlockEntityCull = BoolArgumentType.getBool(ctx, "v"); config.save(); reply(ctx.getSource(), "enableBlockEntityCull=" + config.enableBlockEntityCull); return 1; })))
                    .then(ClientCommandManager.literal("blockEntityCullDistance")
                        .then(ClientCommandManager.argument("v", DoubleArgumentType.doubleArg(0.0, 512.0))
                            .executes(ctx -> { config.blockEntityCullDistance = DoubleArgumentType.getDouble(ctx, "v"); config.save(); reply(ctx.getSource(), "blockEntityCullDistance=" + config.blockEntityCullDistance); return 1; })))
                    .then(ClientCommandManager.literal("enableParticleCull")
                        .then(ClientCommandManager.argument("v", BoolArgumentType.bool())
                            .executes(ctx -> { config.enableParticleCull = BoolArgumentType.getBool(ctx, "v"); config.save(); reply(ctx.getSource(), "enableParticleCull=" + config.enableParticleCull); return 1; })))
                    .then(ClientCommandManager.literal("particleCullDistance")
                        .then(ClientCommandManager.argument("v", DoubleArgumentType.doubleArg(0.0, 256.0))
                            .executes(ctx -> { config.particleCullDistance = DoubleArgumentType.getDouble(ctx, "v"); config.save(); reply(ctx.getSource(), "particleCullDistance=" + config.particleCullDistance); return 1; })))
                ));
        });

        SmoothGL.LOGGER.info("SmoothGL initialized: profiler={}, jit={}, gc={}, preTouch={}MiB",
            config.enableProfiler, config.enableJitWarmup, config.enableGcWatcher, config.preTouchMiB);
    }

    private static void sendStatus(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource src) {
        double median = profiler.currentMedianMs();
        double p99 = profiler.currentP99Ms();
        long frames = profiler.totalFrames();
        long stutters = profiler.totalStutters();
        long gcPauseMs = gcWatcher == null ? -1 : gcWatcher.totalPauseMs();
        long longGcs = gcWatcher == null ? -1 : gcWatcher.longPauseCount();

        String pacerStr;
        if (framePacer == null || !config.enableFramePacer) {
            pacerStr = "off";
        } else if (framePacer.isDisabled()) {
            pacerStr = "unsupported";
        } else {
            pacerStr = "depth=" + framePacer.maxFramesInFlight()
                + " avgWait=" + fmt(framePacer.avgWaitMs()) + "ms"
                + " maxWait=" + fmt(framePacer.maxWaitMs()) + "ms";
        }

        reply(src, "§b[SmoothGL] frames=" + frames + " stutters=" + stutters
            + " median=" + fmt(median) + "ms p99=" + fmt(p99) + "ms"
            + " gcPause=" + (gcPauseMs < 0 ? "off" : (gcPauseMs + "ms"))
            + " longGCs=" + (longGcs < 0 ? "off" : longGcs)
            + " pacer=[" + pacerStr + "]");
    }

    private static void sendBreakdown(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource src) {
        FrameBreakdown b = profiler.breakdown();
        if (b.sampleCount() < 30) {
            reply(src, "[SmoothGL] not enough samples yet (" + b.sampleCount() + "/30) — play for a few seconds");
            return;
        }
        double total = profiler.currentMedianMs();
        double world = b.avgMs(FrameBreakdown.Stage.WORLD);
        double gui = b.avgMs(FrameBreakdown.Stage.GUI);
        double pacer = b.avgMs(FrameBreakdown.Stage.PACER_WAIT);
        double known = world + gui + pacer;
        // Total here is rolling median; "other" approximates uncategorised time per frame.
        double avgFrame = avgFrameMs();
        double other = Math.max(0.0, avgFrame - known);

        reply(src, "§b[SmoothGL] frame breakdown (avg over " + b.sampleCount() + " frames):");
        reply(src, "  total avg=" + fmt(avgFrame) + "ms  median=" + fmt(total) + "ms");
        reply(src, "  world      = " + bar(world, avgFrame) + " " + fmt(world) + "ms (max " + fmt(b.maxMs(FrameBreakdown.Stage.WORLD)) + ")");
        reply(src, "  gui        = " + bar(gui, avgFrame) + " " + fmt(gui) + "ms (max " + fmt(b.maxMs(FrameBreakdown.Stage.GUI)) + ")");
        reply(src, "  pacer-wait = " + bar(pacer, avgFrame) + " " + fmt(pacer) + "ms (max " + fmt(b.maxMs(FrameBreakdown.Stage.PACER_WAIT)) + ")");
        reply(src, "  other      = " + bar(other, avgFrame) + " " + fmt(other) + "ms");
    }

    private static double avgFrameMs() {
        // Weak: re-derive from median; for a precise mean we'd expose it from profiler.
        // This keeps the API minimal — median is close enough for a relative breakdown.
        return profiler.currentMedianMs();
    }

    private static String bar(double v, double total) {
        if (total <= 0) return "[          ]";
        int w = 10;
        int filled = (int) Math.round((v / total) * w);
        if (filled < 0) filled = 0;
        if (filled > w) filled = w;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < w; i++) sb.append(i < filled ? '#' : ' ');
        sb.append(']');
        return sb.toString();
    }

    private static void dumpEvents(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource src) {
        var events = profiler.snapshotEvents();
        if (events.isEmpty()) {
            reply(src, "[SmoothGL] no stutters recorded yet");
            return;
        }
        reply(src, "[SmoothGL] top " + Math.min(5, events.size()) + " stutters:");
        int i = 0;
        for (var ev : events) {
            if (i++ >= 5) break;
            String top = ev.mainStack().length > 0 ? ev.mainStack()[0].toString() : "<no-stack>";
            reply(src, " " + fmt(ev.durationMs()) + "ms (median " + fmt(ev.medianMs()) + "ms) @ " + top);
        }
        SmoothGL.LOGGER.info("---- SmoothGL stutter dump ({} events) ----", events.size());
        for (var ev : events) {
            SmoothGL.LOGGER.info("[stutter] {}ms (median {}ms) ts={}",
                fmt(ev.durationMs()), fmt(ev.medianMs()), ev.timestampMs());
            for (StackTraceElement el : ev.mainStack()) {
                SmoothGL.LOGGER.info("    at {}", el);
            }
        }
    }

    private static void reply(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource src, String msg) {
        src.sendFeedback(Text.literal(msg));
    }

    private static String fmt(double v) { return String.format("%.2f", v); }

    public static SmoothConfig config() { return config; }
    public static StutterProfiler profiler() { return profiler; }
    public static FramePacer framePacer() { return framePacer; }
    public static CullStats cullStats() { return cullStats; }
    public static AdaptiveTuner adaptiveTuner() { return adaptiveTuner; }
    public static PingMonitor pingMonitor() { return pingMonitor; }
}
