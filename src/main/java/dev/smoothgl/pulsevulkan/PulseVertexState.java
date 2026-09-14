package dev.smoothgl.pulsevulkan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks the small subset of VAO state needed to translate Pulse draws to Vulkan. */
public final class PulseVertexState {
    public static final int GL_FLOAT = 0x1406;

    private static final Map<Integer, Attribute> ATTRIBUTES = new ConcurrentHashMap<>();
    private static final Set<Integer> ENABLED = ConcurrentHashMap.newKeySet();

    private PulseVertexState() {}

    public static void enable(int index) {
        ENABLED.add(index);
    }

    public static void disable(int index) {
        ENABLED.remove(index);
    }

    public static void pointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        ATTRIBUTES.put(index, new Attribute(
                index,
                size,
                type,
                normalized,
                stride,
                pointer,
                PulseBufferFallback.boundArrayBuffer()
        ));
    }

    public static List<Attribute> enabledAttributes() {
        List<Attribute> out = new ArrayList<>();
        for (Integer index : ENABLED) {
            Attribute attribute = ATTRIBUTES.get(index);
            if (attribute != null) out.add(attribute);
        }
        out.sort(Comparator.comparingInt(Attribute::index));
        return out;
    }

    public static String describe() {
        StringBuilder out = new StringBuilder();
        for (Attribute attribute : enabledAttributes()) {
            if (!out.isEmpty()) out.append("; ");
            out.append(attribute.index())
                    .append('(')
                    .append(ShaderFallback.attribName(attribute.index()))
                    .append("): size=").append(attribute.size())
                    .append(" type=0x").append(Integer.toHexString(attribute.type()))
                    .append(" stride=").append(attribute.stride())
                    .append(" offset=").append(attribute.pointer())
                    .append(" vbo=").append(attribute.bufferId());
        }
        return out.toString();
    }

    public record Attribute(
            int index,
            int size,
            int type,
            boolean normalized,
            int stride,
            long pointer,
            int bufferId
    ) {
        public int effectiveStride() {
            return stride != 0 ? stride : size * Float.BYTES;
        }
    }
}
