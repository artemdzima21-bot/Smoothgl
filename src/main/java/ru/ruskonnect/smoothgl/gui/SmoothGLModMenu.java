package ru.ruskonnect.smoothgl.gui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu integration entrypoint. Only loaded if ModMenu is present on the
 * classpath; safe to ship without the runtime dep because Fabric Loader
 * only constructs entrypoints whose mod is actually loaded.
 */
public final class SmoothGLModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SmoothConfigScreen::new;
    }
}
