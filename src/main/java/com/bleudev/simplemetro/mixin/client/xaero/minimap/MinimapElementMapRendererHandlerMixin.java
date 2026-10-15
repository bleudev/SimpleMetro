package com.bleudev.simplemetro.mixin.client.xaero.minimap;

import com.bleudev.simplemetro.client.render.xaero.XaeroMinimapSchemeRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xaero.common.HudMod;
import xaero.hud.gui.util.GuiUtils;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderer;
import xaero.hud.minimap.element.render.MinimapElementRendererHandler;
import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(targets = {"xaero.hud.minimap.element.render.map.MinimapElementMapRendererHandler"})
public abstract class MinimapElementMapRendererHandlerMixin extends MinimapElementRendererHandler {
    @Shadow
    private double ps;

    @Shadow
    private double zoom;

    @Shadow
    private double pc;

    protected MinimapElementMapRendererHandlerMixin(HudMod modMain, List<MinimapElementRenderer<?, ?>> renderers, MinimapElementRenderLocation location, int indexLimit, MinimapElementGraphics guiGraphics) {
        super(modMain, renderers, location, indexLimit, guiGraphics);
    }

    @Override
    public void render(Vec3 renderPos, float partialTicks, RenderTarget framebuffer, double backgroundCoordinateScale, ResourceKey<Level> mapDimension) {
        super.render(renderPos, partialTicks, framebuffer, backgroundCoordinateScale, mapDimension);
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        XaeroMinimapSchemeRenderer.render(
            this.guiGraphics,
            renderPos,
            this.ps,
            this.pc,
            this.zoom,
            GuiUtils.getMinimapScale(HudMod.INSTANCE.getHudConfigs().getClientConfigManager()),
            backgroundCoordinateScale,
            level.dimensionType().coordinateScale()
        );
    }
}
