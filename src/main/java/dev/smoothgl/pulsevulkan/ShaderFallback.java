package dev.smoothgl.pulsevulkan;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OpenGL object/state emulation used only for Pulse Visuals calls.
 *
 * Besides keeping Pulse away from a missing OpenGL context, this class now keeps
 * enough linked-program state for the Vulkan bridge to build a real VulkanMod
 * pipeline from the GLSL that Pulse itself submitted at runtime.
 */
public final class ShaderFallback {
    private static final int GL_DELETE_STATUS = 0x8B80;
    private static final int GL_COMPILE_STATUS = 0x8B81;
    private static final int GL_LINK_STATUS = 0x8B82;
    private static final int GL_VALIDATE_STATUS = 0x8B83;
    private static final int GL_INFO_LOG_LENGTH = 0x8B84;
    private static final int GL_ATTACHED_SHADERS = 0x8B85;
    private static final int GL_ACTIVE_UNIFORMS = 0x8B86;
    private static final int GL_ACTIVE_ATTRIBUTES = 0x8B89;
    private static final int GL_SHADER_TYPE = 0x8B4F;
    private static final int GL_FRAGMENT_SHADER = 0x8B30;
    private static final int GL_VERTEX_SHADER = 0x8B31;

    private static final AtomicInteger NEXT_SHADER = new AtomicInteger(100_000);
    private static final AtomicInteger NEXT_PROGRAM = new AtomicInteger(200_000);
    private static final AtomicInteger NEXT_LOCATION = new AtomicInteger(1);

    private static final Map<Integer, Integer> SHADER_TYPES = new ConcurrentHashMap<>();
    private static final Map<Integer, String> SHADER_SOURCES = new ConcurrentHashMap<>();
    private static final Map<Integer, Set<Integer>> PROGRAM_SHADERS = new ConcurrentHashMap<>();
    private static final Map<Integer, ProgramSources> LINKED_PROGRAMS = new ConcurrentHashMap<>();

    private static final Map<String, Integer> LOCATIONS = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> LOCATION_PROGRAMS = new ConcurrentHashMap<>();
    private static final Map<Integer, String> ATTRIB_NAMES = new ConcurrentHashMap<>();
    private static final Map<Integer, String> UNIFORM_NAMES = new ConcurrentHashMap<>();
    private static final Map<Integer, UniformValue> UNIFORM_VALUES = new ConcurrentHashMap<>();
    private static final Map<Integer, Matrix4Value> MATRIX4_VALUES = new ConcurrentHashMap<>();

    private static volatile int currentProgram;

    private ShaderFallback() {}

    public static int createShader(int type) {
        int id = NEXT_SHADER.getAndIncrement();
        SHADER_TYPES.put(id, type);
        PulseDiagnostics.fallback("GL20 shader objects are emulated; custom GLSL is captured for Vulkan conversion");
        return id;
    }

    public static void shaderSource(int shader, CharSequence source) {
        SHADER_SOURCES.put(shader, source == null ? "" : source.toString());
    }

    public static void compileShader(int shader) {
        PulseDiagnostics.fallback("glCompileShader(" + shader + ") captured for Vulkan conversion");
    }

    public static int getShaderInt(int shader, int pname) {
        return switch (pname) {
            case GL_COMPILE_STATUS -> 1;
            case GL_DELETE_STATUS -> 0;
            case GL_INFO_LOG_LENGTH -> 0;
            case GL_SHADER_TYPE -> SHADER_TYPES.getOrDefault(shader, 0);
            default -> 0;
        };
    }

    public static String shaderInfoLog(int shader) { return ""; }

    public static int createProgram() {
        int id = NEXT_PROGRAM.getAndIncrement();
        PROGRAM_SHADERS.put(id, ConcurrentHashMap.newKeySet());
        return id;
    }

    public static void attachShader(int program, int shader) {
        PROGRAM_SHADERS.computeIfAbsent(program, ignored -> ConcurrentHashMap.newKeySet()).add(shader);
    }

    public static void detachShader(int program, int shader) {
        Set<Integer> shaders = PROGRAM_SHADERS.get(program);
        if (shaders != null) shaders.remove(shader);
    }

    /** Snapshot shader sources at link time, matching OpenGL's linked executable lifetime. */
    public static void linkProgram(int program) {
        String vertex = null;
        String fragment = null;
        for (int shader : PROGRAM_SHADERS.getOrDefault(program, Set.of())) {
            int type = SHADER_TYPES.getOrDefault(shader, 0);
            String source = SHADER_SOURCES.get(shader);
            if (source == null) continue;
            if (type == GL_VERTEX_SHADER) vertex = source;
            else if (type == GL_FRAGMENT_SHADER) fragment = source;
        }

        if (vertex != null && fragment != null) {
            LINKED_PROGRAMS.put(program, new ProgramSources(program, vertex, fragment));
            PulseProgramPipeline.invalidate(program);
        } else {
            PulseDiagnostics.infoOnce("linked-program-missing-stage-" + program,
                    "Pulse program " + program + " linked without a captured vertex/fragment pair");
        }
        PulseDiagnostics.fallback("glLinkProgram(" + program + ") captured for Vulkan conversion");
    }

    public static void validateProgram(int program) {
        PulseDiagnostics.fallback("glValidateProgram(" + program + ")");
    }

    public static int getProgramInt(int program, int pname) {
        return switch (pname) {
            case GL_LINK_STATUS, GL_VALIDATE_STATUS -> 1;
            case GL_DELETE_STATUS -> 0;
            case GL_INFO_LOG_LENGTH -> 0;
            case GL_ATTACHED_SHADERS -> PROGRAM_SHADERS.getOrDefault(program, Set.of()).size();
            case GL_ACTIVE_UNIFORMS, GL_ACTIVE_ATTRIBUTES -> 0;
            default -> 0;
        };
    }

    public static String programInfoLog(int program) { return ""; }

    public static boolean isSyntheticProgram(int program) {
        return program >= 200_000 && PROGRAM_SHADERS.containsKey(program);
    }

    public static void useProgram(int program) {
        currentProgram = program;
        PulseCallScope.setProgramActive(program != 0 && isSyntheticProgram(program));
        if (program != 0) {
            PulseDiagnostics.infoOnce("gl-use-program-fallback",
                    "glUseProgram is routed through the Vulkan-safe Pulse program state");
        }
    }

    public static int currentProgram() { return currentProgram; }

    public static ProgramSources programSources(int program) {
        return LINKED_PROGRAMS.get(program);
    }

    public static ProgramSources currentProgramSources() {
        return programSources(currentProgram);
    }

    public static void deleteShader(int shader) {
        /* OpenGL keeps a deleted shader alive while it is attached to a program.
         * LINKED_PROGRAMS already owns an immutable source snapshot, so removing
         * the standalone source here is safe and must not detach it from programs. */
        SHADER_TYPES.remove(shader);
        SHADER_SOURCES.remove(shader);
    }

    public static void deleteProgram(int program) {
        PROGRAM_SHADERS.remove(program);
        LINKED_PROGRAMS.remove(program);
        PulseProgramPipeline.invalidate(program);

        LOCATION_PROGRAMS.entrySet().removeIf(entry -> entry.getValue() == program);
        if (currentProgram == program) {
            currentProgram = 0;
            PulseCallScope.setProgramActive(false);
        }
    }

    public static int uniformLocation(int program, CharSequence name) {
        String text = String.valueOf(name);
        int location = LOCATIONS.computeIfAbsent("u:" + program + ":" + text,
                ignored -> NEXT_LOCATION.getAndIncrement());
        UNIFORM_NAMES.put(location, text);
        LOCATION_PROGRAMS.put(location, program);
        return location;
    }

    public static int attribLocation(int program, CharSequence name) {
        String text = String.valueOf(name);
        int location = LOCATIONS.computeIfAbsent("a:" + program + ":" + text,
                ignored -> NEXT_LOCATION.getAndIncrement());
        ATTRIB_NAMES.put(location, text);
        return location;
    }

    public static void bindAttribLocation(int program, int index, CharSequence name) {
        String text = String.valueOf(name);
        LOCATIONS.put("a:" + program + ":" + text, index);
        ATTRIB_NAMES.put(index, text);
    }

    public static String attribName(int index) { return ATTRIB_NAMES.getOrDefault(index, "?"); }
    public static String uniformName(int location) { return UNIFORM_NAMES.getOrDefault(location, "?"); }

    public static void uniform(int location, Object value) {
        if (value instanceof Integer integer) {
            uniformInts(location, integer);
        } else if (value instanceof Number number) {
            uniformFloats(location, number.floatValue());
        } else if (location >= 0 && Boolean.getBoolean("smoothgl.pulse.trace")) {
            PulseDiagnostics.fallback("untyped uniform update @" + location + " = " + value);
        }
    }

    public static void uniformFloats(int location, float... values) {
        if (location < 0 || values == null) return;
        UNIFORM_VALUES.put(location, UniformValue.floats(values));
        traceUniform(location, "float[" + values.length + "]");
    }

    public static void uniformInts(int location, int... values) {
        if (location < 0 || values == null) return;
        UNIFORM_VALUES.put(location, UniformValue.ints(values));
        traceUniform(location, "int[" + values.length + "]");
    }

    public static void uniformFloats(int location, FloatBuffer values) {
        if (location < 0 || values == null) return;
        FloatBuffer copy = values.duplicate();
        float[] out = new float[copy.remaining()];
        copy.get(out);
        uniformFloats(location, out);
    }

    public static void uniformInts(int location, IntBuffer values) {
        if (location < 0 || values == null) return;
        IntBuffer copy = values.duplicate();
        int[] out = new int[copy.remaining()];
        copy.get(out);
        uniformInts(location, out);
    }

    public static void uniformMatrix(int location, int dimension, boolean transpose, FloatBuffer value) {
        if (location < 0 || value == null) return;
        int count = dimension * dimension;
        if (value.remaining() < count) return;
        FloatBuffer copy = value.duplicate();
        float[] matrix = new float[count];
        copy.get(matrix);
        uniformMatrix(location, dimension, transpose, matrix);
    }

    public static void uniformMatrix(int location, int dimension, boolean transpose, float[] value) {
        if (location < 0 || value == null) return;
        int count = dimension * dimension;
        if (value.length < count) return;
        float[] matrix = Arrays.copyOf(value, count);
        float[] stored = transpose ? transpose(matrix, dimension) : matrix;
        UNIFORM_VALUES.put(location, UniformValue.floats(stored));
        traceUniform(location, "mat" + dimension + " transpose=" + transpose);
    }

    public static void uniformMatrix4(int location, boolean transpose, FloatBuffer value) {
        if (location < 0 || value == null || value.remaining() < 16) return;
        FloatBuffer copy = value.duplicate();
        float[] matrix = new float[16];
        copy.get(matrix);
        uniformMatrix4(location, transpose, matrix);
    }

    public static void uniformMatrix4(int location, boolean transpose, float[] value) {
        if (location < 0 || value == null || value.length < 16) return;
        float[] matrix = Arrays.copyOf(value, 16);
        MATRIX4_VALUES.put(location,
                new Matrix4Value(location, uniformName(location), transpose, matrix));
        uniformMatrix(location, 4, transpose, matrix);
    }

    public static UniformValue uniformValue(int program, String name) {
        Integer location = LOCATIONS.get("u:" + program + ":" + name);
        return location == null ? null : UNIFORM_VALUES.get(location);
    }

    public static int samplerUnit(int program, String name, int fallback) {
        UniformValue value = uniformValue(program, name);
        if (value == null || value.ints().length == 0) return fallback;
        return Math.max(0, value.ints()[0]);
    }

    public static List<Matrix4Value> currentMatrices() {
        int program = currentProgram;
        List<Matrix4Value> out = new ArrayList<>();
        for (Map.Entry<Integer, Matrix4Value> entry : MATRIX4_VALUES.entrySet()) {
            if (LOCATION_PROGRAMS.getOrDefault(entry.getKey(), -1) == program) out.add(entry.getValue());
        }
        out.sort((a, b) -> Integer.compare(a.location(), b.location()));
        return out;
    }

    public static Matrix4Value bestTransformMatrix() {
        Matrix4Value best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Matrix4Value matrix : currentMatrices()) {
            String name = matrix.name().toLowerCase(Locale.ROOT);
            int score = 0;
            if (name.contains("mvp")) score += 100;
            if (name.contains("projection")) score += 70;
            if (name.contains("proj")) score += 50;
            if (name.contains("transform")) score += 40;
            if (name.contains("matrix")) score += 20;
            if (name.contains("model")) score += 10;
            if (score > bestScore) {
                bestScore = score;
                best = matrix;
            }
        }
        List<Matrix4Value> matrices = currentMatrices();
        return bestScore > 0 ? best : (matrices.size() == 1 ? matrices.get(0) : null);
    }

    public static float[] bestColor(int program) {
        float[] best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Map.Entry<Integer, UniformValue> entry : UNIFORM_VALUES.entrySet()) {
            int location = entry.getKey();
            if (LOCATION_PROGRAMS.getOrDefault(location, -1) != program) continue;
            UniformValue value = entry.getValue();
            if (value.floats().length < 3) continue;

            String name = uniformName(location).toLowerCase(Locale.ROOT);
            int score = 0;
            if (name.equals("color") || name.equals("ucolor")) score += 120;
            if (name.contains("colormodulator")) score += 110;
            if (name.contains("tint")) score += 100;
            if (name.contains("color") || name.contains("colour")) score += 70;
            if (name.contains("rgba")) score += 60;
            if (name.contains("alpha")) score -= 20;
            if (score > bestScore) {
                float[] v = value.floats();
                best = new float[]{v[0], v[1], v[2], v.length >= 4 ? v[3] : 1.0f};
                bestScore = score;
            }
        }
        return bestScore > 0 ? best : new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    }

    public static void unsupportedDraw(String operation) {
        PulseDiagnostics.fallback(operation + " skipped because raw OpenGL draw calls cannot be mapped safely to VulkanMod 0.5.4");
    }

    private static void traceUniform(int location, String value) {
        if (Boolean.getBoolean("smoothgl.pulse.trace")) {
            PulseDiagnostics.fallback("uniform update @" + location + " (" + uniformName(location) + ") = " + value);
        }
    }

    private static float[] transpose(float[] source, int dimension) {
        float[] out = new float[source.length];
        for (int row = 0; row < dimension; row++) {
            for (int col = 0; col < dimension; col++) {
                out[row * dimension + col] = source[col * dimension + row];
            }
        }
        return out;
    }

    public record ProgramSources(int program, String vertexSource, String fragmentSource) {
        public boolean complete() {
            return vertexSource != null && !vertexSource.isBlank()
                    && fragmentSource != null && !fragmentSource.isBlank();
        }
    }

    public record UniformValue(float[] floats, int[] ints) {
        private static UniformValue floats(float[] values) {
            return new UniformValue(Arrays.copyOf(values, values.length), new int[0]);
        }

        private static UniformValue ints(int[] values) {
            return new UniformValue(new float[0], Arrays.copyOf(values, values.length));
        }
    }

    public record Matrix4Value(int location, String name, boolean transpose, float[] values) {}
}
