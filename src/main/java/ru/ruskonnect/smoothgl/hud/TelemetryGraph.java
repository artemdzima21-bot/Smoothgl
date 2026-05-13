package ru.ruskonnect.smoothgl.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import ru.ruskonnect.smoothgl.SmoothGLClient;
import ru.ruskonnect.smoothgl.net.PingMonitor;
import ru.ruskonnect.smoothgl.profiler.StutterProfiler;

/**
 * Custom telemetry renderer — our own GPU-rendered widgets, not vanilla
 * F3 text. Two stacked graphs:
 *
 * <ol>
 *   <li><b>Frame-time bars</b> — last 120 frames as vertical bars, height
 *       proportional to ms. Colour-coded against the user's frame budget
 *       target (green = under, yellow = 1×–1.5× target, red = over 1.5×).
 *       Median and target horizontal reference lines overlaid.</li>
 *   <li><b>Ping line</b> — last 64 ping samples as a connected polyline.
 *       Auto-scales to the observed range. Median reference line overlaid.</li>
 * </ol>
 *
 * <p>Renders entirely with {@code DrawContext.fill} (axis-aligned rects).
 * One fill call per pixel column for the frame graph — at 120 columns ×
 * 1 frame budget = ~30 µs total. No allocations in the hot path; the
 * sample buffers are reused.</p>
 */
public final class TelemetryGraph {

    private static final int GRAPH_W = 240;
    private static final int FRAME_GRAPH_H = 40;
    private static final int PING_GRAPH_H = 28;
    private static final int GAP = 4;

    private static final int BG          = 0xB0000000;
    private static final int BORDER      = 0xFF333333;
    private static final int GRID_LINE   = 0x40FFFFFF;
    private static final int TARGET_LINE = 0x60FFFFFF;
    private static final int LABEL_COL   = 0xFFCCCCCC;

    private static final int COL_GOOD    = 0xFF40C040;
    private static final int COL_WARN    = 0xFFC0C040;
    private static final int COL_BAD     = 0xFFC04040;
    private static final int COL_PING    = 0xFF40A0FF;
    private static final int COL_MEDIAN  = 0xFFFFB060;

    // Reused per-frame buffers to keep the renderer allocation-free.
    private static final float[] frameBuf = new float[GRAPH_W];
    private static final int[]   pingBuf  = new int[64];

    private TelemetryGraph() {}

    /** Returns the total height the graph block occupies. */
    public static int totalHeight() {
        return FRAME_GRAPH_H + GAP + PING_GRAPH_H + 2;
    }

    public static int width() { return GRAPH_W; }

    /**
     * Draws both graphs with top-left at (x, y).
     */
    public static void render(DrawContext ctx, TextRenderer tr, int x, int y) {
        renderFrameGraph(ctx, tr, x, y);
        renderPingGraph(ctx, tr, x, y + FRAME_GRAPH_H + GAP);
    }

    private static void renderFrameGraph(DrawContext ctx, TextRenderer tr, int x, int y) {
        StutterProfiler prof = SmoothGLClient.profiler();
        var cfg = SmoothGLClient.config();
        if (prof == null || cfg == null) return;

        int n = prof.copyRecentFrameMs(frameBuf);
        if (n < 2) return;

        // Background + border.
        ctx.fill(x - 1, y - 1, x + GRAPH_W + 1, y + FRAME_GRAPH_H + 1, BORDER);
        ctx.fill(x, y, x + GRAPH_W, y + FRAME_GRAPH_H, BG);

        // Vertical scale: max(observed peak, 2× target) so the budget line is meaningful.
        double target = Math.max(1.0, cfg.adaptiveTargetMs);
        float peak = (float) target * 2f;
        for (int i = 0; i < n; i++) if (frameBuf[i] > peak) peak = frameBuf[i];
        // Cap peak so a single 200ms spike doesn't flatten the rest.
        if (peak > target * 4) peak = (float) (target * 4);

        // Target reference line (horizontal).
        int targetY = y + FRAME_GRAPH_H - (int) ((target / peak) * FRAME_GRAPH_H);
        ctx.fill(x, targetY, x + GRAPH_W, targetY + 1, TARGET_LINE);

        // Bars — one column per pixel; pack the n samples into GRAPH_W columns.
        for (int col = 0; col < GRAPH_W; col++) {
            int sampleIdx = (int) ((long) col * n / GRAPH_W);
            if (sampleIdx >= n) sampleIdx = n - 1;
            float ms = frameBuf[sampleIdx];
            int barH = (int) Math.min(FRAME_GRAPH_H, (ms / peak) * FRAME_GRAPH_H);
            if (barH < 1) barH = 1;
            int colour;
            if (ms <= target) colour = COL_GOOD;
            else if (ms <= target * 1.5) colour = COL_WARN;
            else colour = COL_BAD;
            int px = x + col;
            int py = y + FRAME_GRAPH_H - barH;
            ctx.fill(px, py, px + 1, y + FRAME_GRAPH_H, colour);
        }

        // Labels: peak (top-left), target (next to its line), recent (right).
        ctx.drawTextWithShadow(tr, String.format("%.1fms", peak), x + 2, y + 1, LABEL_COL);
        String tLabel = String.format("%.1f", target);
        ctx.drawTextWithShadow(tr, tLabel, x + GRAPH_W - tr.getWidth(tLabel) - 2, Math.max(y + 1, targetY - tr.fontHeight - 1), LABEL_COL);

        float latest = frameBuf[n - 1];
        String latestStr = String.format("%.1f", latest);
        ctx.drawTextWithShadow(tr, latestStr,
            x + GRAPH_W - tr.getWidth(latestStr) - 2,
            y + FRAME_GRAPH_H - tr.fontHeight - 1,
            latest > target * 1.5 ? COL_BAD : latest > target ? COL_WARN : COL_GOOD);
    }

    private static void renderPingGraph(DrawContext ctx, TextRenderer tr, int x, int y) {
        PingMonitor pm = SmoothGLClient.pingMonitor();
        if (pm == null || !pm.hasData()) return;

        int n = pm.copyRecentSamples(pingBuf);
        if (n < 2) return;

        ctx.fill(x - 1, y - 1, x + GRAPH_W + 1, y + PING_GRAPH_H + 1, BORDER);
        ctx.fill(x, y, x + GRAPH_W, y + PING_GRAPH_H, BG);

        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) { if (pingBuf[i] < min) min = pingBuf[i]; if (pingBuf[i] > max) max = pingBuf[i]; }
        if (max - min < 10) { max = min + 10; } // floor scale so flat lines are visible

        int median = pm.medianMs();
        int medianY = y + PING_GRAPH_H - (int) (((double)(median - min) / (max - min)) * PING_GRAPH_H);
        medianY = Math.max(y, Math.min(y + PING_GRAPH_H - 1, medianY));
        ctx.fill(x, medianY, x + GRAPH_W, medianY + 1, COL_MEDIAN);

        // Polyline as 2-px-wide segments between consecutive sample points.
        int prevX = -1, prevY = -1;
        for (int col = 0; col < GRAPH_W; col++) {
            int sampleIdx = (int) ((long) col * n / GRAPH_W);
            if (sampleIdx >= n) sampleIdx = n - 1;
            int v = pingBuf[sampleIdx];
            int py = y + PING_GRAPH_H - 1 - (int) (((double)(v - min) / (max - min)) * (PING_GRAPH_H - 1));
            int px = x + col;
            if (prevX >= 0) {
                drawLine(ctx, prevX, prevY, px, py, COL_PING);
            }
            prevX = px; prevY = py;
        }

        // Y-range labels (right side, top = max, bottom = min).
        String maxLbl = max + "ms";
        ctx.drawTextWithShadow(tr, maxLbl, x + 2, y + 1, LABEL_COL);
        String minLbl = min + "ms";
        ctx.drawTextWithShadow(tr, minLbl, x + 2, y + PING_GRAPH_H - tr.fontHeight - 1, LABEL_COL);

        String medianStr = "med=" + median;
        ctx.drawTextWithShadow(tr, medianStr, x + GRAPH_W - tr.getWidth(medianStr) - 2, y + 1, COL_MEDIAN);
    }

    /** Cheap Bresenham-ish thin line via axis-aligned 1px fills. */
    private static void drawLine(DrawContext ctx, int x0, int y0, int x1, int y1, int colour) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int cx = x0, cy = y0;
        // Safety cap to avoid pathological infinite loop on bad input.
        int safety = dx + dy + 2;
        while (safety-- > 0) {
            ctx.fill(cx, cy, cx + 1, cy + 1, colour);
            if (cx == x1 && cy == y1) break;
            int e2 = err * 2;
            if (e2 > -dy) { err -= dy; cx += sx; }
            if (e2 <  dx) { err += dx; cy += sy; }
        }
    }
}
