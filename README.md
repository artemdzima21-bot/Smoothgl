# SmoothGL Pulse Vulkan Full

Minecraft 1.21.4 Fabric build that packages the Pulse Visuals compatibility layer and the complete official VulkanMod 0.5.4 runtime into one installable JAR.

## One-JAR layout

`Pulse Visuals -> SmoothGL compatibility mixins -> bundled VulkanMod 0.5.4 -> Vulkan -> GPU`

The build uses Fabric Loom jar-in-jar packaging. The final SmoothGL JAR physically contains the official VulkanMod JAR under `META-INF/jars/`, so a separate VulkanMod JAR is not required in the `mods` directory.

Do not install a second external copy of VulkanMod 0.5.4 next to this build; the bundled copy already provides the `vulkanmod` mod id.

## Max-FPS tuning

The bundled runtime raises VulkanMod's frame queue from the stock value of 2 to 4 for higher throughput at very high uncapped FPS. The selected Vulkan GPU is printed to the log as:

`[SmoothGL/Vulkan] Selected GPU: ...`

Override the queue without rebuilding:

`-Dsmoothgl.vulkan.frameQueue=2`
`-Dsmoothgl.vulkan.frameQueue=3`
`-Dsmoothgl.vulkan.frameQueue=4`
`-Dsmoothgl.vulkan.frameQueue=5`

Disable automatic max-FPS tuning entirely with:

`-Dsmoothgl.vulkan.disableMaxFpsTuning=true`

Higher queue values can improve maximum throughput but may increase input latency slightly, so 4 is used as the default performance-oriented compromise.

## Covered compatibility

- GL11 state: blend, depth test, culling, depth func and color mask.
- GL13 active texture units.
- GL14 separate blending.
- VulkanMod GL15 buffer compatibility.
- GL20 shader/program startup fallback, uniforms and vertex attribute calls.
- GL30 framebuffer/renderbuffer compatibility and VAO emulation.
- VulkanMod texture, framebuffer and renderbuffer backends.
- Persistent diagnostics for unsupported Pulse rendering operations.

## Missing-render diagnostics

When Pulse reaches an operation that still cannot be mapped safely, the bridge records it instead of silently losing the information. The diagnostic report is written to:

`logs/pulse-vulkan-missing.log`

It records the operation, hit count, first caller/stack information and a hint about the Vulkan-side adapter that still needs to be implemented. Detailed console tracing can additionally be enabled with:

`-Dsmoothgl.pulse.trace=true`

For a closed Pulse build with a different mod id, force the compatibility layer with:

`-Dsmoothgl.pulse.force=true`

## Shader fallback

VulkanMod 0.5.4 does not provide a universal translator for arbitrary third-party raw GLSL. Program creation and setup calls are emulated so Pulse can continue loading. A custom effect that performs unsupported direct OpenGL drawing is skipped and written to the diagnostic report until that effect gets a native Vulkan path.

## Target

- Minecraft 1.21.4
- Fabric Loader 0.16.10+
- Fabric API 0.114.0+1.21.4
- Java 21
- Bundled VulkanMod 0.5.4 for Minecraft 1.21.4

## Upstream VulkanMod

The bundled renderer is the official VulkanMod 0.5.4 release/tag from `xCollateral/VulkanMod`. VulkanMod remains under its upstream LGPL-3.0 license. See `VULKANMOD-NOTICE.md` and `VULKAN_BASE.md` for the pinned upstream reference.
