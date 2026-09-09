package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * GUI 渲染工具类，提供通用的流体平铺渲染方法
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings({"null", "deprecation"})
public final class mio_icif_GuiUtils {

    private mio_icif_GuiUtils() {
    }

    /**
     * 在 GUI 中平铺渲染流体液位条
     *
     * @param guiGraphics 绘制上下文
     * @param x           液位条左上角在 GUI 中的 x 坐标
     * @param y           液位条左上角在 GUI 中的 y 坐标
     * @param width       液位条总宽度
     * @param height      液位条总高度
     * @param fluid       要渲染的流体及其当前量
     * @param capacity    流体储罐容量
     * @param topDown     true 表示从上往下填充，false 表示从下往上填充
     */
    public static void renderFluidBar(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                      FluidStack fluid, int capacity, boolean topDown) {
        if (fluid.isEmpty() || capacity <= 0) return;

        int fillHeight = (int) ((float) fluid.getAmount() / capacity * height);
        if (fillHeight <= 0) return;

        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluidType());
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
            .apply(extensions.getStillTexture());

        int color = extensions.getTintColor(fluid);
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        int tileSize = sprite.contents().width();
        int fillTop = topDown ? y : y + height - fillHeight;
        int fillBottom = topDown ? y + fillHeight : y + height;

        guiGraphics.setColor(r, g, b, a);
        guiGraphics.enableScissor(x, fillTop, x + width, fillBottom);

        for (int tileY = fillTop; tileY < fillBottom; tileY += tileSize) {
            for (int tileX = x; tileX < x + width; tileX += tileSize) {
                guiGraphics.blit(tileX, tileY, 0, tileSize, tileSize, sprite);
            }
        }

        guiGraphics.disableScissor();
        guiGraphics.setColor(1f, 1f, 1f, 1f);
    }

    /**
     * 在 GUI 中平铺渲染流体液位条（默认从下往上填充）
     */
    public static void renderFluidBar(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                      FluidStack fluid, int capacity) {
        renderFluidBar(guiGraphics, x, y, width, height, fluid, capacity, false);
    }

    /**
     * 在 GUI 中平铺渲染流体液位条（从左往右填充）
     */
    public static void renderFluidBarHorizontal(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                                FluidStack fluid, int capacity) {
        if (fluid.isEmpty() || capacity <= 0) return;

        int fillWidth = (int) ((float) fluid.getAmount() / capacity * width);
        if (fillWidth <= 0) return;

        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluidType());
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
            .apply(extensions.getStillTexture());

        int color = extensions.getTintColor(fluid);
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        int tileSize = sprite.contents().width();
        int fillRight = x + fillWidth;

        guiGraphics.setColor(r, g, b, a);
        guiGraphics.enableScissor(x, y, fillRight, y + height);

        for (int tileY = y; tileY < y + height; tileY += tileSize) {
            for (int tileX = x; tileX < fillRight; tileX += tileSize) {
                guiGraphics.blit(tileX, tileY, 0, tileSize, tileSize, sprite);
            }
        }

        guiGraphics.disableScissor();
        guiGraphics.setColor(1f, 1f, 1f, 1f);
    }
}