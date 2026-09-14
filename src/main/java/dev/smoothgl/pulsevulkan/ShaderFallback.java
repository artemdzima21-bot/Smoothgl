package dev.smoothgl.pulsevulkan;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

    private static final AtomicInteger NEXT_SHADER = new AtomicInteger(100_000);
    private static final AtomicInteger NEXT_PROGRAM = new AtomicInteger(200_000);
    private static final AtomicInteger NEXT_LOCATION = new AtomicInteger(1);
    private static final Map<Integer, Integer> SHADER_TYPES = new ConcurrentHashMap<>();
    private static final Map<Integer, String> SHADER_SOURCES = new ConcurrentHashMap<>();
    private static final Map<Integer, Set<Integer>> PROGRAM_SHADERS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> LOCATIONS = new ConcurrentHashMap<>();
    private static final Map<Integer, String> ATTRIB_NAMES = new ConcurrentHashMap<>();
    private static final Map<Integer, String> UNIFORM_NAMES = new ConcurrentHashMap<>();
    private static final Map<Integer, Matrix4Value> MATRIX4_VALUES = new ConcurrentHashMap<>();
    private static volatile int currentProgram;

    private ShaderFallback() {}

    public static int createShader(int type) {
        int id = NEXT_SHADER.getAndIncrement();
        SHADER_TYPES.put(id, type);
        PulseDiagnostics.fallback("GL20 shader objects are emulated; custom GLSL effects may be disabled");
        return id;
    }

    public static void shaderSource(int shader, CharSequence source) {
        SHADER_SOURCES.put(shader, source == null ? "" : source.toString());
    }

    public static void compileShader(int shader) { PulseDiagnostics.fallback("glCompileShader(" + shader + ")"); }

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

    public static void linkProgram(int program) { PulseDiagnostics.fallback("glLinkProgram(" + program + ")"); }
    public static void validateProgram(int program) { PulseDiagnostics.fallback("glValidateProgram(" + program + ")"); }

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

    public static void useProgram(int program) {
        currentProgram = program;
        if (program != 0) PulseDiagnostics.fallback("glUseProgram: using Vulkan-safe fallback pipeline");
    }

    public static int currentProgram() { return currentProgram; }

    public static void deleteShader(int shader) {
        SHADER_TYPES.remove(shader);
        SHADER_SOURCES.remove(shader);
        PROGRAM_SHADERS.values().forEach(set -> set.remove(shader));
    }

    public static void deleteProgram(int program) {
        PROGRAM_SHADERS.remove(program);
        if (currentProgram == program) currentProgram = 0;
    }

    public static int uniformLocation(int program, CharSequence name) {
        String text = String.valueOf(name);
        int location = LOCATIONS.computeIfAbsent("u:" + program + ":" + text, ignored -> NEXT_LOCATION.getAndIncrement());
        UNIFORM_NAMES.put(location, text);
        return location;
    }

    public static int attribLocation(int program, CharSequence name) {
        String text = String.valueOf(name);
        int location = LOCATIONS.computeIfAbsent("a:" + program + ":" + text, ignored -> NEXT_LOCATION.getAndIncrement());
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
        if (location >= 0 && Boolean.getBoolean("smoothgl.pulse.trace")) {
            PulseDiagnostics.fallback("uniform update @" + location + " = " + value);
        }
    }

    public static void uniformMatrix4(int location, boolean transpose, FloatBuffer value) {
        if (location < 0 || value == null || value.remaining() < 16) return;
        FloatBuffer copy = value.duplicate();
        float[] matrix = new float[16];
        copy.get(matrix);
        MATRIX4_VALUES.put(location, new Matrix4Value(location, uniformName(location), transpose, matrix));
        if (Boolean.getBoolean("smoothgl.pulse.trace")) {
            PulseDiagnostics.fallback("mat4 uniform @" + location + " (" + uniformName(location) + "), transpose=" + transpose);
        }
    }

    public static void uniformMatrix4(int location, boolean transpose, float[] value) {
        if (location < 0 || value == null || value.length < 16) return;
        float[] matrix = new float[16];
        System.arraycopy(value, 0, matrix, 0, 16);
        MATRIX4_VALUES.put(location, new Matrix4Value(location, uniformName(location), transpose, matrix));
    }

    public static List<Matrix4Value> currentMatrices() {
        List<Matrix4Value> out = new ArrayList<>(MATRIX4_VALUES.values());
        out.sort((a, b) -> Integer.compare(a.location(), b.location()));
        return out;
    }

    public static Matrix4Value bestTransformMatrix() {
        Matrix4Value best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Matrix4Value matrix : MATRIX4_VALUES.values()) {
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
        return bestScore > 0 ? best : (MATRIX4_VALUES.size() == 1 ? MATRIX4_VALUES.values().iterator().next() : null);
    }

    public static void unsupportedDraw(String operation) {
        PulseDiagnostics.fallback(operation + " skipped because raw OpenGL draw calls cannot be mapped safely to VulkanMod 0.5.4");
    }

    public record Matrix4Value(int location, String name, boolean transpose, float[] values) {}
}
