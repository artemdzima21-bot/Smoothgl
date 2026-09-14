package dev.smoothgl.pulsevulkan;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import java.util.Locale;

public final class PulseVisualsDetector {
    private PulseVisualsDetector() {}

    public static boolean isPresent() {
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            String id = mod.getMetadata().getId().toLowerCase(Locale.ROOT);
            String name = mod.getMetadata().getName().toLowerCase(Locale.ROOT);
            if (id.equals("pulsevisuals") || id.equals("pulse_visuals") || id.equals("pulse-visuals") || (id.contains("pulse") && name.contains("visual"))) {
                return true;
            }
        }
        return false;
    }
}
