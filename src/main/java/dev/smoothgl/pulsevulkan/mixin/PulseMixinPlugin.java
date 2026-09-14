package dev.smoothgl.pulsevulkan.mixin;

import dev.smoothgl.pulsevulkan.LwjglStackBootstrap;
import dev.smoothgl.pulsevulkan.PulseDiagnostics;
import dev.smoothgl.pulsevulkan.PulseVisualsDetector;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class PulseMixinPlugin implements IMixinConfigPlugin {
    static {
        // Mixin bootstrap happens extremely early. Configure LWJGL here as a
        // second line of defense before any renderer class can initialize its
        // default 64 KiB MemoryStack.
        LwjglStackBootstrap.ensureConfigured();
    }

    private boolean enabled;

    @Override
    public void onLoad(String mixinPackage) {
        boolean forced = Boolean.getBoolean("smoothgl.pulse.force");
        boolean vulkan = FabricLoader.getInstance().isModLoaded("vulkanmod");
        boolean pulse = PulseVisualsDetector.isPresent();
        enabled = vulkan && (pulse || forced);
        if (enabled) {
            PulseDiagnostics.infoOnce("mixins", "Pulse/Vulkan compatibility mixins enabled");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return enabled;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
