package com.singularity_iteration.mio_icif.Items.Cell;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

public class DynamicCellColorProvider implements net.minecraft.client.color.item.ItemColor {

    private static final int SAMPLE_X = 6;
    private static final int SAMPLE_Y = 8;

    private final Map<Fluid, Integer> colorCache = new IdentityHashMap<>();
    private static Field mipLevelField = null;
    private static boolean fieldInitialized = false;

    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 1) return -1;
        if (!(stack.getItem() instanceof mio_icif_dynamic_cell dynamicCell)) return -1;

        FluidStack fluid = dynamicCell.getFluid(stack);
        if (fluid.isEmpty()) return -1;

        if (mio_icif_cells.getCellTypeForFluid(fluid.getFluid()) != mio_icif_cells.CELL_TYPE_GENERIC) {
            return -1;
        }

        return getFluidColor(fluid.getFluid());
    }

    private int getFluidColor(Fluid fluid) {
        Integer cached = colorCache.get(fluid);
        if (cached != null) return cached;

        int color = sampleFluidColor(fluid);
        colorCache.put(fluid, color);
        return color;
    }

    private int sampleFluidColor(Fluid fluid) {
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluidType());

        int tintColor = extensions.getTintColor(new FluidStack(fluid, 1000));
        if (tintColor != 0xFFFFFFFF && tintColor != -1) {
            return tintColor;
        }

        var stillTexture = extensions.getStillTexture();
        if (stillTexture == null) return 0xFFFFFFFF;

        try {
            Minecraft mc = Minecraft.getInstance();
            TextureAtlasSprite sprite = mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(stillTexture);
            SpriteContents contents = sprite.contents();

            NativeImage image = getSpriteImage(contents);
            if (image == null) return 0xFFFFFFFF;

            int w = image.getWidth();
            int h = image.getHeight();
            if (w <= SAMPLE_X || h <= SAMPLE_Y) return 0xFFFFFFFF;

            int pixel = image.getPixelRGBA(SAMPLE_X, SAMPLE_Y);
            int a = (pixel >> 24) & 0xFF;
            if (a == 0) return 0xFFFFFFFF;

            int r = pixel & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = (pixel >> 16) & 0xFF;

            return (0xFF << 24) | (r << 16) | (g << 8) | b;
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }

    private static NativeImage getSpriteImage(SpriteContents contents) {
        if (!fieldInitialized) {
            try {
                mipLevelField = SpriteContents.class.getDeclaredField("byMipLevel");
                mipLevelField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                try {
                    for (Field f : SpriteContents.class.getDeclaredFields()) {
                        if (f.getType().isArray() && f.getType().getComponentType() == NativeImage.class) {
                            mipLevelField = f;
                            mipLevelField.setAccessible(true);
                            break;
                        }
                    }
                } catch (Exception ex) {
                    mipLevelField = null;
                }
            }
            fieldInitialized = true;
        }

        if (mipLevelField == null) return null;

        try {
            NativeImage[] images = (NativeImage[]) mipLevelField.get(contents);
            return images != null && images.length > 0 ? images[0] : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void invalidateCache() {
        colorCache.clear();
    }
}