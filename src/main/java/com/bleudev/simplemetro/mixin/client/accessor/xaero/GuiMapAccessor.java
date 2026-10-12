package com.bleudev.simplemetro.mixin.client.accessor.xaero;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.gui.GuiMap;

@Environment(EnvType.CLIENT)
@Mixin(GuiMap.class)
public interface GuiMapAccessor {
    @Accessor("cameraX")
    double simplemetro$cameraX();
    @Accessor("cameraZ")
    double simplemetro$cameraZ();
    @Accessor("scale")
    double simplemetro$scale();
    @Accessor("userScale")
    double simplemetro$userScale();
}
