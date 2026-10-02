package com.miophas.singularity_iteration.common.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 进度页签背景彩蛋。
 * <p>
 * 原版把整张 background 当成一个 16x16 的 tile 平铺满内容区（234x113）。
 * 这里保持原版平铺不变，只对平铺网格中的部分格子按概率换成 Miopha.png，
 * 即"背景里冒出 Miopha"，而不是整片背景被替换。
 * <p>
 * 判定只针对本模组命名空间的页签，避免影响原版 tab。
 */
@Mixin(AdvancementTab.class)
@SuppressWarnings("null")
public class AdvancementTabMiophaMixin {

    /**
     * 彩蛋贴图，与 background.png 同目录。
     * 注意：ResourceLocation 的 path 只允许 [a-z0-9/._-]，文件名必须全小写。
     */
    @Unique
    private static final ResourceLocation MIOPHA_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "mio_icif", "textures/achievements/miopha.png"
    );

    /** 单个背景 tile 的尺寸，与原版保持一致。 */
    @Unique
    private static final int TILE_SIZE = 16;

    /** 平铺网格范围：原版循环为 i1 in [-1, 15]、j1 in [-1, 8]。 */
    @Unique
    private static final int GRID_START_X = -1;

    @Unique
    private static final int GRID_END_X = 15;

    @Unique
    private static final int GRID_START_Y = -1;

    @Unique
    private static final int GRID_END_Y = 8;

    /**
     * 彩蛋出现概率：先判定"本次是否出现彩蛋"，命中后再随机挑一个瓦片替换。
     * 即整片背景中出现 1 个 Miopha 的概率（每打开一次页签判定一次）。
     */
    @Unique
    private static final double MIOPHA_CHANCE = 0.00001D;

    @Shadow
    private double scrollX;

    @Shadow
    private double scrollY;

    /**
     * 每格是否绘制彩蛋，null 表示尚未生成。
     * 只生成一次并缓存，避免每帧重新随机导致闪烁。
     */
    @Unique
    private boolean[] miophaCells;

    @Inject(
        method = "drawContents",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementWidget;drawConnectivity(Lnet/minecraft/client/gui/GuiGraphics;IIZ)V",
            ordinal = 0
        )
    )
    private void mio_icif$drawMiophaTiles(GuiGraphics guiGraphics, int x, int y, CallbackInfo ci) {
        if (this.miophaCells == null) {
            this.miophaCells = mio_icif$rollCells();
        }

        int i = Mth.floor(this.scrollX);
        int j = Mth.floor(this.scrollY);
        int k = i % TILE_SIZE;
        int l = j % TILE_SIZE;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (int i1 = GRID_START_X; i1 <= GRID_END_X; i1++) {
            for (int j1 = GRID_START_Y; j1 <= GRID_END_Y; j1++) {
                if (this.miophaCells[mio_icif$cellIndex(i1, j1)]) {
                    guiGraphics.blit(
                        MIOPHA_TEXTURE,
                        k + TILE_SIZE * i1,
                        l + TILE_SIZE * j1,
                        0.0F,
                        0.0F,
                        TILE_SIZE,
                        TILE_SIZE,
                        TILE_SIZE,
                        TILE_SIZE
                    );
                }
            }
        }
    }

    @Unique
    private boolean[] mio_icif$rollCells() {
        int width = GRID_END_X - GRID_START_X + 1;
        int height = GRID_END_Y - GRID_START_Y + 1;
        boolean[] cells = new boolean[width * height];
        if (mio_icif$isOwnTab() && Math.random() < MIOPHA_CHANCE) {
            // 命中彩蛋：在平铺网格里随机挑一个瓦片替换成 Miopha
            cells[(int) (Math.random() * cells.length)] = true;
        }
        return cells;
    }

    @Unique
    private static int mio_icif$cellIndex(int i1, int j1) {
        int height = GRID_END_Y - GRID_START_Y + 1;
        return (i1 - GRID_START_X) * height + (j1 - GRID_START_Y);
    }

    @Unique
    private boolean mio_icif$isOwnTab() {
        AdvancementTab self = (AdvancementTab) (Object) this;
        return self.getDisplay()
            .getBackground()
            .map(background -> "mio_icif".equals(background.getNamespace()))
            .orElse(false);
    }
}
