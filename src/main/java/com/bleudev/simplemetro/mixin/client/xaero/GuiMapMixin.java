package com.bleudev.simplemetro.mixin.client.xaero;

import com.bleudev.simplemetro.client.render.xaero.XaeroMapSchemeRenderer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;

@Environment(EnvType.CLIENT)
@Mixin(targets = {"xaero.map.gui.GuiMap"})
public class GuiMapMixin {
    @Shadow
    private double cameraX;

    @Shadow
    private double cameraZ;

    @Shadow
    private double scale;

    @Shadow
    private double userScale;

    @Shadow
    private double screenScale;

    @Shadow
    private MapProcessor mapProcessor;

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lxaero/map/graphics/ImprovedFramebuffer;bindDefaultFramebuffer(Lnet/minecraft/client/Minecraft;)V", shift = At.Shift.AFTER))
    private void renderMetroScheme(GuiGraphicsExtractor guiGraphics, int scaledMouseX, int scaledMouseY, float delta, CallbackInfo ci) {
        XaeroMapSchemeRenderer.extractRenderState$com_bleudev_simplemetro(
            guiGraphics,
            scaledMouseX,
            scaledMouseY,
            this.cameraX,
            this.cameraZ,
            this.scale <= 0f ? this.userScale : this.scale,
            this.screenScale,
            this.mapProcessor.getMapWorld().getCurrentDimension().getDimId().identifier()
        );
    }

    @WrapMethod(method = "mouseClicked")
    private boolean customMouseClick(MouseButtonEvent event, boolean doubleClick, Operation<Boolean> original) {
        boolean var1 = original.call(event, doubleClick);
        if (!var1) {
            return XaeroMapSchemeRenderer.mouseClicked$com_bleudev_simplemetro(event, doubleClick);
        } else {
            return true;
        }
    }
}
