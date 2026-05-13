package ru.ruskonnect.smoothgl.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import ru.ruskonnect.smoothgl.SmoothGLClient;
import ru.ruskonnect.smoothgl.config.SmoothConfig;

/**
 * Minimal vanilla-widget config screen. Avoids cloth-config to keep the
 * mod self-contained — no extra runtime dependencies for users.
 *
 * <p>Layout: two columns of widgets, OK/Cancel-style "Done" button at
 * the bottom that saves the config to disk.</p>
 */
public final class SmoothConfigScreen extends Screen {

    private final Screen parent;
    private final SmoothConfig cfg;

    public SmoothConfigScreen(Screen parent) {
        super(Text.literal("SmoothGL"));
        this.parent = parent;
        this.cfg = SmoothGLClient.config();
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = 40;
        int rowH = 24;
        int btnW = 200;
        int colSpacing = 6;

        // Toggles (boolean buttons).
        addToggle(cx - btnW - colSpacing / 2, y, btnW, "Profiler",
            () -> cfg.enableProfiler, v -> cfg.enableProfiler = v);
        addToggle(cx + colSpacing / 2, y, btnW, "Frame Pacer",
            () -> cfg.enableFramePacer, v -> cfg.enableFramePacer = v);
        y += rowH;

        addToggle(cx - btnW - colSpacing / 2, y, btnW, "Entity Cull",
            () -> cfg.enableEntityCull, v -> cfg.enableEntityCull = v);
        addToggle(cx + colSpacing / 2, y, btnW, "Block Entity Cull",
            () -> cfg.enableBlockEntityCull, v -> cfg.enableBlockEntityCull = v);
        y += rowH;

        addToggle(cx - btnW - colSpacing / 2, y, btnW, "Particle Cull",
            () -> cfg.enableParticleCull, v -> cfg.enableParticleCull = v);
        addToggle(cx + colSpacing / 2, y, btnW, "Layer Cull (armor/cape)",
            () -> cfg.enableLayerCull, v -> cfg.enableLayerCull = v);
        y += rowH;

        addToggle(cx - btnW - colSpacing / 2, y, btnW, "Adaptive Tuner",
            () -> cfg.enableAdaptive, v -> cfg.enableAdaptive = v);
        addToggle(cx + colSpacing / 2, y, btnW, "Particle Size Weighting",
            () -> cfg.enableParticleSizeWeighting, v -> cfg.enableParticleSizeWeighting = v);
        y += rowH + 4;

        // Distance sliders.
        addSlider(cx - btnW - colSpacing / 2, y, btnW, "Entity Cull Distance", cfg.entityCullDistance, 16, 256,
            v -> cfg.entityCullDistance = v);
        addSlider(cx + colSpacing / 2, y, btnW, "Block Entity Cull Distance", cfg.blockEntityCullDistance, 12, 192,
            v -> cfg.blockEntityCullDistance = v);
        y += rowH;

        addSlider(cx - btnW - colSpacing / 2, y, btnW, "Particle Cull Distance", cfg.particleCullDistance, 8, 128,
            v -> cfg.particleCullDistance = v);
        addSlider(cx + colSpacing / 2, y, btnW, "Layer Cull Distance", cfg.layerCullDistance, 8, 128,
            v -> cfg.layerCullDistance = v);
        y += rowH;

        addSlider(cx - btnW - colSpacing / 2, y, btnW, "Adaptive Target (ms)", cfg.adaptiveTargetMs, 4.0, 50.0,
            v -> cfg.adaptiveTargetMs = v);
        addSlider(cx + colSpacing / 2, y, btnW, "Adaptive Min Distance", cfg.adaptiveMinDistance, 4, 64,
            v -> cfg.adaptiveMinDistance = v);
        y += rowH + 8;

        // Done.
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> {
            cfg.save();
            if (this.client != null) this.client.setScreen(parent);
        }).dimensions(cx - 100, y, 200, 20).build());
    }

    private void addToggle(int x, int y, int w, String label,
                            java.util.function.BooleanSupplier getter,
                            java.util.function.Consumer<Boolean> setter) {
        addDrawableChild(ButtonWidget.builder(
            Text.literal(label + ": " + (getter.getAsBoolean() ? "ON" : "OFF")),
            b -> {
                boolean nv = !getter.getAsBoolean();
                setter.accept(nv);
                b.setMessage(Text.literal(label + ": " + (nv ? "ON" : "OFF")));
            }
        ).dimensions(x, y, w, 20).build());
    }

    private void addSlider(int x, int y, int w, String label,
                            double current, double min, double max,
                            java.util.function.DoubleConsumer setter) {
        double normalized = (current - min) / (max - min);
        addDrawableChild(new SliderWidget(x, y, w, 20,
            Text.literal(String.format("%s: %.1f", label, current)),
            Math.max(0.0, Math.min(1.0, normalized))) {
            @Override
            protected void updateMessage() {
                double v = min + value * (max - min);
                setMessage(Text.literal(String.format("%s: %.1f", label, v)));
            }
            @Override
            protected void applyValue() {
                double v = min + value * (max - min);
                setter.accept(v);
            }
        });
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 16, 0xFFFFFF);
    }

    @Override
    public void close() {
        cfg.save();
        if (this.client != null) this.client.setScreen(parent);
    }
}
