package ru.ruskonnect.smoothgl.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import ru.ruskonnect.smoothgl.SmoothGL;
import ru.ruskonnect.smoothgl.config.SmoothConfig;

import java.util.concurrent.Executor;

/**
 * Client-side network tuning. Applied once per connection in
 * {@link ru.ruskonnect.smoothgl.mixin.ClientConnectionMixin}, right after
 * the Netty channel becomes active.
 *
 * <p><b>What this does NOT do:</b> reduce the actual round-trip latency to
 * the server. That is bounded by physics + routing. No client mod can beat
 * the speed of light.</p>
 *
 * <p><b>What this DOES do</b> (perceived input lag improvements):</p>
 * <ul>
 *   <li>Forces {@code TCP_NODELAY} (disables Nagle batching of small packets).
 *       Vanilla already does this on most builds; this is a belt-and-braces
 *       enforcement that survives proxies that strip the option.</li>
 *   <li>Sets {@code IP_TOS=0x10} (IPTOS_LOWDELAY DSCP marking). Routers that
 *       honour DSCP will prioritize our packets. Most don't, but it costs us
 *       nothing.</li>
 *   <li>Bumps the kernel send/receive buffers so a momentary congestion
 *       spike doesn't immediately cause retransmits.</li>
 *   <li>Raises the Netty event-loop thread priority to {@code MAX_PRIORITY}
 *       so packet processing competes less with the renderer.</li>
 * </ul>
 *
 * <p><b>What is risky</b> (not enabled by default; gated by
 * {@code enableAggressiveNetwork}):</p>
 * <ul>
 *   <li>Per-send flush — disables Netty's natural write coalescing. Sends
 *       packets out the wire immediately. Can trigger anti-cheat heuristics
 *       on servers that watch for unusual packet timing.</li>
 * </ul>
 */
public final class NetworkTuner {

    private NetworkTuner() {}

    public static void applyTo(Channel channel, SmoothConfig cfg) {
        if (channel == null || !cfg.enableNetworkTuning) return;
        try {
            // Belt-and-braces: ensure TCP_NODELAY regardless of what vanilla/proxy did.
            channel.config().setOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);

            if (cfg.networkLowDelayTos) {
                // IPTOS_LOWDELAY = 0x10. Some routers / ISPs honour this DSCP bit.
                channel.config().setOption(ChannelOption.IP_TOS, 0x10);
            }

            int bufBytes = Math.max(64 * 1024, cfg.networkBufferKiB * 1024);
            channel.config().setOption(ChannelOption.SO_RCVBUF, bufBytes);
            channel.config().setOption(ChannelOption.SO_SNDBUF, bufBytes);

            // Keep-alive on so dead connections drop fast (server side, but harmless).
            channel.config().setOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);

            if (cfg.networkRaisePriority) {
                raiseEventLoopPriority(channel);
            }

            SmoothGL.LOGGER.info("[NetworkTuner] Applied to {} (NODELAY=on, TOS={}, buf={}KiB)",
                channel.remoteAddress(), cfg.networkLowDelayTos ? "0x10" : "default", bufBytes / 1024);
        } catch (Throwable t) {
            // Never let a tuning failure break the connection.
            SmoothGL.LOGGER.warn("[NetworkTuner] Failed to apply options: {}", t.toString());
        }
    }

    /**
     * Netty's NioEventLoop runs a single thread. We can't easily set its priority
     * from outside, but we can submit a task that runs ON that thread and bumps
     * itself.
     */
    private static void raiseEventLoopPriority(Channel channel) {
        EventLoop loop = channel.eventLoop();
        if (loop == null) return;
        Executor exec = loop;
        exec.execute(() -> {
            try {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            } catch (SecurityException ignored) {
                // Some sandboxes block setPriority; not fatal.
            }
        });
    }
}
