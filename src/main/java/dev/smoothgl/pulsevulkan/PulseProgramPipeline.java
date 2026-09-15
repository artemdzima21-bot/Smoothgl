package dev.smoothgl.pulsevulkan;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.converter.GlslConverter;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.shader.layout.Uniform;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lazily converts the GLSL captured from a linked Pulse program through
 * VulkanMod's own legacy GLSL converter. No Pulse source is written to disk.
 */
public final class PulseProgramPipeline {
    private static final Map<Integer, Entry> ENTRIES = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> FAILED_SOURCE_HASHES = new ConcurrentHashMap<>();

    private static final Pattern FRAGMENT_OUT = Pattern.compile(
            "(?m)^\\s*(?:layout\\s*\\([^)]*\\)\\s*)?out\\s+vec4\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*;");
    private static final Pattern VERTEX_IN = Pattern.compile(
            "(?m)^\\s*in\\s+(?:lowp\\s+|mediump\\s+|highp\\s+)?[A-Za-z_][A-Za-z0-9_]*\\s+[A-Za-z_][A-Za-z0-9_]*\\s*;");
    private static final Pattern NON_SAMPLER_UNIFORM = Pattern.compile(
            "(?m)^\\s*uniform\\s+(?!sampler)[A-Za-z_][A-Za-z0-9_]*\\s+[A-Za-z_][A-Za-z0-9_]*\\s*;");
    private static final Pattern UNSUPPORTED_UNIFORM = Pattern.compile(
            "(?m)^\\s*uniform\\s+(?:bool|ivec[234]|uvec[234]|mat2|mat[234]x[234]|samplerCube|sampler3D|sampler2DArray|image\\w*)\\b");

    private PulseProgramPipeline() {}

    public static Prepared prepare(int program) {
        if (program == 0) return null;
        ShaderFallback.ProgramSources sources = ShaderFallback.programSources(program);
        if (sources == null || !sources.complete()) return null;

        int sourceHash = sourceHash(sources);
        if (FAILED_SOURCE_HASHES.getOrDefault(program, 0) == sourceHash) return null;

        Entry existing = ENTRIES.get(program);
        if (existing != null && existing.sourceHash == sourceHash) {
            if (existing.samplerSignature() == samplerSignature(program, existing.samplerNames)) {
                existing.updateUniforms();
                return existing.texturesReady() ? new Prepared(existing.pipeline) : null;
            }
            invalidate(program);
        }

        try {
            Entry compiled = compile(program, sources, sourceHash);
            ENTRIES.put(program, compiled);
            FAILED_SOURCE_HASHES.remove(program);
            compiled.updateUniforms();
            PulseDiagnostics.infoOnce("pulse-program-native-" + program,
                    "Pulse program " + program + " compiled through VulkanMod GLSL -> SPIR-V pipeline");
            return compiled.texturesReady() ? new Prepared(compiled.pipeline) : null;
        } catch (Throwable error) {
            FAILED_SOURCE_HASHES.put(program, sourceHash);
            String message = error.getMessage();
            if (message == null || message.isBlank()) message = error.getClass().getSimpleName();
            if (message.length() > 220) message = message.substring(0, 220);
            PulseDiagnostics.infoOnce("pulse-program-compile-failed-" + program + "-" + sourceHash,
                    "Pulse program " + program + " Vulkan shader conversion rejected; using conservative fallback: " + message);
            return null;
        }
    }

    public static void invalidate(int program) {
        Entry removed = ENTRIES.remove(program);
        FAILED_SOURCE_HASHES.remove(program);
        if (removed != null) {
            try {
                removed.pipeline.scheduleCleanUp();
            } catch (Throwable ignored) {
            }
        }
    }

    private static Entry compile(int program, ShaderFallback.ProgramSources sources, int sourceHash) {
        String vertex = normalizeVertex(sources.vertexSource());
        String fragment = normalizeFragment(sources.fragmentSource());
        validateForVulkanModConverter(vertex, fragment);

        // VulkanMod's converter always emits a binding-0 UBO. Make sure it is
        // non-empty even for a shader that only uses samplers/constants.
        if (!NON_SAMPLER_UNIFORM.matcher(vertex).find() && !NON_SAMPLER_UNIFORM.matcher(fragment).find()) {
            vertex = "uniform float smoothglDummy;\n" + vertex;
        }

        GlslConverter converter = new GlslConverter();
        converter.process(vertex, fragment);

        UBO ubo = converter.createUBO();
        Map<String, UniformSlot> slots = new HashMap<>();
        for (Uniform uniform : ubo.getUniforms()) {
            int size = Math.max(4, uniform.getSize() * Float.BYTES);
            MappedBuffer mapped = new MappedBuffer(size);
            MemoryUtil.memSet(mapped.ptr, 0, size);
            uniform.setSupplier(() -> mapped);
            slots.put(uniform.getName(), new UniformSlot(uniform.getInfo().type, uniform.getSize(), mapped));
        }

        List<ImageDescriptor> convertedSamplers = converter.getSamplerList();
        List<ImageDescriptor> mappedSamplers = new ArrayList<>(convertedSamplers.size());
        List<String> samplerNames = new ArrayList<>(convertedSamplers.size());
        for (ImageDescriptor descriptor : convertedSamplers) {
            int unit = ShaderFallback.samplerUnit(program, descriptor.name, descriptor.imageIdx);
            mappedSamplers.add(new ImageDescriptor(
                    descriptor.getBinding(), descriptor.qualifier, descriptor.name, unit));
            samplerNames.add(descriptor.name);
        }

        Pipeline.Builder builder = new Pipeline.Builder(
                DefaultVertexFormat.POSITION_TEX, "smoothgl/pulse_program_" + program);
        builder.setUniforms(List.of(ubo), mappedSamplers);
        builder.compileShaders("smoothgl_pulse_program_" + program,
                converter.getVshConverted(), converter.getFshConverted());
        GraphicsPipeline pipeline = builder.createGraphicsPipeline();

        int samplerSignature = samplerSignature(program, samplerNames);
        return new Entry(program, sourceHash, pipeline, slots, samplerNames, samplerSignature);
    }

    private static void validateForVulkanModConverter(String vertex, String fragment) {
        if (vertex.contains("gl_Frag") || fragment.contains("gl_FragData")) {
            throw new IllegalArgumentException("legacy fragment output form is not safely convertible");
        }
        if (vertex.contains("layout(binding") || fragment.contains("layout(binding")) {
            throw new IllegalArgumentException("explicit descriptor bindings need a dedicated mapper");
        }
        if (vertex.matches("(?s).*uniform\\s+[^;]*\\[[^;]*;.*")
                || fragment.matches("(?s).*uniform\\s+[^;]*\\[[^;]*;.*")) {
            throw new IllegalArgumentException("uniform arrays are not supported by VulkanMod 0.5.4 GlslConverter");
        }
        if (UNSUPPORTED_UNIFORM.matcher(vertex).find() || UNSUPPORTED_UNIFORM.matcher(fragment).find()) {
            throw new IllegalArgumentException("shader uses a uniform type not supported by VulkanMod 0.5.4 GlslConverter");
        }
        if (vertex.matches("(?s).*uniform\\s+[^;=]+=.*") || fragment.matches("(?s).*uniform\\s+[^;=]+=.*")) {
            throw new IllegalArgumentException("uniform initializers are not supported by VulkanMod 0.5.4 GlslConverter");
        }

        int inputs = 0;
        Matcher matcher = VERTEX_IN.matcher(vertex);
        while (matcher.find()) inputs++;
        if (inputs > 2) {
            throw new IllegalArgumentException("shader needs " + inputs + " vertex inputs; native bridge currently provides POSITION+UV");
        }
    }

    private static String normalizeVertex(String source) {
        String out = source.replace("\r", "");
        out = out.replaceAll("(?m)^\\s*attribute\\s+", "in ");
        out = out.replaceAll("(?m)^\\s*varying\\s+", "out ");
        out = stripIoLayoutQualifiers(out);
        out = out.replace("texture2D(", "texture(");
        return out;
    }

    private static String normalizeFragment(String source) {
        String out = source.replace("\r", "");
        out = out.replaceAll("(?m)^\\s*varying\\s+", "in ");

        Matcher output = FRAGMENT_OUT.matcher(out);
        if (output.find()) {
            String name = output.group(1);
            if (!"fragColor".equals(name)) {
                out = out.replaceAll("\\b" + Pattern.quote(name) + "\\b", "fragColor");
            }
        }

        out = out.replace("gl_FragColor", "fragColor");
        out = stripIoLayoutQualifiers(out);
        out = out.replace("texture2D(", "texture(");
        return out;
    }

    private static String stripIoLayoutQualifiers(String source) {
        return source.replaceAll(
                "(?m)^\\s*layout\\s*\\([^)]*\\)\\s*(in|out)\\s+", "$1 ");
    }

    private static int sourceHash(ShaderFallback.ProgramSources sources) {
        int result = sources.vertexSource().hashCode();
        result = 31 * result + sources.fragmentSource().hashCode();
        return result == 0 ? 1 : result;
    }

    private static int samplerSignature(int program, List<String> names) {
        int hash = 1;
        int fallback = 0;
        for (String name : names) {
            hash = 31 * hash + ShaderFallback.samplerUnit(program, name, fallback++);
        }
        return hash;
    }

    public record Prepared(GraphicsPipeline pipeline) {}

    private static final class Entry {
        final int program;
        final int sourceHash;
        final GraphicsPipeline pipeline;
        final Map<String, UniformSlot> uniforms;
        final List<String> samplerNames;
        final int builtSamplerSignature;

        Entry(int program, int sourceHash, GraphicsPipeline pipeline,
              Map<String, UniformSlot> uniforms, List<String> samplerNames,
              int builtSamplerSignature) {
            this.program = program;
            this.sourceHash = sourceHash;
            this.pipeline = pipeline;
            this.uniforms = uniforms;
            this.samplerNames = List.copyOf(samplerNames);
            this.builtSamplerSignature = builtSamplerSignature;
        }

        int samplerSignature() {
            return builtSamplerSignature;
        }

        void updateUniforms() {
            for (Map.Entry<String, UniformSlot> uniformEntry : uniforms.entrySet()) {
                String name = uniformEntry.getKey();
                if ("smoothglDummy".equals(name)) continue;
                ShaderFallback.UniformValue value = ShaderFallback.uniformValue(program, name);
                if (value == null) continue;
                uniformEntry.getValue().write(value);
            }
        }

        boolean texturesReady() {
            int fallback = 0;
            for (String name : samplerNames) {
                int unit = ShaderFallback.samplerUnit(program, name, fallback++);
                try {
                    if (VTextureSelector.getImage(unit) == null) return false;
                } catch (Throwable error) {
                    return false;
                }
            }
            return true;
        }
    }

    private record UniformSlot(String type, int size, MappedBuffer mapped) {
        void write(ShaderFallback.UniformValue value) {
            if ("int".equals(type)) {
                int[] ints = value.ints();
                if (ints.length > 0) mapped.putInt(0, ints[0]);
                else if (value.floats().length > 0) mapped.putInt(0, (int) value.floats()[0]);
                return;
            }

            float[] floats = value.floats();
            int[] ints = value.ints();
            int count = Math.min(size, floats.length > 0 ? floats.length : ints.length);
            for (int i = 0; i < count; i++) {
                float v = floats.length > 0 ? floats[i] : ints[i];
                mapped.putFloat(i * Float.BYTES, v);
            }
        }
    }
}
