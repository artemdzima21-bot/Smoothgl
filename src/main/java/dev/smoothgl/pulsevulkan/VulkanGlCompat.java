package dev.smoothgl.pulsevulkan;

import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlRenderbuffer;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.VRenderSystem;

public final class VulkanGlCompat {
    private VulkanGlCompat() {}

    public static void activeTexture(int texture) {
        GlTexture.activeTexture(texture);
    }

    public static void depthFunc(int func) {
        VRenderSystem.depthFunc(func);
    }

    public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        VRenderSystem.colorMask(red, green, blue, alpha);
    }

    public static int genFramebuffer() {
        return GlFramebuffer.genFramebufferId();
    }

    public static void bindFramebuffer(int target, int framebuffer) {
        GlFramebuffer.bindFramebuffer(target, framebuffer);
    }

    public static void framebufferTexture2D(int target, int attachment, int textureTarget, int texture, int level) {
        GlFramebuffer.framebufferTexture2D(target, attachment, textureTarget, texture, level);
    }

    public static void framebufferRenderbuffer(int target, int attachment, int renderbufferTarget, int renderbuffer) {
        GlFramebuffer.framebufferRenderbuffer(target, attachment, renderbufferTarget, renderbuffer);
    }

    public static int checkFramebufferStatus(int target) {
        return GlFramebuffer.glCheckFramebufferStatus(target);
    }

    public static void deleteFramebuffer(int framebuffer) {
        GlFramebuffer.deleteFramebuffer(framebuffer);
    }

    public static int genRenderbuffer() {
        return GlRenderbuffer.genId();
    }

    public static void bindRenderbuffer(int target, int renderbuffer) {
        GlRenderbuffer.bindRenderbuffer(target, renderbuffer);
    }

    public static void renderbufferStorage(int target, int internalFormat, int width, int height) {
        GlRenderbuffer.renderbufferStorage(target, internalFormat, width, height);
    }

    public static void deleteRenderbuffer(int renderbuffer) {
        GlRenderbuffer.deleteRenderbuffer(renderbuffer);
    }

    public static void generateMipmap(int target) {
        GlTexture.generateMipmap(target);
    }
}
