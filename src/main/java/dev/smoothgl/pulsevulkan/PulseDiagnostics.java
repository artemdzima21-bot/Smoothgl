package dev.smoothgl.pulsevulkan;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class PulseDiagnostics {
    private static final Set<String> ONCE = ConcurrentHashMap.newKeySet();
    private static final AtomicLong FALLBACK_CALLS = new AtomicLong();
    private static final Map<String, MissingEntry> MISSING = new ConcurrentHashMap<>();
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();
    private static final Object FILE_LOCK = new Object();
    private static final boolean TRACE = Boolean.getBoolean("smoothgl.pulse.trace");
    private static final StackWalker STACK_WALKER = StackWalker.getInstance();

    private static volatile Path reportPath;

    private PulseDiagnostics() {}

    public static void init() {
        if (!INITIALIZED.compareAndSet(false, true)) return;

        try {
            Path logDir = FabricLoader.getInstance().getGameDir().resolve("logs");
            Files.createDirectories(logDir);
            reportPath = logDir.resolve("pulse-vulkan-missing.log");
            writeSummary();
            infoOnce("report-path", "Compatibility report: " + reportPath.toAbsolutePath());
        } catch (Throwable t) {
            System.err.println("[PulseVulkanBridge] Could not initialize compatibility report: " + t);
        }

        try {
            Runtime.getRuntime().addShutdownHook(new Thread(PulseDiagnostics::writeSummary, "pulse-vulkan-report"));
        } catch (Throwable ignored) {
            // A shutdown hook may be unavailable in restricted launchers. First-seen entries are still written immediately.
        }
    }

    public static void infoOnce(String key, String message) {
        if (ONCE.add("info:" + key)) {
            System.out.println("[PulseVulkanBridge] " + message);
        }
    }

    public static void fallback(String operation) {
        record(operation, severityFor(operation));
    }

    public static void unsupported(String operation) {
        record(operation, Severity.BLOCKED);
    }

    public static long fallbackCalls() {
        return FALLBACK_CALLS.get();
    }

    public static Path reportPath() {
        return reportPath;
    }

    public static void flushReport() {
        writeSummary();
    }

    private static void record(String operation, Severity severity) {
        init();
        long total = FALLBACK_CALLS.incrementAndGet();
        String safeOperation = operation == null || operation.isBlank() ? "unknown operation" : operation.trim();
        String key = normalize(safeOperation);

        MissingEntry entry = MISSING.get(key);
        boolean first = false;
        if (entry == null) {
            MissingEntry candidate = captureEntry(key, safeOperation, severity);
            MissingEntry previous = MISSING.putIfAbsent(key, candidate);
            entry = previous == null ? candidate : previous;
            first = previous == null;
        }

        long count = entry.count.incrementAndGet();
        entry.lastSeen = Instant.now();
        if (severity.priority > entry.severity.priority) entry.severity = severity;

        if (first) {
            System.out.println("[PulseVulkanBridge] Missing Vulkan compatibility: " + safeOperation);
            if (!entry.caller.equals("unknown")) {
                System.out.println("[PulseVulkanBridge]   first caller: " + entry.caller);
            }
            System.out.println("[PulseVulkanBridge]   fix: " + entry.suggestion);
            writeSummary();
        } else if (TRACE) {
            System.out.println("[PulseVulkanBridge] fallback #" + count + ": " + safeOperation);
        } else if ((total & 63L) == 0L) {
            // Periodically persist counters without doing disk I/O on every render call.
            writeSummary();
        }
    }

    private static MissingEntry captureEntry(String key, String operation, Severity severity) {
        List<String> stack = STACK_WALKER.walk(stream -> stream
                .filter(frame -> isExternal(frame.getClassName()))
                .limit(12)
                .map(frame -> frame.getClassName() + "#" + frame.getMethodName() + ":" + frame.getLineNumber())
                .toList());

        String caller = stack.isEmpty() ? "unknown" : stack.getFirst();
        return new MissingEntry(key, operation, severity, caller, stack, suggestionFor(operation));
    }

    private static boolean isExternal(String className) {
        return !className.startsWith("dev.smoothgl.pulsevulkan.")
                && !className.startsWith("org.lwjgl.")
                && !className.startsWith("org.spongepowered.")
                && !className.startsWith("net.fabricmc.")
                && !className.startsWith("java.")
                && !className.startsWith("jdk.")
                && !className.startsWith("sun.");
    }

    private static String normalize(String operation) {
        return operation
                .replaceAll("0x[0-9a-fA-F]+", "0x#")
                .replaceAll("\\b-?\\d+\\b", "#")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static Severity severityFor(String operation) {
        String op = operation.toLowerCase(Locale.ROOT);
        if (op.contains("skipped") || op.contains("cannot be mapped") || op.contains("raw opengl draw")) {
            return Severity.BLOCKED;
        }
        if (op.contains("ignored") || op.contains("no-op") || op.contains("disabled")) {
            return Severity.DEGRADED;
        }
        return Severity.EMULATED;
    }

    private static String suggestionFor(String operation) {
        String op = operation.toLowerCase(Locale.ROOT);
        if (op.contains("gldrawarrays") || op.contains("gldrawelements") || op.contains("draw call")) {
            return "Implement a Vulkan draw path: capture Pulse vertex/index data, vertex layout, topology and the active shader/pipeline.";
        }
        if (op.contains("shader") || op.contains("glcompile") || op.contains("gllinkprogram") || op.contains("gluseprogram") || op.contains("glsl")) {
            return "Port this Pulse shader path to a VulkanMod pipeline/SPIR-V-compatible path and map its uniforms/textures.";
        }
        if (op.contains("uniform") || op.contains("attrib")) {
            return "Map the Pulse uniform/vertex attribute state into Vulkan descriptors, push constants or vertex input state.";
        }
        if (op.contains("framebuffer") || op.contains("renderbuffer") || op.contains("readbuffer") || op.contains("readpixels")) {
            return "Add a Vulkan framebuffer/readback adapter and map the requested attachment or transfer operation.";
        }
        if (op.contains("blend")) {
            return "Map this blend state to Vulkan pipeline blend configuration in VRenderSystem/PipelineState.";
        }
        if (op.contains("texture") || op.contains("mipmap") || op.contains("tex")) {
            return "Map this texture operation to VulkanMod GlTexture/VulkanImage and sampler state.";
        }
        if (op.contains("vao") || op.contains("vertex array")) {
            return "Extend VAO emulation so Pulse vertex bindings are translated into Vulkan vertex input state.";
        }
        return "Add a targeted OpenGL-to-Vulkan adapter/mixin for this call after checking its Pulse call site and required render state.";
    }

    private static void writeSummary() {
        Path path = reportPath;
        if (path == null) return;

        synchronized (FILE_LOCK) {
            try {
                List<MissingEntry> entries = new ArrayList<>(MISSING.values());
                entries.sort(Comparator
                        .comparingInt((MissingEntry e) -> e.severity.priority).reversed()
                        .thenComparingLong((MissingEntry e) -> e.count.get()).reversed()
                        .thenComparing(e -> e.operation));

                StringBuilder out = new StringBuilder(8192);
                out.append("SmoothGL Pulse Vulkan compatibility report\n");
                out.append("Generated: ").append(Instant.now()).append('\n');
                out.append("Total fallback/unsupported calls: ").append(FALLBACK_CALLS.get()).append('\n');
                out.append("Unique compatibility gaps: ").append(entries.size()).append('\n');
                out.append("\nSend this file after reproducing missing/broken Pulse effects.\n");
                out.append("Priority: BLOCKED = effect cannot render; DEGRADED = partial/no-op; EMULATED = fallback works but should be ported natively.\n\n");

                if (entries.isEmpty()) {
                    out.append("No compatibility gaps have been observed yet.\n");
                }

                int index = 1;
                for (MissingEntry entry : entries) {
                    out.append("============================================================\n");
                    out.append('#').append(index++).append(' ').append(entry.severity).append('\n');
                    out.append("Operation: ").append(entry.operation).append('\n');
                    out.append("Normalized: ").append(entry.key).append('\n');
                    out.append("Count: ").append(entry.count.get()).append('\n');
                    out.append("First seen: ").append(entry.firstSeen).append('\n');
                    out.append("Last seen: ").append(entry.lastSeen).append('\n');
                    out.append("First external caller: ").append(entry.caller).append('\n');
                    out.append("What to implement: ").append(entry.suggestion).append('\n');
                    out.append("First external stack:\n");
                    if (entry.stack.isEmpty()) {
                        out.append("  <no external frames captured>\n");
                    } else {
                        for (String frame : entry.stack) out.append("  at ").append(frame).append('\n');
                    }
                    out.append('\n');
                }

                Files.writeString(path, out.toString(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            } catch (IOException e) {
                if (ONCE.add("report-write-error")) {
                    System.err.println("[PulseVulkanBridge] Could not write compatibility report: " + e);
                }
            }
        }
    }

    private enum Severity {
        EMULATED(1),
        DEGRADED(2),
        BLOCKED(3);

        final int priority;

        Severity(int priority) {
            this.priority = priority;
        }
    }

    private static final class MissingEntry {
        final String key;
        final String operation;
        volatile Severity severity;
        final String caller;
        final List<String> stack;
        final String suggestion;
        final Instant firstSeen = Instant.now();
        volatile Instant lastSeen = firstSeen;
        final AtomicLong count = new AtomicLong();

        MissingEntry(String key, String operation, Severity severity, String caller, List<String> stack, String suggestion) {
            this.key = key;
            this.operation = operation;
            this.severity = severity;
            this.caller = caller;
            this.stack = List.copyOf(stack);
            this.suggestion = suggestion;
        }
    }
}
