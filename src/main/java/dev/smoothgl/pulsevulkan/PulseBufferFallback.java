package dev.smoothgl.pulsevulkan;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CPU-side emulation for OpenGL vertex/index buffers that VulkanMod 0.5.4
 * does not support. PIXEL_PACK/UNPACK remain handled by VulkanMod.
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
        if (target == GL_ARRAY_BUFFER) boundArrayBuffer = buffer;
        else boundElementArrayBuffer = buffer;
        if (buffer != 0) BUFFERS.computeIfAbsent(buffer, BufferState::new).target = target;
        PulseDiagnostics.infoOnce("gl15-array-buffer-fallback", "GL15 ARRAY/ELEMENT buffer compatibility enabled");
    }

    public static void bufferData(int target, ByteBuffer data, int usage) {
        store(target, copy(data), usage);
    }

    public static void bufferData(int target, ShortBuffer data, int usage) {
        if (data == null) { store(target, ByteBuffer.allocateDirect(0), usage); return; }
        ShortBuffer src = data.duplicate();
        ByteBuffer out = direct(src.remaining() * Short.BYTES);
        while (src.hasRemaining()) out.putShort(src.get());
        out.flip();
        store(target, out, usage);
    }

    public static void bufferData(int target, IntBuffer data, int usage) {
        if (data == null) { store(target, ByteBuffer.allocateDirect(0), usage); return; }
        IntBuffer src = data.duplicate();
        ByteBuffer out = direct(src.remaining() * Integer.BYTES);
        while (src.hasRemaining()) out.putInt(src.get());
        out.flip();
        store(target, out, usage);
    }

    public static void bufferData(int target, LongBuffer data, int usage) {
        if (data == null) { store(target, ByteBuffer.allocateDirect(0), usage); return; }
        LongBuffer src = data.duplicate();
        ByteBuffer out = direct(src.remaining() * Long.BYTES);
        while (src.hasRemaining()) out.putLong(src.get());
        out.flip();
        store(target, out, usage);
    }

    public static void bufferData(int target, FloatBuffer data, int usage) {
        if (data == null) { store(target, ByteBuffer.allocateDirect(0), usage); return; }
        FloatBuffer src = data.duplicate();
        ByteBuffer out = direct(src.remaining() * Float.BYTES);
        while (src.hasRemaining()) out.putFloat(src.get());
        out.flip();
        store(target, out, usage);
    }

    public static void bufferData(int target, DoubleBuffer data, int usage) {
        if (data == null) { store(target, ByteBuffer.allocateDirect(0), usage); return; }
        DoubleBuffer src = data.duplicate();
        ByteBuffer out = direct(src.remaining() * Double.BYTES);
        while (src.hasRemaining()) out.putDouble(src.get());
        out.flip();
        store(target, out, usage);
    }

    public static void bufferData(int target, long size, int usage) {
        if (size < 0 || size > Integer.MAX_VALUE) throw new IllegalArgumentException("Unsupported buffer size: " + size);
        store(target, direct((int) size), usage);
    }

    public static ByteBuffer mapBuffer(int target) {
        BufferState state = requireBound(target);
        if (state.data == null) state.data = direct(0);
        ByteBuffer view = state.data.duplicate().order(ByteOrder.nativeOrder());
        view.clear();
        return view;
    }

    public static void delete(int id) {
        BUFFERS.remove(id);
        if (boundArrayBuffer == id) boundArrayBuffer = 0;
        if (boundElementArrayBuffer == id) boundElementArrayBuffer = 0;
    }

    public static int boundArrayBuffer() { return boundArrayBuffer; }
    public static int boundElementArrayBuffer() { return boundElementArrayBuffer; }

    public static int sizeOf(int id) {
        BufferState state = BUFFERS.get(id);
        return state == null || state.data == null ? 0 : state.data.capacity();
    }

    private static ByteBuffer copy(ByteBuffer data) {
        if (data == null) return direct(0);
        ByteBuffer src = data.duplicate();
        ByteBuffer copy = direct(src.remaining());
        copy.put(src);
        copy.flip();
        return copy;
    }

    private static ByteBuffer direct(int bytes) {
        return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
    }

    private static void store(int target, ByteBuffer data, int usage) {
        BufferState state = requireBound(target);
        state.usage = usage;
        state.data = data;
    }

    private static BufferState requireBound(int target) {
        if (!handles(target)) throw new IllegalArgumentException("Target is not handled by Pulse fallback: " + target);
        int id = target == GL_ARRAY_BUFFER ? boundArrayBuffer : boundElementArrayBuffer;
        if (id == 0) throw new IllegalStateException("No buffer bound for target " + target);
        return BUFFERS.computeIfAbsent(id, BufferState::new);
    }

    private static final class BufferState {
        final int id;
        volatile int target;
        volatile int usage;
        volatile ByteBuffer data;
        BufferState(int id) { this.id = id; }
    }
}
