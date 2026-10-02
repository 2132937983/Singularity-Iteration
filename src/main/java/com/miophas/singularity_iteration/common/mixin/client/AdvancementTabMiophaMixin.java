package com.miophas.singularity_iteration.common.mixin.client;

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
 * Advancement tab background easter egg.
 * <p>
 * Vanilla tiles the whole {@code background} texture as a 16x16 tile across the
 * 234x113 content area. This keeps that tiling untouched and, very rarely, replaces a
 * single tile of the grid with {@code miopha.png}, so the egg "shows up inside" the
 * background instead of replacing it.
 * <p>
 * Only applies to tabs whose background belongs to this mod's namespace.
 */
@Mixin(AdvancementTab.class)
@SuppressWarnings("null")
public class AdvancementTabMiophaMixin {

    /**
     * Easter egg texture, next to {@code background.png}.
     * Note: a ResourceLocation path only allows [a-z0-9/._-], so the file name must be
     * all lowercase.
     */
    @Unique
    private static final ResourceLocation SI_MIOPHA_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "mio_icif", "textures/achievements/miopha.png"
    );

    /** Vanilla background tile size. */
    @Unique
    private static final int SI_TILE_SIZE = 16;

    /** Vanilla tiling loop range: i1 in [-1, 15], j1 in [-1, 8]. */
    @Unique
    private static final int SI_GRID_START_X = -1;

    @Unique
    private static final int SI_GRID_END_X = 15;

    @Unique
    private static final int SI_GRID_START_Y = -1;

    @Unique
    private static final int SI_GRID_END_Y = 8;

    /**
     * Egg chance: first roll whether the egg appears at all, then pick one random tile.
     * Rolled once per tab instance, i.e. once per time the advancements screen is opened.
     */
    @Unique
    private static final double SI_MIOPHA_CHANCE = 0.00001D;

    @Shadow
    private double scrollX;

    @Shadow
    private double scrollY;

    /**
     * Whether each tile draws the egg. Null until generated; cached so it does not
     * re-randomise (and flicker) every frame.
     */
    @Unique
    private boolean[] si$miophaCells;

    @Inject(
        method = "drawContents",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementWidget;drawConnectivity(Lnet/minecraft/client/gui/GuiGraphics;IIZ)V",
            ordinal = 0
        )
    )
    private void si$drawMiophaTiles(GuiGraphics guiGraphics, int x, int y, CallbackInfo ci) {
        if (this.si$miophaCells == null) {
            this.si$miophaCells = this.si$rollCells();
        }

        int i = Mth.floor(this.scrollX);
        int j = Mth.floor(this.scrollY);
        int k = i % SI_TILE_SIZE;
        int l = j % SI_TILE_SIZE;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (int i1 = SI_GRID_START_X; i1 <= SI_GRID_END_X; i1++) {
            for (int j1 = SI_GRID_START_Y; j1 <= SI_GRID_END_Y; j1++) {
                if (this.si$miophaCells[si$cellIndex(i1, j1)]) {
                    guiGraphics.blit(
                        SI_MIOPHA_TEXTURE,
                        k + SI_TILE_SIZE * i1,
                        l + SI_TILE_SIZE * j1,
                        0.0F,
                        0.0F,
                        SI_TILE_SIZE,
                        SI_TILE_SIZE,
                        SI_TILE_SIZE,
                        SI_TILE_SIZE
                    );
                }
            }
        }
    }

    @Unique
    private boolean[] si$rollCells() {
        int width = SI_GRID_END_X - SI_GRID_START_X + 1;
        int height = SI_GRID_END_Y - SI_GRID_START_Y + 1;
        boolean[] cells = new boolean[width * height];
        if (this.si$isOwnTab() && Math.random() < SI_MIOPHA_CHANCE) {
            // Egg hit: replace one random tile of the tiling grid with miopha.
            cells[(int) (Math.random() * cells.length)] = true;
        }
        return cells;
    }

    @Unique
    private static int si$cellIndex(int i1, int j1) {
        int height = SI_GRID_END_Y - SI_GRID_START_Y + 1;
        return (i1 - SI_GRID_START_X) * height + (j1 - SI_GRID_START_Y);
    }

    @Unique
    private boolean si$isOwnTab() {
        AdvancementTab self = (AdvancementTab) (Object) this;
        return self.getDisplay()
            .getBackground()
            .map(background -> "mio_icif".equals(background.getNamespace()))
            .orElse(false);
    }
}
