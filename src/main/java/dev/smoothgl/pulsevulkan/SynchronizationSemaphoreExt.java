package dev.smoothgl.pulsevulkan;

import net.vulkanmod.vulkan.queue.CommandPool;
import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;

/**
 * Backport surface for VulkanMod's newer in-frame semaphore synchronization.
 * Implemented onto VulkanMod 0.5.4 Synchronization by a mixin so the bundled
 * 1.21.4 renderer can use the 0.5.7-dev synchronization strategy without
 * replacing VulkanMod with a Minecraft-1.21.10 build.
 */
public interface SynchronizationSemaphoreExt {
    void smoothgl$addCommandBuffer(CommandPool.CommandBuffer commandBuffer, boolean useSemaphore);
    void smoothgl$addWaitSemaphore(long semaphore);
    LongBuffer smoothgl$getWaitSemaphores(MemoryStack stack);
    void smoothgl$scheduleCbReset();
}
