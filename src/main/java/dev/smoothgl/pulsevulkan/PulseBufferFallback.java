package dev.smoothgl.pulsevulkan;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal CPU-side emulation for OpenGL vertex/index buffers that VulkanMod
 * 0.5.4 does not support. VulkanMod's own GlBuffer remains responsible for
 * PIXEL_PACK_BUFFER / PIXEL_UNPACK_BUFFER; this class only handles
 * ARRAY_BUFFER / ELEMENT_ARRAY_BUFFER so third-party renderers do not crash.
 */
public final class PulseBufferFallback {
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;

    private static final Map<Integer, BufferState> BUFFERS = new ConcurrentHashMap<>();
    private static volatile int boundArrayBuffer;
    private static volatile int boundElementArrayBuffer;

    private PulseBufferFallback() {}

    public static boolean handles(int target) {
        return target == GL_ARRAY_BUFFER || target == GL_ELEMENT_ARRAY_BUFFER;
    }

    public static void bind(int target, int buffer) {
        if (!handles(target)) return;

        if (target == GL_ARRAY_BUFFER) {
            boundArrayBuffer = buffer;
        } else {
            boundElementArrayBuffer = buffer;
        }

        if (buffer != 0) {
            BUFFERS.computeIfAbsent(buffer, BufferState::new).target = target;
        }

        PulseDiagnostics.infoOnce(
                "gl15-array-buffer-fallback",
                "GL15 ARRAY/ELEMENT buffer compatibility enabled"
        );
    }

    public static void bufferData(int target, ByteBuffer data, int usage) {
        BufferState state = requireBound(target);
        state.usage = usage;
        if (data == null) {
            state.data = ByteBuffer.allocateDirect(0);
            return;
        }

        ByteBuffer src = data.duplicate();
        ByteBuffer copy = ByteBuffer.allocateDirect(src.remaining());
        copy.put(src);
        copy.flip();
        state.data = copy;
    }

    public static void bufferData(int target, long size, int usage) {
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Unsupported buffer size: " + size);
        }
        BufferState state = requireBound(target);
        state.usage = usage;
        state.data = ByteBuffer.allocateDirect((int) size);
    }

    public static ByteBuffer mapBuffer(int target) {
        BufferState state = requireBound(target);
        if (state.data == null) {
            state.data = ByteBuffer.allocateDirect(0);
        }
        ByteBuffer view = state.data.duplicate();
        view.clear();
        return view;
    }

    public static void delete(int id) {
        BUFFERS.remove(id);
        if (boundArrayBuffer == id) boundArrayBuffer = 0;
        if (boundElementArrayBuffer == id) boundElementArrayBuffer = 0;
    }

    public static int boundArrayBuffer() {
        return boundArrayBuffer;
    }

    public static int boundElementArrayBuffer() {
        return boundElementArrayBuffer;
    }

    public static int sizeOf(int id) {
        BufferState state = BUFFERS.get(id);
        return state == null || state.data == null ? 0 : state.data.capacity();
    }

    private static BufferState requireBound(int target) {
        if (!handles(target)) {
            throw new IllegalArgumentException("Target is not handled by Pulse fallback: " + target);
        }

        int id = target == GL_ARRAY_BUFFER ? boundArrayBuffer : boundElementArrayBuffer;
        if (id == 0) {
            throw new IllegalStateException("No buffer bound for target " + target);
        }
        return BUFFERS.computeIfAbsent(id, BufferState::new);
    }

    private static final class BufferState {
        final int id;
        volatile int target;
        volatile int usage;
        volatile ByteBuffer data;

        BufferState(int id) {
            this.id = id;
        }
    }
}
