package dev.smoothgl.pulsevulkan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small source-to-source compatibility pass for GLSL captured from Pulse.
 *
 * VulkanMod 0.5.4's GlslConverter intentionally accepts only a narrow subset
 * of legacy GLSL. This pass removes syntax that is semantically irrelevant for
 * our Vulkan translation but otherwise confuses its token parser. It does not
 * dump or persist proprietary shader sources.
 */
public final class PulseGlslSanitizer {
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("(?m)//.*$");
    private static final Pattern PRECISION_STATEMENT = Pattern.compile(
            "(?m)^\\s*precision\\s+(?:lowp|mediump|highp)\\s+[A-Za-z_][A-Za-z0-9_]*\\s*;\\s*$");
    private static final Pattern PRECISION_QUALIFIER = Pattern.compile("\\b(?:lowp|mediump|highp)\\s+");
    private static final Pattern INTERPOLATION_QUALIFIER = Pattern.compile(
            "\\b(?:flat|smooth|noperspective|centroid)\\s+(?=(?:in|out)\\b)");
    private static final Pattern UNIFORM_INITIALIZER = Pattern.compile(
            "(?m)(\\buniform\\s+[A-Za-z_][A-Za-z0-9_]*\\s+[A-Za-z_][A-Za-z0-9_]*\\s*)=\\s*([^;]+);" );
    private static final Pattern ARRAY_SUFFIX = Pattern.compile(
            "(?m)(\\b(?:uniform|in|out)\\s+[A-Za-z_][A-Za-z0-9_]*\\s+[A-Za-z_][A-Za-z0-9_]*)\\s+;" );

    private PulseGlslSanitizer() {}

    public static String sanitize(String source) {
        if (source == null || source.isEmpty()) return source;

        String out = source.replace("\r", "").replace("\uFEFF", "");

        // VulkanMod's tokenizer keeps consuming tokens after a declaration.
        // A trailing comment after `uniform sampler2D x;` therefore used to be
        // parsed as another type/name pair and failed with "last char is not ;".
        out = BLOCK_COMMENT.matcher(out).replaceAll(" ");
        out = LINE_COMMENT.matcher(out).replaceAll("");

        // ES-style precision is meaningless in desktop/Vulkan GLSL and the old
        // converter mistakes it for a declaration type/name.
        out = PRECISION_STATEMENT.matcher(out).replaceAll("");
        out = PRECISION_QUALIFIER.matcher(out).replaceAll("");
        out = INTERPOLATION_QUALIFIER.matcher(out).replaceAll("");

        // Uniform initializers are legal in desktop GLSL but VulkanMod moves
        // uniforms into a UBO where member initializers are invalid. Pulse sets
        // its live values through glUniform*, which our bridge captures, so the
        // declaration initializer must not be copied into the UBO.
        Matcher initializer = UNIFORM_INITIALIZER.matcher(out);
        StringBuffer rebuilt = new StringBuffer(out.length());
        boolean strippedInitializer = false;
        while (initializer.find()) {
            strippedInitializer = true;
            initializer.appendReplacement(rebuilt, Matcher.quoteReplacement(initializer.group(1) + ";"));
        }
        initializer.appendTail(rebuilt);
        out = rebuilt.toString();
        if (strippedInitializer) {
            PulseDiagnostics.infoOnce("glsl-uniform-initializer-normalized",
                    "Pulse GLSL uniform initializers are normalized for Vulkan UBOs");
        }

        // Common legacy sampling aliases accepted by OpenGL but not by a 450
        // Vulkan shader after conversion.
        out = out.replace("texture2D(", "texture(")
                 .replace("textureCube(", "texture(");

        // Clean odd whitespace before semicolons in declarations. This also
        // makes the old removeSemicolon() parser deterministic.
        out = ARRAY_SUFFIX.matcher(out).replaceAll("$1;");
        return out;
    }
}
