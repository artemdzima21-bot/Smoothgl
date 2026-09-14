package dev.smoothgl.pulsevulkan;

public final class PulseRenderApi {
    private PulseRenderApi() {}

    public static boolean available() { return VulkanDispatch.isReady(); }
    public static void submit(RenderCommand command) { VulkanDispatch.submit(command); }
    public static void enableBlend() { submit(new RenderCommand.EnableBlend()); }
    public static void disableBlend() { submit(new RenderCommand.DisableBlend()); }
    public static void blendFunc(int src, int dst) { submit(new RenderCommand.BlendFunc(src, dst)); }
    public static void blendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) { submit(new RenderCommand.BlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha)); }
    public static void enableDepth() { submit(new RenderCommand.EnableDepth()); }
    public static void disableDepth() { submit(new RenderCommand.DisableDepth()); }
    public static void depthMask(boolean enabled) { submit(new RenderCommand.DepthMask(enabled)); }
    public static void enableCull() { submit(new RenderCommand.EnableCull()); }
    public static void disableCull() { submit(new RenderCommand.DisableCull()); }
    public static void lineWidth(float width) { submit(new RenderCommand.LineWidth(width)); }
    public static void scissor(int x, int y, int width, int height) { submit(new RenderCommand.Scissor(x, y, width, height)); }
    public static void viewport(int x, int y, int width, int height) { submit(new RenderCommand.Viewport(x, y, width, height)); }
    public static void clearColor(float r, float g, float b, float a) { submit(new RenderCommand.ClearColor(r, g, b, a)); }
    public static void clear(int mask) { submit(new RenderCommand.Clear(mask)); }
    public static void bindTexture(int textureId) { submit(new RenderCommand.BindTexture(textureId)); }
    public static void shaderColor(float r, float g, float b, float a) { submit(new RenderCommand.ShaderColor(r, g, b, a)); }
}
