package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.SynchronizationSemaphoreExt;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Synchronization;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;

/**
 * Backports the important part of VulkanMod commit 9be5fe8 (0.5.7-dev):
 * upload/transition command buffers signal semaphores and the main frame submit
 * waits on them instead of forcing a CPU fence wait before every frame.
 */
@Mixin(value = Renderer.class, remap = false)
public abstract class RendererSemaphoreSyncMixin {
    @Unique private static volatile boolean smoothgl$logged;

    @Unique
    private static boolean smoothgl$enabled() {
        return Boolean.parseBoolean(System.getProperty("smoothgl.vulkan.semaphoreSync", "true"));
    }

    @Unique
    private static SynchronizationSemaphoreExt smoothgl$sync() {
        return (SynchronizationSemaphoreExt) (Object) Synchronization.INSTANCE;
    }

    @Redirect(
            method = "submitFrame()V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VkSubmitInfo;pWaitSemaphores(Ljava/nio/LongBuffer;)Lorg/lwjgl/vulkan/VkSubmitInfo;"),
            remap = false,
            require = 0
    )
    private VkSubmitInfo smoothgl$combineWaitSemaphores(VkSubmitInfo submitInfo, LongBuffer originalWaits) {
        if (!smoothgl$enabled()) {
            return submitInfo.pWaitSemaphores(originalWaits);
        }

        SynchronizationSemaphoreExt sync = smoothgl$sync();
        if (originalWaits != null) {
            LongBuffer copy = originalWaits.duplicate();
            while (copy.hasRemaining()) sync.smoothgl$addWaitSemaphore(copy.get());
        }

        MemoryStack stack = MemoryStack.stackGet();
        LongBuffer waits = sync.smoothgl$getWaitSemaphores(stack);
        int count = waits.remaining();
        if (count == 0) {
            return submitInfo.pWaitSemaphores(originalWaits);
        }

        IntBuffer stages = stack.mallocInt(count);
        for (int i = 0; i < count; i++) stages.put(i, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT);
        // The swapchain image-available semaphore is appended after the in-frame upload semaphores.
        stages.put(count - 1, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);

        submitInfo.waitSemaphoreCount(count);
        submitInfo.pWaitDstStageMask(stages);

        if (!smoothgl$logged) {
            smoothgl$logged = true;
            System.out.println("[SmoothGL/Vulkan] In-frame semaphore sync active (backport of VulkanMod 9be5fe8 / 0.5.7-dev)");
        }

        return submitInfo.pWaitSemaphores(waits);
    }

    @Redirect(
            method = "submitFrame()V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VkSubmitInfo;pWaitDstStageMask(Ljava/nio/IntBuffer;)Lorg/lwjgl/vulkan/VkSubmitInfo;"),
            remap = false,
            require = 0
    )
    private VkSubmitInfo smoothgl$keepCombinedStageMasks(VkSubmitInfo submitInfo, IntBuffer originalMask) {
        if (smoothgl$enabled()) return submitInfo;
        return submitInfo.pWaitDstStageMask(originalMask);
    }

    @Inject(
            method = "submitFrame()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/vulkan/KHRSwapchain;vkQueuePresentKHR(Lorg/lwjgl/vulkan/VkQueue;Lorg/lwjgl/vulkan/VkPresentInfoKHR;)I",
                    shift = At.Shift.BEFORE
            ),
            remap = false,
            require = 0
    )
    private void smoothgl$scheduleSemaphoreCommandBufferReset(CallbackInfo ci) {
        if (smoothgl$enabled()) smoothgl$sync().smoothgl$scheduleCbReset();
    }
}
