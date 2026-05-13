package ru.ruskonnect.smoothgl.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import ru.ruskonnect.smoothgl.SmoothGLClient;
import ru.ruskonnect.smoothgl.cull.CullStats;
import ru.ruskonnect.smoothgl.pacing.FramePacer;
import ru.ruskonnect.smoothgl.profiler.FrameBreakdown;
import ru.ruskonnect.smoothgl.profiler.StutterProfiler;

/**
 * Live frame-time breakdown overlay, toggled via F7.
 *
 * <p>Renders into the top-left of the HUD (under any debug overlay).
 * Every line is the rolling average over the profiler window (~240 frames).
 * Bars are coloured to make stage dominance obvious without reading numbers.</p>
 */
public final class BreakdownHud {

    private static final int BAR_WIDTH_PX = 80;
    private static final int PAD = 4;
    private static final int LINE_GAP = 2;
    private static final int BG_COLOR = 0xC0000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int LABEL_COLOR = 0xFFB0B0B0;

    private static final int COL_WORLD  = 0xFF4FC3F7; // light blue
    private static final int COL_GUI    = 0xFF81C784; // green
    private static final int COL_PACER  = 0xFFFFB74D; // orange
    private static final int COL_OTHER  = 0xFFE57373; // red
    private static final int COL_BAR_BG = 0x40FFFFFF;

    private static volatile boolean visible = false;

    public static void toggle() { visible = !visible; }
    public static boolean isVisible() { return visible; }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (!visible) return;
        StutterProfiler profiler = SmoothGLClient.profiler();
        if (profiler == null) return;
        FrameBreakdown b = profiler.breakdown();

        TextRenderer tr = mc.textRenderer;

        double median = profiler.currentMedianMs();
        double p99 = profiler.currentP99Ms();
        double world = b.avgMs(FrameBreakdown.Stage.WORLD);
        double gui   = b.avgMs(FrameBreakdown.Stage.GUI);
        double pacer = b.avgMs(FrameBreakdown.Stage.PACER_WAIT);
        double total = Math.max(median, world + gui + pacer);
        double other = Math.max(0.0, total - world - gui - pacer);

        // Layout: title + 4 stage rows + footer, top-left.
        int rows = 6;
        int height = PAD * 2 + rows * (tr.fontHeight + LINE_GAP) - LINE_GAP;
        int width  = PAD * 2 + 28 /* label */ + BAR_WIDTH_PX + 6 + 70 /* "12.34ms (max …)" */;
        // Top-left placement; if F3 debug overlay is open the user can move it
        // by reconfiguring the keybind. We don't try to detect F3 state because
        // the field name varies across MC versions.
        int x = 4;
        int y = 4;

        ctx.fill(x, y, x + width, y + height, BG_COLOR);

        int cy = y + PAD;
        FramePacer pacerInstance = SmoothGLClient.framePacer();
        String pacerInfo;
        if (pacerInstance == null || !SmoothGLClient.config().enableFramePacer) pacerInfo = "off";
        else if (pacerInstance.isDisabled()) pacerInfo = "n/a";
        else pacerInfo = "depth=" + pacerInstance.maxFramesInFlight();

        ctx.drawTextWithShadow(tr, "§b[SmoothGL] §rmedian=" + fmt(median) + "ms p99=" + fmt(p99)
            + "ms pacer=" + pacerInfo, x + PAD, cy, TEXT_COLOR);
        cy += tr.fontHeight + LINE_GAP;

        cy = drawRow(ctx, tr, x, cy, "world",  world, total, COL_WORLD,  b.maxMs(FrameBreakdown.Stage.WORLD));
        cy = drawRow(ctx, tr, x, cy, "gui",    gui,   total, COL_GUI,    b.maxMs(FrameBreakdown.Stage.GUI));
        cy = drawRow(ctx, tr, x, cy, "pacer",  pacer, total, COL_PACER,  b.maxMs(FrameBreakdown.Stage.PACER_WAIT));
        cy = drawRow(ctx, tr, x, cy, "other",  other, total, COL_OTHER,  -1.0);

        CullStats cs = SmoothGLClient.cullStats();
        var tuner = SmoothGLClient.adaptiveTuner();
        String adapt = (tuner != null && tuner.isAdjusting())
            ? String.format(" §eADAPT E:%.0f B:%.0f P:%.0f§7",
                tuner.effectiveEntityDistance(),
                tuner.effectiveBlockEntityDistance(),
                tuner.effectiveParticleDistance())
            : "";
        ctx.drawTextWithShadow(tr,
            "§7culled E:" + cs.lastFrameEntitiesCulled()
                + " B:" + cs.lastFrameBlockEntitiesCulled()
                + " P:" + cs.lastFrameParticlesCulled()
                + adapt
                + "  samples=" + b.sampleCount() + "  stutters=" + profiler.totalStutters() + "  [F7]",
            x + PAD, cy, LABEL_COLOR);

        // Network row — only when we actually have ping data (i.e. on a server).
        var pm = SmoothGLClient.pingMonitor();
        if (pm != null && pm.hasData()) {
            cy += tr.fontHeight + LINE_GAP;
            int cur = pm.currentMs();
            int med = pm.medianMs();
            int p99Ms = pm.p99Ms();
            double jit = pm.jitterMs();
            // Colour the live value by how it compares to median: green if stable, yellow if 1.5x, red if 2x.
            String colour = (cur >= med * 2 && med > 0) ? "§c"
                          : (cur >= med * 1.5 && med > 0) ? "§e"
                          : "§a";
            ctx.drawTextWithShadow(tr,
                "§7net  " + colour + cur + "ms§7  median=" + med + "  p99=" + p99Ms
                    + "  jitter=±" + String.format("%.1f", jit) + "ms",
                x + PAD, cy, LABEL_COLOR);
        }
    }

    private static int drawRow(DrawContext ctx, TextRenderer tr, int x, int y,
                                String label, double valueMs, double totalMs,
                                int color, double maxMs) {
        ctx.drawTextWithShadow(tr, label, x + PAD, y, LABEL_COLOR);
        int barX = x + PAD + 28;
        int barY = y + 1;
        int barH = tr.fontHeight - 2;
        ctx.fill(barX, barY, barX + BAR_WIDTH_PX, barY + barH, COL_BAR_BG);
        if (totalMs > 0 && valueMs > 0) {
            int filled = (int) Math.round((valueMs / totalMs) * BAR_WIDTH_PX);
            if (filled > BAR_WIDTH_PX) filled = BAR_WIDTH_PX;
            if (filled > 0) ctx.fill(barX, barY, barX + filled, barY + barH, color);
        }
        String txt = fmt(valueMs) + "ms" + (maxMs >= 0 ? " (max " + fmt(maxMs) + ")" : "");
        ctx.drawTextWithShadow(tr, txt, barX + BAR_WIDTH_PX + 6, y, TEXT_COLOR);
        return y + tr.fontHeight + LINE_GAP;
    }

    private static String fmt(double v) { return String.format("%.2f", v); }
}
