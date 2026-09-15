package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.SynchronizationSemaphoreExt;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.queue.CommandPool;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.nio.LongBuffer;

/** Backports VulkanMod 0.5.7-dev's semaphore list onto the bundled 0.5.4 Synchronization. */
@Mixin(value = Synchronization.class, remap = false)
public abstract class SynchronizationSemaphoreMixin implements SynchronizationSemaphoreExt {
    @Unique
    private final LongArrayList smoothgl$semaphores = new LongArrayList();
    @Unique
    private final ObjectArrayList<CommandPool.CommandBuffer> smoothgl$semaphoreCbs = new ObjectArrayList<>();

    @Override
    public synchronized void smoothgl$addCommandBuffer(CommandPool.CommandBuffer commandBuffer, boolean useSemaphore) {
        if (!useSemaphore) {
            ((Synchronization) (Object) this).addCommandBuffer(commandBuffer);
            return;
        }
        // CommandBuffer already owns a semaphore in VulkanMod 0.5.4; newer VulkanMod merely exposes it.
        this.smoothgl$semaphores.add(commandBuffer.semaphore);
        this.smoothgl$semaphoreCbs.add(commandBuffer);
    }

    @Override
    public synchronized void smoothgl$addWaitSemaphore(long semaphore) {
        this.smoothgl$semaphores.add(semaphore);
    }

    @Override
    public synchronized LongBuffer smoothgl$getWaitSemaphores(MemoryStack stack) {
        LongBuffer buffer = stack.mallocLong(this.smoothgl$semaphores.size());
        if (!this.smoothgl$semaphores.isEmpty()) {
            buffer.put(this.smoothgl$semaphores.elements(), 0, this.smoothgl$semaphores.size());
        }
        buffer.flip();
        this.smoothgl$semaphores.clear();
        return buffer;
    }

    @Override
    public synchronized void smoothgl$scheduleCbReset() {
        if (this.smoothgl$semaphoreCbs.isEmpty()) return;
        final ObjectArrayList<CommandPool.CommandBuffer> frameSemaphoreCbs = this.smoothgl$semaphoreCbs.clone();
        MemoryManager.getInstance().addFrameOp(() -> frameSemaphoreCbs.forEach(CommandPool.CommandBuffer::reset));
        this.smoothgl$semaphoreCbs.clear();
    }
}
