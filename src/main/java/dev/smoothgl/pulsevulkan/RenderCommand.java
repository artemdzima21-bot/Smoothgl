package dev.smoothgl.pulsevulkan;

public interface RenderCommand {
    record EnableBlend() implements RenderCommand {}
    record DisableBlend() implements RenderCommand {}
    record BlendFunc(int srcFactor, int dstFactor) implements RenderCommand {}
    record BlendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) implements RenderCommand {}
    record EnableDepth() implements RenderCommand {}
    record DisableDepth() implements RenderCommand {}
    record DepthMask(boolean enabled) implements RenderCommand {}
    record EnableCull() implements RenderCommand {}
    record DisableCull() implements RenderCommand {}
    record LineWidth(float width) implements RenderCommand {}
    record Scissor(int x, int y, int width, int height) implements RenderCommand {}
    record Viewport(int x, int y, int width, int height) implements RenderCommand {}
    record ClearColor(float red, float green, float blue, float alpha) implements RenderCommand {}
    record Clear(int mask) implements RenderCommand {}
    record BindTexture(int textureId) implements RenderCommand {}
    record ShaderColor(float red, float green, float blue, float alpha) implements RenderCommand {}
}
