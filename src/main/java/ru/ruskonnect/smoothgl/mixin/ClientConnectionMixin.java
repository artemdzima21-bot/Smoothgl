package ru.ruskonnect.smoothgl.mixin;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.ruskonnect.smoothgl.SmoothGLClient;
import ru.ruskonnect.smoothgl.config.SmoothConfig;
import ru.ruskonnect.smoothgl.net.NetworkTuner;

/**
 * Hook the connection lifecycle to apply socket-level tuning right after
 * the channel becomes active. At this point the Netty channel exists, is
 * connected, and we have a {@code ChannelHandlerContext} to read it from.
 */
@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {

    @Inject(
        method = "channelActive(Lio/netty/channel/ChannelHandlerContext;)V",
        at = @At("HEAD"),
        require = 0
    )
    private void smoothgl$tuneSocket(ChannelHandlerContext ctx, CallbackInfo ci) {
        SmoothConfig cfg = SmoothGLClient.config();
        if (cfg == null) return;
        NetworkTuner.applyTo(ctx.channel(), cfg);
    }
}
