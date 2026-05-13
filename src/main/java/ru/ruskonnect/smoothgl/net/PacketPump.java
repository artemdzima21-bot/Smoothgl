package ru.ruskonnect.smoothgl.net;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import ru.ruskonnect.smoothgl.SmoothGL;
import ru.ruskonnect.smoothgl.config.SmoothConfig;

import java.lang.reflect.Field;

/**
 * Drains the incoming packet queue from the render thread, not just from
 * the game-tick boundary.
 *
 * <p>Vanilla architecture: Netty thread receives a packet, schedules its
 * handler on the main thread via {@code NetworkThreadUtils.forceMainThread},
 * which queues the handler in {@link ClientConnection}'s internal task
 * queue. The queue is drained in {@code ClientConnection.tick()}, which
 * vanilla only calls from {@code MinecraftClient.tick()} (20 Hz, every
 * 50 ms).</p>
 *
 * <p>On a 144 Hz client this means a server packet (movement correction,
 * hit confirmation, projectile spawn) can wait up to 50 ms <em>after</em>
 * physical arrival before being processed. That's pure perceived lag
 * with no upside.</p>
 *
 * <p>We call {@code tick()} additionally once per render frame from the
 * same thread. Same main thread, just more frequently. No thread safety
 * regressions — vanilla's invariant is "drained only on main thread",
 * which we preserve.</p>
 *
 * <p><b>What this is NOT:</b> we do not process packets on the Netty
 * thread (that <em>would</em> break things). We do not re-order packets.
 * We do not send fake packets. We just call an existing public vanilla
 * method more often.</p>
 */
public final class PacketPump {

    private static Field connectionField;

    private PacketPump() {}

    /** Called from {@code MinecraftClient.render} HEAD via mixin. */
    public static void pump() {
        SmoothConfig cfg = ru.ruskonnect.smoothgl.SmoothGLClient.config();
        if (cfg == null || !cfg.enablePacketPump) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        // Must be on the render/main thread (vanilla invariant).
        if (!mc.isOnThread()) return;

        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler == null) return;

        ClientConnection conn = resolveConnection(handler);
        if (conn == null) return;

        try {
            conn.tick();
        } catch (Throwable t) {
            SmoothGL.LOGGER.warn("[PacketPump] tick() threw, disabling for this session: {}", t.toString());
            cfg.enablePacketPump = false;
        }
    }

    /**
     * ClientPlayNetworkHandler doesn't expose its connection publicly in
     * 1.21 yarn. We resolve it once via reflection and cache the field.
     * If yarn name shifts in future versions, this becomes a no-op rather
     * than crashing.
     */
    private static ClientConnection resolveConnection(ClientPlayNetworkHandler handler) {
        try {
            if (connectionField == null) {
                // Walk up the class hierarchy until we find a ClientConnection field.
                Class<?> c = handler.getClass();
                while (c != null && connectionField == null) {
                    for (Field f : c.getDeclaredFields()) {
                        if (ClientConnection.class.isAssignableFrom(f.getType())) {
                            f.setAccessible(true);
                            connectionField = f;
                            break;
                        }
                    }
                    c = c.getSuperclass();
                }
                if (connectionField == null) {
                    SmoothGL.LOGGER.warn("[PacketPump] ClientConnection field not found on {}",
                        handler.getClass().getName());
                    return null;
                }
            }
            return (ClientConnection) connectionField.get(handler);
        } catch (Throwable t) {
            return null;
        }
    }
}
