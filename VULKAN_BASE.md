# Vulkan base

## Runtime target
- Minecraft: 1.21.4
- Loader: Fabric
- Java: 21
- VulkanMod: 0.5.4
- Official Modrinth version ID: 9BnBJI0w
- Artifact: VulkanMod_1.21.4-0.5.4.jar

## Historical source anchor
The bridge design was originally based on upstream VulkanMod commit:
`d5cf53022d5ecda013253dfedd9240786f61d5c6` (2025-07-05).

That commit targets Minecraft 1.21.1 and is kept only as a source-reference point.
The actual compile/runtime dependency of this branch is the official VulkanMod 0.5.4 build for Minecraft 1.21.4.

Bridge flow:
Pulse Visuals -> PulseRenderApi -> VulkanDispatch -> VulkanMod -> Vulkan.
