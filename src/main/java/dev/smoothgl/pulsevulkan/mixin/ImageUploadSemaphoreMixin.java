package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.SynchronizationSemaphoreExt;
import net.vulkanmod.render.texture.ImageUploadHelper;
import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.queue.Queue;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Uses the newer VulkanMod semaphore submission path for same-frame image uploads. */
@Mixin(value = ImageUploadHelper.class, remap = false)
public abstract class ImageUploadSemaphoreMixin {
    @Shadow @Final private Queue queue;
    @Shadow private CommandPool.CommandBuffer currentCmdBuffer;

    @Inject(method = "submitCommands()V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void smoothgl$submitWithSemaphore(CallbackInfo ci) {
        if (!Boolean.parseBoolean(System.getProperty("smoothgl.vulkan.semaphoreSync", "true"))) return;

        if (this.currentCmdBuffer == null) {
            ci.cancel();
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // VulkanMod 0.5.4 already has the useSemaphore boolean at this lowest layer.
            this.currentCmdBuffer.submitCommands(stack, this.queue.queue(), true);
        }

        ((SynchronizationSemaphoreExt) (Object) Synchronization.INSTANCE)
                .smoothgl$addCommandBuffer(this.currentCmdBuffer, true);
        this.currentCmdBuffer = null;

        ci.cancel();
    }
}
