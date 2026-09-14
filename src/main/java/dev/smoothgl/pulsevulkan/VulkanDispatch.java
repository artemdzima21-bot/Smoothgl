package dev.smoothgl.pulsevulkan;

import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;

public final class VulkanDispatch {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();
    private static final Map<String, MethodHandle> HANDLES = new HashMap<>();
    private static volatile boolean ready;

    private VulkanDispatch() {}

    public static boolean bootstrap() {
        try {
            bind("enableBlend", "net.vulkanmod.vulkan.VRenderSystem", "enableBlend", void.class);
            bind("disableBlend", "net.vulkanmod.vulkan.VRenderSystem", "disableBlend", void.class);
            bind("blendFunc", "net.vulkanmod.vulkan.VRenderSystem", "blendFunc", void.class, int.class, int.class);
            bind("blendFuncSeparate", "net.vulkanmod.vulkan.VRenderSystem", "blendFuncSeparate", void.class, int.class, int.class, int.class, int.class);
            bind("enableDepth", "net.vulkanmod.vulkan.VRenderSystem", "enableDepthTest", void.class);
            bind("disableDepth", "net.vulkanmod.vulkan.VRenderSystem", "disableDepthTest", void.class);
            bind("depthMask", "net.vulkanmod.vulkan.VRenderSystem", "depthMask", void.class, boolean.class);
            bind("enableCull", "net.vulkanmod.vulkan.VRenderSystem", "enableCull", void.class);
            bind("disableCull", "net.vulkanmod.vulkan.VRenderSystem", "disableCull", void.class);
            bind("lineWidth", "net.vulkanmod.vulkan.VRenderSystem", "setLineWidth", void.class, float.class);
            bind("clearColor", "net.vulkanmod.vulkan.VRenderSystem", "setClearColor", void.class, float.class, float.class, float.class, float.class);
            bind("clear", "net.vulkanmod.vulkan.VRenderSystem", "clear", void.class, int.class);
            bind("shaderColor", "net.vulkanmod.vulkan.VRenderSystem", "setShaderColor", void.class, float.class, float.class, float.class, float.class);
            bind("scissor", "net.vulkanmod.vulkan.Renderer", "setScissor", void.class, int.class, int.class, int.class, int.class);
            bind("viewport", "net.vulkanmod.vulkan.Renderer", "setViewport", void.class, int.class, int.class, int.class, int.class);
            bind("bindTexture", "net.vulkanmod.gl.VkGlTexture", "bindTexture", void.class, int.class);
            ready = true;
            return true;
        } catch (Throwable error) {
            HANDLES.clear();
            ready = false;
            System.err.println("[PulseVulkanBridge] Vulkan binding failed: " + error);
            return false;
        }
    }

    public static boolean isReady() { return ready; }

    public static void submit(RenderCommand command) {
        if (!ready) return;
        if (RenderSystem.isOnRenderThread()) execute(command);
        else RenderSystem.recordRenderCall(() -> execute(command));
    }

    private static void execute(RenderCommand command) {
        try {
            if (command instanceof RenderCommand.EnableBlend) invoke("enableBlend");
            else if (command instanceof RenderCommand.DisableBlend) invoke("disableBlend");
            else if (command instanceof RenderCommand.BlendFunc c) invoke("blendFunc", c.srcFactor(), c.dstFactor());
            else if (command instanceof RenderCommand.BlendFuncSeparate c) invoke("blendFuncSeparate", c.srcRgb(), c.dstRgb(), c.srcAlpha(), c.dstAlpha());
            else if (command instanceof RenderCommand.EnableDepth) invoke("enableDepth");
            else if (command instanceof RenderCommand.DisableDepth) invoke("disableDepth");
            else if (command instanceof RenderCommand.DepthMask c) invoke("depthMask", c.enabled());
            else if (command instanceof RenderCommand.EnableCull) invoke("enableCull");
            else if (command instanceof RenderCommand.DisableCull) invoke("disableCull");
            else if (command instanceof RenderCommand.LineWidth c) invoke("lineWidth", c.width());
            else if (command instanceof RenderCommand.Scissor c) invoke("scissor", c.x(), c.y(), c.width(), c.height());
            else if (command instanceof RenderCommand.Viewport c) invoke("viewport", c.x(), c.y(), c.width(), c.height());
            else if (command instanceof RenderCommand.ClearColor c) invoke("clearColor", c.red(), c.green(), c.blue(), c.alpha());
            else if (command instanceof RenderCommand.Clear c) invoke("clear", c.mask());
            else if (command instanceof RenderCommand.BindTexture c) invoke("bindTexture", c.textureId());
            else if (command instanceof RenderCommand.ShaderColor c) invoke("shaderColor", c.red(), c.green(), c.blue(), c.alpha());
        } catch (Throwable error) {
            System.err.println("[PulseVulkanBridge] Command failed: " + command + " -> " + error);
        }
    }

    private static void bind(String key, String className, String methodName, Class<?> returnType, Class<?>... args) throws Throwable {
        Class<?> owner = Class.forName(className);
        HANDLES.put(key, LOOKUP.findStatic(owner, methodName, MethodType.methodType(returnType, args)));
    }

    private static void invoke(String key, Object... args) throws Throwable {
        MethodHandle handle = HANDLES.get(key);
        if (handle == null) throw new IllegalStateException("Missing binding: " + key);
        handle.invokeWithArguments(args);
    }
}
