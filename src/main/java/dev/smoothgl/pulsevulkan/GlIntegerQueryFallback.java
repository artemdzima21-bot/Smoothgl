package dev.smoothgl.pulsevulkan;

import net.minecraft.client.Minecraft;

import java.nio.IntBuffer;

/** Vulkan-safe replacement for OpenGL integer state queries used by Pulse. */
public final class GlIntegerQueryFallback {
    private static final int GL_VIEWPORT = 0x0BA2;
    private static final int GL_SCISSOR_BOX = 0x0C10;
    private static final int GL_PACK_ALIGNMENT = 0x0D05;
    private static final int GL_UNPACK_ALIGNMENT = 0x0CF5;
    private static final int GL_MAX_TEXTURE_SIZE = 0x0D33;
    private static final int GL_MAX_VIEWPORT_DIMS = 0x0D3A;
    private static final int GL_TEXTURE_BINDING_2D = 0x8069;
    private static final int GL_ACTIVE_TEXTURE = 0x84E0;
    private static final int GL_TEXTURE0 = 0x84C0;
    private static final int GL_VERTEX_ARRAY_BINDING = 0x85B5;
    private static final int GL_MAX_DRAW_BUFFERS = 0x8824;
    private static final int GL_MAX_VERTEX_ATTRIBS = 0x8869;
    private static final int GL_MAX_TEXTURE_IMAGE_UNITS = 0x8872;
    private static final int GL_ARRAY_BUFFER_BINDING = 0x8894;
    private static final int GL_ELEMENT_ARRAY_BUFFER_BINDING = 0x8895;
    private static final int GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS = 0x8B4D;
    private static final int GL_CURRENT_PROGRAM = 0x8B8D;
    private static final int GL_FRAMEBUFFER_BINDING = 0x8CA6;
    private static final int GL_RENDERBUFFER_BINDING = 0x8CA7;
    private static final int GL_READ_FRAMEBUFFER_BINDING = 0x8CAA;
    private static final int GL_MAX_COLOR_ATTACHMENTS = 0x8CDF;
    private static final int GL_MAX_SAMPLES = 0x8D57;
    private static final int GL_MAJOR_VERSION = 0x821B;
    private static final int GL_MINOR_VERSION = 0x821C;
    private static final int GL_NUM_EXTENSIONS = 0x821D;

    private GlIntegerQueryFallback() {}

    public static int scalar(int pname) {
        int[] values = new int[4];
        fill(pname, values);
        return values[0];
    }

    public static void fill(int pname, int[] out) {
        if (out == null || out.length == 0) return;
        for (int i = 0; i < out.length; i++) out[i] = 0;

        switch (pname) {
            case GL_VIEWPORT, GL_SCISSOR_BOX -> {
                int[] size = framebufferSize();
                put(out, 0, 0); put(out, 1, 0); put(out, 2, size[0]); put(out, 3, size[1]);
            }
            case GL_MAX_VIEWPORT_DIMS -> { put(out, 0, 16384); put(out, 1, 16384); }
            case GL_MAX_TEXTURE_SIZE -> put(out, 0, 16384);
            case GL_MAX_VERTEX_ATTRIBS, GL_MAX_TEXTURE_IMAGE_UNITS -> put(out, 0, 16);
            case GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS -> put(out, 0, 32);
            case GL_MAX_DRAW_BUFFERS, GL_MAX_COLOR_ATTACHMENTS -> put(out, 0, 8);
            case GL_ACTIVE_TEXTURE -> put(out, 0, GL_TEXTURE0);
            case GL_CURRENT_PROGRAM -> put(out, 0, ShaderFallback.currentProgram());
            case GL_ARRAY_BUFFER_BINDING -> put(out, 0, PulseBufferFallback.boundArrayBuffer());
            case GL_ELEMENT_ARRAY_BUFFER_BINDING -> put(out, 0, PulseBufferFallback.boundElementArrayBuffer());
            case GL_PACK_ALIGNMENT, GL_UNPACK_ALIGNMENT -> put(out, 0, 4);
            case GL_MAJOR_VERSION -> put(out, 0, 4);
            case GL_MINOR_VERSION -> put(out, 0, 6);
            case GL_MAX_SAMPLES -> put(out, 0, 1);
            case GL_TEXTURE_BINDING_2D, GL_VERTEX_ARRAY_BINDING, GL_FRAMEBUFFER_BINDING,
                 GL_RENDERBUFFER_BINDING, GL_READ_FRAMEBUFFER_BINDING, GL_NUM_EXTENSIONS -> put(out, 0, 0);
            default -> PulseDiagnostics.fallback("glGetIntegerv(pname=0x" + Integer.toHexString(pname) + ") emulated as zero");
        }
    }

    public static void fill(int pname, IntBuffer out) {
        if (out == null || !out.hasRemaining()) return;
        int count = Math.min(out.remaining(), 4);
        int[] values = new int[count];
        fill(pname, values);
        int pos = out.position();
        for (int i = 0; i < count; i++) out.put(pos + i, values[i]);
    }

    private static int[] framebufferSize() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.getWindow() != null) {
                return new int[] { Math.max(1, minecraft.getWindow().getWidth()), Math.max(1, minecraft.getWindow().getHeight()) };
            }
        } catch (Throwable ignored) {}
        return new int[] { 1, 1 };
    }

    private static void put(int[] out, int index, int value) {
        if (index < out.length) out[index] = value;
    }
}
