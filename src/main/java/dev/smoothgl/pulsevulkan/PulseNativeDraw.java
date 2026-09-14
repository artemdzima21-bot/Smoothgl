package dev.smoothgl.pulsevulkan;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.texture.VTextureSelector;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Conservative bridge for the Pulse path that repeatedly submits GL_TRIANGLES with six vertices.
 * It only accepts float position + UV layouts that can be translated without guessing.
 */
public final class PulseNativeDraw {
    private static final int GL_TRIANGLES = 0x0004;
    private static volatile GraphicsPipeline copyPipeline;
    private static volatile boolean pipelineFailed;

    private PulseNativeDraw() {}

    public static boolean tryDrawArrays(int mode, int first, int count) {
        if (mode != GL_TRIANGLES || count != 6 || first < 0) return false;

        try {
            Layout layout = findLayout();
            if (layout == null) {
                PulseDiagnostics.infoOnce("native-draw-layout-missing",
                        "Pulse native draw waiting for a safe POSITION+UV layout: " + PulseVertexState.describe());
                return false;
            }

            if (VTextureSelector.getBoundTexture(0) == null) {
                PulseDiagnostics.infoOnce("native-draw-texture-missing",
                        "Pulse native draw waiting for Vulkan texture unit 0");
                return false;
            }

            ByteBuffer vertices = translate(layout, first, count);
            if (vertices == null) return false;

            GraphicsPipeline pipeline = getCopyPipeline();
            if (pipeline == null) return false;

            VRenderSystem.setPrimitiveTopologyGL(GL_TRIANGLES);
            Renderer renderer = Renderer.getInstance();
            renderer.bindGraphicsPipeline(pipeline);
            renderer.uploadAndBindUBOs(pipeline);
            Renderer.getDrawer().draw(vertices, VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX, count);

            PulseDiagnostics.infoOnce("native-copy-draw",
                    "Pulse GL_TRIANGLES x6 is using Vulkan native POSITION_TEX draw");
            return true;
        } catch (Throwable error) {
            PulseDiagnostics.infoOnce("native-draw-error-" + error.getClass().getName(),
                    "Pulse native Vulkan draw rejected safely: " + error);
            return false;
        }
    }

    private static Layout findLayout() {
        List<PulseVertexState.Attribute> attributes = PulseVertexState.enabledAttributes();
        PulseVertexState.Attribute position = null;
        PulseVertexState.Attribute uv = null;

        for (PulseVertexState.Attribute attribute : attributes) {
            if (attribute.type() != PulseVertexState.GL_FLOAT) continue;
            String name = ShaderFallback.attribName(attribute.index()).toLowerCase(Locale.ROOT);
            if (position == null && (name.contains("pos") || name.contains("vertex")) && (attribute.size() == 2 || attribute.size() == 3 || attribute.size() == 4)) {
                position = attribute;
            }
            if (uv == null && (name.contains("uv") || name.contains("tex") || name.contains("coord")) && attribute.size() >= 2) {
                uv = attribute;
            }
        }

        if (position == null) {
            for (PulseVertexState.Attribute attribute : attributes) {
                if (attribute.type() == PulseVertexState.GL_FLOAT && (attribute.size() == 2 || attribute.size() == 3 || attribute.size() == 4)) {
                    position = attribute;
                    break;
                }
            }
        }

        if (uv == null) {
            for (PulseVertexState.Attribute attribute : attributes) {
                if (attribute != position && attribute.type() == PulseVertexState.GL_FLOAT && attribute.size() == 2) {
                    uv = attribute;
                    break;
                }
            }
        }

        if (position == null || uv == null) return null;
        if (position.bufferId() == 0 || uv.bufferId() == 0) return null;
        return new Layout(position, uv);
    }

    private static ByteBuffer translate(Layout layout, int first, int count) {
        ByteBuffer posBuffer = PulseBufferFallback.snapshot(layout.position.bufferId());
        ByteBuffer uvBuffer = PulseBufferFallback.snapshot(layout.uv.bufferId());
        if (posBuffer == null || uvBuffer == null) return null;

        float[][] raw = new float[count][3];
        float[][] tex = new float[count][2];
        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < count; i++) {
            int vertex = first + i;
            int posBase = checkedBase(posBuffer, layout.position, vertex, Math.min(layout.position.size(), 3));
            int uvBase = checkedBase(uvBuffer, layout.uv, vertex, 2);
            if (posBase < 0 || uvBase < 0) return null;

            raw[i][0] = posBuffer.getFloat(posBase);
            raw[i][1] = posBuffer.getFloat(posBase + 4);
            raw[i][2] = layout.position.size() >= 3 ? posBuffer.getFloat(posBase + 8) : 0.0f;
            tex[i][0] = uvBuffer.getFloat(uvBase);
            tex[i][1] = uvBuffer.getFloat(uvBase + 4);

            minX = Math.min(minX, raw[i][0]); maxX = Math.max(maxX, raw[i][0]);
            minY = Math.min(minY, raw[i][1]); maxY = Math.max(maxY, raw[i][1]);
        }

        ShaderFallback.Matrix4Value matrix = ShaderFallback.bestTransformMatrix();
        boolean directClip = minX >= -1.25f && maxX <= 1.25f && minY >= -1.25f && maxY <= 1.25f
                && (maxX - minX >= 1.5f || maxY - minY >= 1.5f);
        boolean unitQuad = minX >= -0.1f && maxX <= 1.1f && minY >= -0.1f && maxY <= 1.1f
                && (maxX - minX >= 0.8f && maxY - minY >= 0.8f);

        if (matrix == null && !directClip && !unitQuad) {
            PulseDiagnostics.infoOnce("native-draw-transform-missing",
                    "Pulse native draw captured vertex layout but needs a transform matrix; range x=" + minX + ".." + maxX + " y=" + minY + ".." + maxY);
            return null;
        }

        boolean flipY = !"false".equalsIgnoreCase(System.getProperty("smoothgl.pulse.flipY", "true"));
        ByteBuffer out = ByteBuffer.allocateDirect(count * DefaultVertexFormat.POSITION_TEX.getVertexSize()).order(ByteOrder.nativeOrder());

        for (int i = 0; i < count; i++) {
            float x = raw[i][0], y = raw[i][1], z = raw[i][2];
            if (matrix != null && !directClip) {
                float[] transformed = transform(matrix, x, y, z);
                if (transformed == null) return null;
                x = transformed[0]; y = transformed[1]; z = transformed[2];
            } else if (unitQuad && !directClip) {
                x = x * 2.0f - 1.0f;
                y = y * 2.0f - 1.0f;
            }

            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z) || Math.abs(x) > 8.0f || Math.abs(y) > 8.0f) {
                return null;
            }

            float u = tex[i][0];
            float v = flipY ? 1.0f - tex[i][1] : tex[i][1];
            out.putFloat(x).putFloat(y).putFloat(z).putFloat(u).putFloat(v);
        }
        out.flip();
        return out;
    }

    private static int checkedBase(ByteBuffer buffer, PulseVertexState.Attribute attribute, int vertex, int floatsNeeded) {
        long base = attribute.pointer() + (long) vertex * attribute.effectiveStride();
        long end = base + (long) floatsNeeded * Float.BYTES;
        if (base < 0 || end > buffer.limit() || base > Integer.MAX_VALUE) return -1;
        return (int) base;
    }

    private static float[] transform(ShaderFallback.Matrix4Value matrixValue, float x, float y, float z) {
        float[] m = matrixValue.values();
        if (m == null || m.length < 16) return null;
        float cx, cy, cz, cw;
        if (!matrixValue.transpose()) {
            cx = m[0] * x + m[4] * y + m[8] * z + m[12];
            cy = m[1] * x + m[5] * y + m[9] * z + m[13];
            cz = m[2] * x + m[6] * y + m[10] * z + m[14];
            cw = m[3] * x + m[7] * y + m[11] * z + m[15];
        } else {
            cx = m[0] * x + m[1] * y + m[2] * z + m[3];
            cy = m[4] * x + m[5] * y + m[6] * z + m[7];
            cz = m[8] * x + m[9] * y + m[10] * z + m[11];
            cw = m[12] * x + m[13] * y + m[14] * z + m[15];
        }
        if (!Float.isFinite(cw) || Math.abs(cw) < 1.0e-6f) return null;
        return new float[]{cx / cw, cy / cw, cz / cw};
    }

    private static GraphicsPipeline getCopyPipeline() {
        GraphicsPipeline existing = copyPipeline;
        if (existing != null) return existing;
        if (pipelineFailed) return null;

        synchronized (PulseNativeDraw.class) {
            if (copyPipeline != null) return copyPipeline;
            if (pipelineFailed) return null;
            try {
                String vertexShader = """
                        #version 450
                        layout(location = 0) in vec3 Position;
                        layout(location = 1) in vec2 UV0;
                        layout(location = 0) out vec2 texCoord;
                        void main() {
                            gl_Position = vec4(Position, 1.0);
                            texCoord = UV0;
                        }
                        """;
                String fragmentShader = """
                        #version 450
                        layout(binding = 0) uniform sampler2D Sampler0;
                        layout(location = 0) in vec2 texCoord;
                        layout(location = 0) out vec4 fragColor;
                        void main() {
                            fragColor = texture(Sampler0, texCoord);
                        }
                        """;

                Pipeline.Builder builder = new Pipeline.Builder(DefaultVertexFormat.POSITION_TEX, "smoothgl/pulse_native_copy");
                builder.setUniforms(new ArrayList<>(), List.of(new ImageDescriptor(0, "sampler2D", "Sampler0", 0)));
                builder.compileShaders("smoothgl_pulse_native_copy", vertexShader, fragmentShader);
                copyPipeline = builder.createGraphicsPipeline();
                PulseDiagnostics.infoOnce("native-copy-pipeline", "Pulse Vulkan native copy pipeline compiled");
                return copyPipeline;
            } catch (Throwable error) {
                pipelineFailed = true;
                PulseDiagnostics.infoOnce("native-copy-pipeline-failed", "Pulse Vulkan native copy pipeline failed: " + error);
                return null;
            }
        }
    }

    private record Layout(PulseVertexState.Attribute position, PulseVertexState.Attribute uv) {}
}
