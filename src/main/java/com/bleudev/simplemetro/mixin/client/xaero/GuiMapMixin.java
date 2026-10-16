package com.bleudev.simplemetro.mixin.client.xaero;

import com.bleudev.simplemetro.client.render.xaero.XaeroMapSchemeRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(targets = {"xaero.map.gui.GuiMap"})
public class GuiMapMixin {
    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lxaero/map/graphics/ImprovedFramebuffer;bindDefaultFramebuffer(Lnet/minecraft/client/Minecraft;)V", shift = At.Shift.AFTER))
    private void renderMetroScheme(GuiGraphicsExtractor guiGraphics, int scaledMouseX, int scaledMouseY, float delta, CallbackInfo ci) {
        XaeroMapSchemeRenderer.extractRenderState$com_bleudev_simplemetro(this, guiGraphics, scaledMouseX, scaledMouseY);
    }
}
