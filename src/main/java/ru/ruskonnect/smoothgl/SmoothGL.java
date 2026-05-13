package ru.ruskonnect.smoothgl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared constants for the SmoothGL mod.
 *
 * <p>This mod is intentionally narrow in scope. It does NOT promise magical
 * FPS gains. It targets concrete, measurable sources of micro-stutter on the
 * OpenGL render path:</p>
 *
 * <ul>
 *     <li>JIT tier-up spikes during the first seconds of play</li>
 *     <li>{@link System#gc()} stop-the-world pauses</li>
 *     <li>First-time direct {@link java.nio.ByteBuffer} allocation spikes
 *         when chunk meshes start streaming</li>
 *     <li>Frame-time outliers ("micro-freezes"), which are profiled and
 *         reported so the user can verify what is (and isn't) actually
 *         improving.</li>
 * </ul>
 */
public final class SmoothGL {
    public static final String MOD_ID = "smoothgl";
    public static final Logger LOGGER = LoggerFactory.getLogger("SmoothGL");

    private SmoothGL() {}
}
