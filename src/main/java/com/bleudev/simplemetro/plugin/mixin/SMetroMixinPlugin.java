package com.bleudev.simplemetro.plugin.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

public class SMetroMixinPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith("com.bleudev.simplemetro.mixin.client.xaero.minimap")) {
            return FabricLoader.getInstance().isModLoaded("xaerominimap");
        }
        if (mixinClassName.startsWith("com.bleudev.simplemetro.mixin.client.xaero")) {
            return FabricLoader.getInstance().isModLoaded("xaeroworldmap");
        }
        return true;
    }
}
