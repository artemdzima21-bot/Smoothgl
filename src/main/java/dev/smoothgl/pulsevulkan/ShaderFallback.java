package dev.smoothgl.pulsevulkan;

import java.nio.FloatBuffer;
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

    public static void compileShader(int shader) {
        PulseDiagnostics.fallback("glCompileShader(" + shader + ")");
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
        return LOCATIONS.computeIfAbsent("u:" + program + ":" + name, ignored -> NEXT_LOCATION.getAndIncrement());
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

    public static String attribName(int index) {
        return ATTRIB_NAMES.getOrDefault(index, "?");
    }

    public static void uniform(int location, Object value) {
        if (location >= 0 && Boolean.getBoolean("smoothgl.pulse.trace")) {
            PulseDiagnostics.fallback("uniform update @" + location + " = " + value);
        }
    }

    public static void uniformMatrix4(int location, boolean transpose, FloatBuffer value) {
        if (location >= 0 && Boolean.getBoolean("smoothgl.pulse.trace")) {
            PulseDiagnostics.fallback("mat4 uniform @" + location + ", transpose=" + transpose);
        }
    }

    public static void unsupportedDraw(String operation) {
        PulseDiagnostics.fallback(operation + " skipped because raw OpenGL draw calls cannot be mapped safely to VulkanMod 0.5.4");
    }
}
