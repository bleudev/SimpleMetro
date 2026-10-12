package com.bleudev.simplemetro.mixin.client.xaero;

import com.bleudev.simplemetro.client.util.MetroSchemeRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.gui.GuiMap;

@Environment(EnvType.CLIENT)
@Mixin(GuiMap.class)
public class GuiMapMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void renderMetroScheme(GuiGraphicsExtractor guiGraphics, int scaledMouseX, int scaledMouseY, float delta, CallbackInfo ci) {
        MetroSchemeRenderer.getInstance().extractRenderState((GuiMap) (Object) this, guiGraphics);
    }
}
