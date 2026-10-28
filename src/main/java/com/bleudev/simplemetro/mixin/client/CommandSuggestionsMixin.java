package com.bleudev.simplemetro.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.suggestion.Suggestion;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static com.bleudev.simplemetro.client.util.CommandSuggestionUtilKt.shouldForceRenderTooltip;

@Environment(EnvType.CLIENT)
@Mixin(CommandSuggestions.SuggestionsList.class)
public class CommandSuggestionsMixin {
    @Shadow
    @Final
    private String originalContents;

    @Shadow
    @Final
    private List<Suggestion> suggestionList;

    @Shadow
    private int current;

    @Shadow
    @Final
    private Rect2i rect;

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;II)V"))
    private void cancelTooltip(GuiGraphicsExtractor instance, Font font, Component text, int xo, int yo, Operation<Void> original) {
        if (!shouldForceRenderTooltip(originalContents)) {
            original.call(instance, font, text, xo, yo);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void extractTooltipIfNotHovered(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (shouldForceRenderTooltip(originalContents)) {
            Message tooltip = this.suggestionList.get(this.current).getTooltip();
            if (tooltip != null) {
                graphics.setTooltipForNextFrame(Minecraft.getInstance().font, ComponentUtils.fromMessage(tooltip), this.rect.getX() + this.rect.getWidth(), this.rect.getY());
            }
        }
    }
}
