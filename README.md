# SmoothGL Pulse Vulkan Bridge

Fabric 1.21.4 compatibility layer for running Pulse Visuals together with VulkanMod 0.5.4.

## Render path

`Pulse / OpenGL-style calls -> compatibility mixins -> bridge APIs -> VulkanMod -> Vulkan`

The bridge does not start a second OpenGL renderer. State, texture, framebuffer and supported rendering commands are redirected to VulkanMod.

## Covered compatibility

- GL11 state: blend, depth test, culling, depth func and color mask.
- GL13 active texture units.
- GL14 separate blending.
- VulkanMod GL15 buffer compatibility remains in use.
- GL20 shader/program startup fallback, uniforms and vertex attribute calls.
- GL30 framebuffer/renderbuffer compatibility and VAO emulation.
- VulkanMod texture, framebuffer and renderbuffer backends.
- Diagnostics for features that cannot be translated safely.

## Shader fallback

VulkanMod 0.5.4 does not provide a general raw OpenGL GL20-to-Vulkan shader translator. The bridge therefore emulates GL20 program creation/linking so Pulse can continue loading. Raw OpenGL draw calls that depend on those custom programs are skipped rather than sent to a nonexistent OpenGL context. Minecraft/Fabric/VulkanMod rendering continues normally.

This means Pulse features implemented entirely as custom raw OpenGL post-processing may be disabled until a native Vulkan implementation is added, while HUD and features using Minecraft/Fabric rendering can continue to work.

## Detection

Compatibility mixins only activate when both VulkanMod and a Pulse Visuals-like mod id/name are detected. To force activation for a closed Pulse build with a different mod id, add this JVM argument:

`-Dsmoothgl.pulse.force=true`

For detailed fallback logging:

`-Dsmoothgl.pulse.trace=true`

## Target

- Minecraft 1.21.4
- Fabric Loader 0.16.10+
- Java 21
- VulkanMod 0.5.4 for Minecraft 1.21.4

See `VULKAN_BASE.md` for the pinned upstream/runtime references.
