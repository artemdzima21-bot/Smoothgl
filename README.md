# SmoothGL Pulse Vulkan Bridge

Experimental compatibility layer for running Pulse Visuals with VulkanMod on Minecraft 1.21.4 Fabric.

## Target
- Minecraft 1.21.4
- Fabric Loader 0.16.10+
- Java 21
- VulkanMod 0.5.4 for Minecraft 1.21.4

## Render path
`Pulse Visuals -> PulseRenderApi -> VulkanDispatch -> VulkanMod -> Vulkan`

The bridge keeps Pulse-facing render commands separate from VulkanMod internals. This lets us add targeted adapters for OpenGL, framebuffer and shader calls without making Pulse create a second renderer.

See `VULKAN_BASE.md` for the pinned runtime/source references.
