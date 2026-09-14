package dev.smoothgl.pulsevulkan;

public final class PulseFramebufferApi {
    private PulseFramebufferApi() {}

    public static int genFramebuffer() {
        return VulkanGlCompat.genFramebuffer();
    }

    public static void bindFramebuffer(int target, int framebuffer) {
        VulkanGlCompat.bindFramebuffer(target, framebuffer);
    }

    public static void framebufferTexture2D(int target, int attachment, int textureTarget, int texture, int level) {
        VulkanGlCompat.framebufferTexture2D(target, attachment, textureTarget, texture, level);
    }

    public static void framebufferRenderbuffer(int target, int attachment, int renderbufferTarget, int renderbuffer) {
        VulkanGlCompat.framebufferRenderbuffer(target, attachment, renderbufferTarget, renderbuffer);
    }

    public static int checkFramebufferStatus(int target) {
        return VulkanGlCompat.checkFramebufferStatus(target);
    }

    public static void deleteFramebuffer(int framebuffer) {
        VulkanGlCompat.deleteFramebuffer(framebuffer);
    }

    public static int genRenderbuffer() {
        return VulkanGlCompat.genRenderbuffer();
    }

    public static void bindRenderbuffer(int target, int renderbuffer) {
        VulkanGlCompat.bindRenderbuffer(target, renderbuffer);
    }

    public static void renderbufferStorage(int target, int internalFormat, int width, int height) {
        VulkanGlCompat.renderbufferStorage(target, internalFormat, width, height);
    }

    public static void deleteRenderbuffer(int renderbuffer) {
        VulkanGlCompat.deleteRenderbuffer(renderbuffer);
    }
}
