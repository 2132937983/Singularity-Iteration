package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.util.mio_icif_gui_global_variables;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 能量发电机的GUI基类
 */
@SuppressWarnings("null")
public abstract class mio_icif_screen<T extends net.minecraft.world.inventory.AbstractContainerMenu> extends AbstractContainerScreen<T> {

    // 引用全局常量
    protected static final ResourceLocation ATLAS_TEXTURE = mio_icif_gui_global_variables.ATLAS_TEXTURE;
    protected static final int ATLAS_WIDTH = mio_icif_gui_global_variables.ATLAS_WIDTH;
    protected static final int ATLAS_HEIGHT = mio_icif_gui_global_variables.ATLAS_HEIGHT;

    protected static final int ENERGY_BAR_BG_WIDTH = mio_icif_gui_global_variables.ENERGY_BAR_BG_WIDTH;
    protected static final int ENERGY_BAR_BG_HEIGHT = mio_icif_gui_global_variables.ENERGY_BAR_BG_HEIGHT;
    protected static final int ENERGY_BAR_BG_TEXTURE_X = mio_icif_gui_global_variables.ENERGY_BAR_BG_TEXTURE_X;
    protected static final int ENERGY_BAR_BG_TEXTURE_Y = mio_icif_gui_global_variables.ENERGY_BAR_BG_TEXTURE_Y;

    protected static final int ENERGY_BAR_WIDTH = mio_icif_gui_global_variables.ENERGY_BAR_WIDTH;
    protected static final int ENERGY_BAR_HEIGHT = mio_icif_gui_global_variables.ENERGY_BAR_HEIGHT;
    protected static final int ENERGY_BAR_TEXTURE_X = mio_icif_gui_global_variables.ENERGY_BAR_TEXTURE_X;
    protected static final int ENERGY_BAR_TEXTURE_Y = mio_icif_gui_global_variables.ENERGY_BAR_TEXTURE_Y;

    protected static final int KINETIC_ENERGY_BAR_WIDTH = mio_icif_gui_global_variables.KINETIC_ENERGY_BAR_WIDTH;
    protected static final int KINETIC_ENERGY_BAR_HEIGHT = mio_icif_gui_global_variables.KINETIC_ENERGY_BAR_HEIGHT;
    protected static final int KINETIC_ENERGY_BAR_TEXTURE_X = mio_icif_gui_global_variables.KINETIC_ENERGY_BAR_TEXTURE_X;
    protected static final int KINETIC_ENERGY_BAR_TEXTURE_Y = mio_icif_gui_global_variables.KINETIC_ENERGY_BAR_TEXTURE_Y;

    protected static final int KINETIC_ENERGY_BAR2_WIDTH = mio_icif_gui_global_variables.KINETIC_ENERGY_BAR2_WIDTH;
    protected static final int KINETIC_ENERGY_BAR2_HEIGHT = mio_icif_gui_global_variables.KINETIC_ENERGY_BAR2_HEIGHT;
    protected static final int KINETIC_ENERGY_BAR2_TEXTURE_X = mio_icif_gui_global_variables.KINETIC_ENERGY_BAR2_TEXTURE_X;
    protected static final int KINETIC_ENERGY_BAR2_TEXTURE_Y = mio_icif_gui_global_variables.KINETIC_ENERGY_BAR2_TEXTURE_Y;

    protected static final int LIGHTNING_BG_WIDTH = mio_icif_gui_global_variables.LIGHTNING_BG_WIDTH;
    protected static final int LIGHTNING_BG_HEIGHT = mio_icif_gui_global_variables.LIGHTNING_BG_HEIGHT;
    protected static final int LIGHTNING_BG_U = mio_icif_gui_global_variables.LIGHTNING_BG_TEXTURE_X;
    protected static final int LIGHTNING_BG_V = mio_icif_gui_global_variables.LIGHTNING_BG_TEXTURE_Y;

    protected static final int LIGHTNING_WIDTH = mio_icif_gui_global_variables.LIGHTNING_WIDTH;
    protected static final int LIGHTNING_HEIGHT = mio_icif_gui_global_variables.LIGHTNING_HEIGHT;
    protected static final int LIGHTNING_U = mio_icif_gui_global_variables.LIGHTNING_U;
    protected static final int LIGHTNING_V = mio_icif_gui_global_variables.LIGHTNING_V;

    protected static final int ARROW_WIDTH = mio_icif_gui_global_variables.ARROW_WIDTH;
    protected static final int ARROW_HEIGHT = mio_icif_gui_global_variables.ARROW_HEIGHT;
    protected static final int ARROW_U = mio_icif_gui_global_variables.ARROW_U;
    protected static final int ARROW_V = mio_icif_gui_global_variables.ARROW_V;

    // 金属成型机专属进度条
    protected static final int METAL_FORMER_PROGRESS_WIDTH = 48;
    protected static final int METAL_FORMER_PROGRESS_HEIGHT = 17;
    // 背景纹理位置 xy=(208,304)
    protected static final int METAL_FORMER_PROGRESS_BG_U = mio_icif_gui_global_variables.METAL_FORMER_WORK_PROGRESS_BG_X;
    protected static final int METAL_FORMER_PROGRESS_BG_V = mio_icif_gui_global_variables.METAL_FORMER_WORK_PROGRESS_BG_Y;
    // 进度纹理位置 xy=(160,304)
    protected static final int METAL_FORMER_PROGRESS_U = mio_icif_gui_global_variables.METAL_FORMER_WORK_PROGRESS_X;
    protected static final int METAL_FORMER_PROGRESS_V = mio_icif_gui_global_variables.METAL_FORMER_WORK_PROGRESS_Y;

    protected static final int ENERGY_BAR2_WIDTH = mio_icif_gui_global_variables.ENERGY_BAR2_WIDTH;
    protected static final int ENERGY_BAR2_HEIGHT = mio_icif_gui_global_variables.ENERGY_BAR2_HEIGHT;
    protected static final int ENERGY_BAR2_U = mio_icif_gui_global_variables.ENERGY_BAR2_U;
    protected static final int ENERGY_BAR2_V = mio_icif_gui_global_variables.ENERGY_BAR2_V;

    protected static final int KINETIC_BAR_WIDTH = mio_icif_gui_global_variables.KINETIC_BAR_WIDTH;
    protected static final int KINETIC_BAR_HEIGHT = mio_icif_gui_global_variables.KINETIC_BAR_HEIGHT;
    protected static final int KINETIC_BAR_U = mio_icif_gui_global_variables.KINETIC_BAR_U;
    protected static final int KINETIC_BAR_V = mio_icif_gui_global_variables.KINETIC_BAR_V;

    protected static final int WORK_ICON_WIDTH = mio_icif_gui_global_variables.WORK_ICON_WIDTH;
    protected static final int WORK_ICON_HEIGHT = mio_icif_gui_global_variables.WORK_ICON_HEIGHT;
    protected static final int WORK_ACTIVE_U = mio_icif_gui_global_variables.WORK_ACTIVE_U;
    protected static final int WORK_ACTIVE_V = mio_icif_gui_global_variables.WORK_ACTIVE_V;
    protected static final int WORK_INACTIVE_U = mio_icif_gui_global_variables.WORK_INACTIVE_U;
    protected static final int WORK_INACTIVE_V = mio_icif_gui_global_variables.WORK_INACTIVE_V;

    protected static final int HEAT_BAR_WIDTH = mio_icif_gui_global_variables.HEAT_BAR_WIDTH;
    protected static final int HEAT_BAR_HEIGHT = mio_icif_gui_global_variables.HEAT_BAR_HEIGHT;
    protected static final int HEAT_BAR_U = mio_icif_gui_global_variables.HEAT_BAR_U;
    protected static final int HEAT_BAR_V = mio_icif_gui_global_variables.HEAT_BAR_V;

    protected static final int HEAT_OK_WIDTH = mio_icif_gui_global_variables.HEAT_OK_WIDTH;
    protected static final int HEAT_OK_HEIGHT = mio_icif_gui_global_variables.HEAT_OK_HEIGHT;
    protected static final int HEAT_OK_U = mio_icif_gui_global_variables.HEAT_OK_U;
    protected static final int HEAT_OK_V = mio_icif_gui_global_variables.HEAT_OK_V;

    protected static final int AIR_OK_WIDTH = mio_icif_gui_global_variables.AIR_OK_WIDTH;
    protected static final int AIR_OK_HEIGHT = mio_icif_gui_global_variables.AIR_OK_HEIGHT;
    protected static final int AIR_OK_U = mio_icif_gui_global_variables.AIR_OK_U;
    protected static final int AIR_OK_V = mio_icif_gui_global_variables.AIR_OK_V;

    protected static final int BLAST_PROGRESS_WIDTH = mio_icif_gui_global_variables.BLAST_PROGRESS_WIDTH;
    protected static final int BLAST_PROGRESS_HEIGHT = mio_icif_gui_global_variables.BLAST_PROGRESS_HEIGHT;
    protected static final int BLAST_PROGRESS_U = mio_icif_gui_global_variables.BLAST_PROGRESS_U;
    protected static final int BLAST_PROGRESS_V = mio_icif_gui_global_variables.BLAST_PROGRESS_V;

    protected static final int PROGRESS_BAR_BG_WIDTH = mio_icif_gui_global_variables.PROGRESS_BAR_BG_WIDTH;
    protected static final int PROGRESS_BAR_BG_HEIGHT = mio_icif_gui_global_variables.PROGRESS_BAR_BG_HEIGHT;
    protected static final int PROGRESS_BAR_BG_U = mio_icif_gui_global_variables.PROGRESS_BAR_BG_TEXTURE_X;
    protected static final int PROGRESS_BAR_BG_V = mio_icif_gui_global_variables.PROGRESS_BAR_BG_TEXTURE_Y;

    protected static final int COMPRESSOR_PROGRESS_BG_WIDTH = mio_icif_gui_global_variables.COMPRESSOR_WORK_PROGRESS_BG_U;
    protected static final int COMPRESSOR_PROGRESS_BG_HEIGHT = mio_icif_gui_global_variables.COMPRESSOR_WORK_PROGRESS_BG_V;
    protected static final int COMPRESSOR_PROGRESS_BG_U = mio_icif_gui_global_variables.COMPRESSOR_WORK_PROGRESS_BG_X;
    protected static final int COMPRESSOR_PROGRESS_BG_V = mio_icif_gui_global_variables.COMPRESSOR_WORK_PROGRESS_BG_Y;

    protected static final int COMPRESSOR_PROGRESS_WIDTH = mio_icif_gui_global_variables.COMPRESSOR_WORK_PROGRESS_U;
    protected static final int COMPRESSOR_PROGRESS_HEIGHT = mio_icif_gui_global_variables.COMPRESSOR_WORK_PROGRESS_V;
    protected static final int COMPRESSOR_PROGRESS_U = mio_icif_gui_global_variables.COMPRESSOR_WORK_PROGRESS_X;
    protected static final int COMPRESSOR_PROGRESS_V = mio_icif_gui_global_variables.COMPRESSOR_WORK_PROGRESS_Y;

    protected static final int CONDENSER_PROGRESS_WIDTH = mio_icif_gui_global_variables.CONDENSER_WORK_PROGRESS_U;
    protected static final int CONDENSER_PROGRESS_HEIGHT = mio_icif_gui_global_variables.CONDENSER_WORK_PROGRESS_V;
    protected static final int CONDENSER_PROGRESS_U = mio_icif_gui_global_variables.CONDENSER_WORK_PROGRESS_X;
    protected static final int CONDENSER_PROGRESS_V = mio_icif_gui_global_variables.CONDENSER_WORK_PROGRESS_Y;

    protected static final int EXTRACTOR_PROGRESS_BG_WIDTH = mio_icif_gui_global_variables.EXTRACTOR_WORK_PROGRESS_BG_U;
    protected static final int EXTRACTOR_PROGRESS_BG_HEIGHT = mio_icif_gui_global_variables.EXTRACTOR_WORK_PROGRESS_BG_V;
    protected static final int EXTRACTOR_PROGRESS_BG_U = mio_icif_gui_global_variables.EXTRACTOR_WORK_PROGRESS_BG_X;
    protected static final int EXTRACTOR_PROGRESS_BG_V = mio_icif_gui_global_variables.EXTRACTOR_WORK_PROGRESS_BG_Y;

    protected static final int EXTRACTOR_PROGRESS_WIDTH = mio_icif_gui_global_variables.EXTRACTOR_WORK_PROGRESS_U;
    protected static final int EXTRACTOR_PROGRESS_HEIGHT = mio_icif_gui_global_variables.EXTRACTOR_WORK_PROGRESS_V;
    protected static final int EXTRACTOR_PROGRESS_U = mio_icif_gui_global_variables.EXTRACTOR_WORK_PROGRESS_X;
    protected static final int EXTRACTOR_PROGRESS_V = mio_icif_gui_global_variables.EXTRACTOR_WORK_PROGRESS_Y;

    protected static final int CUTTER_PROGRESS_BG_WIDTH = mio_icif_gui_global_variables.CUTTER_WORK_PROGRESS_BG_U;
    protected static final int CUTTER_PROGRESS_BG_HEIGHT = mio_icif_gui_global_variables.CUTTER_WORK_PROGRESS_BG_V;
    protected static final int CUTTER_PROGRESS_BG_U = mio_icif_gui_global_variables.CUTTER_WORK_PROGRESS_BG_X;
    protected static final int CUTTER_PROGRESS_BG_V = mio_icif_gui_global_variables.CUTTER_WORK_PROGRESS_BG_Y;

    protected static final int CUTTER_PROGRESS_WIDTH = mio_icif_gui_global_variables.CUTTER_WORK_PROGRESS_U;
    protected static final int CUTTER_PROGRESS_HEIGHT = mio_icif_gui_global_variables.CUTTER_WORK_PROGRESS_V;
    protected static final int CUTTER_PROGRESS_U = mio_icif_gui_global_variables.CUTTER_WORK_PROGRESS_X;
    protected static final int CUTTER_PROGRESS_V = mio_icif_gui_global_variables.CUTTER_WORK_PROGRESS_Y;

    protected static final int HEAT_ENERGY_CENTRIFUGE_U = mio_icif_gui_global_variables.HEAT_ENERGY_CENTRIFUGE_U;
    protected static final int HEAT_ENERGY_CENTRIFUGE_V = mio_icif_gui_global_variables.HEAT_ENERGY_CENTRIFUGE_V;
    protected static final int HEAT_ENERGY_CENTRIFUGE_WIDTH = mio_icif_gui_global_variables.HEAT_ENERGY_CENTRIFUGE_X;
    protected static final int HEAT_ENERGY_CENTRIFUGE_HEIGHT = mio_icif_gui_global_variables.HEAT_ENERGY_CENTRIFUGE_Y;

    protected static final int FERMENTER_PROGRESS_WIDTH = mio_icif_gui_global_variables.FERMENTER_PROGRESS_U;
    protected static final int FERMENTER_PROGRESS_HEIGHT = mio_icif_gui_global_variables.FERMENTER_PROGRESS_V;
    protected static final int FERMENTER_PROGRESS_U = mio_icif_gui_global_variables.FERMENTER_PROGRESS_X;
    protected static final int FERMENTER_PROGRESS_V = mio_icif_gui_global_variables.FERMENTER_PROGRESS_Y;

    protected static final int FERMENTER_HEAT_WIDTH = mio_icif_gui_global_variables.FERMENTER_HEAT_U;
    protected static final int FERMENTER_HEAT_HEIGHT = mio_icif_gui_global_variables.FERMENTER_HEAT_V;
    protected static final int FERMENTER_HEAT_U = mio_icif_gui_global_variables.FERMENTER_HEAT_X;
    protected static final int FERMENTER_HEAT_V = mio_icif_gui_global_variables.FERMENTER_HEAT_Y;

    protected static final int FLUID_TANK_BG_WIDTH = mio_icif_gui_global_variables.FLUID_TANK_BG_U;
    protected static final int FLUID_TANK_BG_HEIGHT = mio_icif_gui_global_variables.FLUID_TANK_BG_V;
    protected static final int FLUID_TANK_BG_U = mio_icif_gui_global_variables.FLUID_TANK_BG_TEXTURE_X;
    protected static final int FLUID_TANK_BG_V = mio_icif_gui_global_variables.FLUID_TANK_BG_TEXTURE_Y;

    protected static final int FLUID_TANK_WIDTH = mio_icif_gui_global_variables.FLUID_TANK_U;
    protected static final int FLUID_TANK_HEIGHT = mio_icif_gui_global_variables.FLUID_TANK_V;
    protected static final int FLUID_TANK_U = mio_icif_gui_global_variables.FLUID_TANK_X;
    protected static final int FLUID_TANK_V = mio_icif_gui_global_variables.FLUID_TANK_Y;

    protected static final int FLUID_WIDTH = mio_icif_gui_global_variables.FLUID_U;
    protected static final int FLUID_HEIGHT = mio_icif_gui_global_variables.FLUID_V;

    protected static final int FLUID_TANK_SCALE_WIDTH = mio_icif_gui_global_variables.FLUID_TANK_SCALE_U;
    protected static final int FLUID_TANK_SCALE_HEIGHT = mio_icif_gui_global_variables.FLUID_TANK_SCALE_V;
    protected static final int FLUID_TANK_SCALE_U = mio_icif_gui_global_variables.FLUID_TANK_SCALE_X;
    protected static final int FLUID_TANK_SCALE_V = mio_icif_gui_global_variables.FLUID_TANK_SCALE_Y;

    protected static final int FLAME_ICON_BG_WIDTH = mio_icif_gui_global_variables.FLAME_ICON_BG_U;
    protected static final int FLAME_ICON_BG_HEIGHT = mio_icif_gui_global_variables.FLAME_ICON_BG_V;
    protected static final int FLAME_ICON_BG_U = mio_icif_gui_global_variables.FLAME_ICON_BG_X;
    protected static final int FLAME_ICON_BG_V = mio_icif_gui_global_variables.FLAME_ICON_BG_Y;

    protected static final int FLAME_ICON_WIDTH = mio_icif_gui_global_variables.FLAME_ICON_U;
    protected static final int FLAME_ICON_HEIGHT = mio_icif_gui_global_variables.FLAME_ICON_V;
    protected static final int FLAME_ICON_U = mio_icif_gui_global_variables.FLAME_ICON_X;
    protected static final int FLAME_ICON_V = mio_icif_gui_global_variables.FLAME_ICON_Y;

    // 滑块纹理
    protected static final int SCROLLBAR_TEXTURE_X = mio_icif_gui_global_variables.SCROLLBAR_TEXTURE_X;
    protected static final int SCROLLBAR_TEXTURE_Y = mio_icif_gui_global_variables.SCROLLBAR_TEXTURE_Y;
    protected static final int SCROLLBAR_TEXTURE_WIDTH = mio_icif_gui_global_variables.SCROLLBAR_TEXTURE_U;
    protected static final int SCROLLBAR_TEXTURE_HEIGHT = mio_icif_gui_global_variables.SCROLLBAR_TEXTURE_V;
    
    // 红石模式按钮纹理
    protected static final int REDSTONE_BUTTON_TEXTURE_X = mio_icif_gui_global_variables.REDSTONE_BUTTON_TEXTURE_X;
    protected static final int REDSTONE_BUTTON_TEXTURE_Y = mio_icif_gui_global_variables.REDSTONE_BUTTON_TEXTURE_Y;
    protected static final int REDSTONE_BUTTON_TEXTURE_WIDTH = mio_icif_gui_global_variables.REDSTONE_BUTTON_TEXTURE_U;
    protected static final int REDSTONE_BUTTON_TEXTURE_HEIGHT = mio_icif_gui_global_variables.REDSTONE_BUTTON_TEXTURE_V;
    
    protected static final int REDSTONE_BUTTON_CLICKED_TEXTURE_X = mio_icif_gui_global_variables.REDSTONE_BUTTON_CLICKED_TEXTURE_X;
    protected static final int REDSTONE_BUTTON_CLICKED_TEXTURE_Y = mio_icif_gui_global_variables.REDSTONE_BUTTON_CLICKED_TEXTURE_Y;
    protected static final int REDSTONE_BUTTON_CLICKED_TEXTURE_WIDTH = mio_icif_gui_global_variables.REDSTONE_BUTTON_CLICKED_TEXTURE_U;
    protected static final int REDSTONE_BUTTON_CLICKED_TEXTURE_HEIGHT = mio_icif_gui_global_variables.REDSTONE_BUTTON_CLICKED_TEXTURE_V;

    public mio_icif_screen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 子类可以通过重写 renderCustomTooltips 来添加自定义悬浮提示
        renderCustomTooltips(guiGraphics, mouseX, mouseY);
        // 最后渲染物品栏的默认 tooltip
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * 渲染自定义悬浮提示，子类可以重写此方法添加机器特有的 tooltip
     * 在绝对屏幕坐标系中渲染
     */
    protected void renderCustomTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 默认空实现，子类重写
    }

    // 绘制标准能量条 - 储电盒 - 发电机
    protected void drawEnergyBar(GuiGraphics guiGraphics, int x, int y, int progressPixels) {
        // 先渲染背景
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) ENERGY_BAR_BG_TEXTURE_X, (float) ENERGY_BAR_BG_TEXTURE_Y,
            ENERGY_BAR_BG_WIDTH, ENERGY_BAR_BG_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
        
        // 再渲染能量条，相对背景向右移动2像素
        if (progressPixels <= 0) return;
        int progressToDraw = Math.min(progressPixels, ENERGY_BAR_WIDTH);
        guiGraphics.blit(ATLAS_TEXTURE, x + 2, y, 0,
            (float) ENERGY_BAR_TEXTURE_X, (float) ENERGY_BAR_TEXTURE_Y,
            progressToDraw, ENERGY_BAR_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    // 绘制动能发电机EU能量条 - 带背景
    protected void drawKineticEnergyBar(GuiGraphics guiGraphics, int x, int y, int progressPixels) {
        // 先渲染背景
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) KINETIC_ENERGY_BAR_TEXTURE_X, (float) KINETIC_ENERGY_BAR_TEXTURE_Y,
            KINETIC_ENERGY_BAR_WIDTH, KINETIC_ENERGY_BAR_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
        
        // 再渲染能量条，相对背景向右移动2像素
        if (progressPixels <= 0) return;
        int progressToDraw = Math.min(progressPixels, KINETIC_ENERGY_BAR2_WIDTH);
        guiGraphics.blit(ATLAS_TEXTURE, x + 2, y, 0,
            (float) KINETIC_ENERGY_BAR2_TEXTURE_X, (float) KINETIC_ENERGY_BAR2_TEXTURE_Y,
            progressToDraw, KINETIC_ENERGY_BAR2_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    // === 进度箭头（先渲染背景，再从左往右填充）===
    protected void drawProgressArrow(GuiGraphics guiGraphics, int x, int y, int progressPixels) {
        // 先渲染背景
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) PROGRESS_BAR_BG_U, (float) PROGRESS_BAR_BG_V,
            PROGRESS_BAR_BG_WIDTH, PROGRESS_BAR_BG_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
        
        if (progressPixels <= 0) return;
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) ARROW_U, (float) ARROW_V,
            progressPixels, ARROW_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    // === 金属成型机专属进度条（先渲染背景，再从左往右填充）===
    protected void drawMetalFormerProgress(GuiGraphics guiGraphics, int x, int y, int progressPixels) {
        // 先渲染背景
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) METAL_FORMER_PROGRESS_BG_U, (float) METAL_FORMER_PROGRESS_BG_V,
            METAL_FORMER_PROGRESS_WIDTH, METAL_FORMER_PROGRESS_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
        
        if (progressPixels <= 0) return;
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) METAL_FORMER_PROGRESS_U, (float) METAL_FORMER_PROGRESS_V,
            progressPixels, METAL_FORMER_PROGRESS_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    // === 压缩机工作进度条（先渲染背景，再从左往右填充）===
    protected void drawCompressorProgress(GuiGraphics guiGraphics, int x, int y, int progressPixels) {
        // 先渲染背景
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) COMPRESSOR_PROGRESS_BG_U, (float) COMPRESSOR_PROGRESS_BG_V,
            COMPRESSOR_PROGRESS_BG_WIDTH, COMPRESSOR_PROGRESS_BG_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
        
        if (progressPixels <= 0) return;
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) COMPRESSOR_PROGRESS_U, (float) COMPRESSOR_PROGRESS_V,
            progressPixels, COMPRESSOR_PROGRESS_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    // === 冷凝机工作进度条（从左往右填充，无背景）===
    protected void drawCondenserProgress(GuiGraphics guiGraphics, int x, int y, int progressPixels) {
        if (progressPixels <= 0) return;
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) CONDENSER_PROGRESS_U, (float) CONDENSER_PROGRESS_V,
            progressPixels, CONDENSER_PROGRESS_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    // === 提取机工作进度条（先渲染背景，再从左往右填充）===
    protected void drawExtractorProgress(GuiGraphics guiGraphics, int x, int y, int progressPixels) {
        // 先渲染背景
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) EXTRACTOR_PROGRESS_BG_U, (float) EXTRACTOR_PROGRESS_BG_V,
            EXTRACTOR_PROGRESS_BG_WIDTH, EXTRACTOR_PROGRESS_BG_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
        
        if (progressPixels <= 0) return;
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) EXTRACTOR_PROGRESS_U, (float) EXTRACTOR_PROGRESS_V,
            progressPixels, EXTRACTOR_PROGRESS_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }
    // === 电动切割机工作进度条（先渲染背景，再从左往右填充）===
    protected void drawCutterProgress(GuiGraphics guiGraphics, int x, int y, int progressPixels) {
        // 先渲染背景
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) CUTTER_PROGRESS_BG_U, (float) CUTTER_PROGRESS_BG_V,
            CUTTER_PROGRESS_BG_WIDTH, CUTTER_PROGRESS_BG_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
        
        if (progressPixels <= 0) return;
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) CUTTER_PROGRESS_U, (float) CUTTER_PROGRESS_V,
            progressPixels, CUTTER_PROGRESS_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    // === 发酵机工作进度条（从左往右填充，无背景）===
    protected void drawFermenterProgress(GuiGraphics guiGraphics, int x, int y, int progressPixels) {
        if (progressPixels <= 0) return;
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) FERMENTER_PROGRESS_U, (float) FERMENTER_PROGRESS_V,
            progressPixels, FERMENTER_PROGRESS_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    // === 发酵机热量条（从左往右填充，无背景）===
    protected void drawFermenterHeat(GuiGraphics guiGraphics, int x, int y, int heatPixels) {
        if (heatPixels <= 0) return;
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) FERMENTER_HEAT_U, (float) FERMENTER_HEAT_V,
            heatPixels, FERMENTER_HEAT_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    // === 标准流体槽（固定渲染背景，流体相对背景偏移(4,4)，刻度相对背景偏移(7,9)且在最顶层）===
    protected void drawFluidTank(GuiGraphics guiGraphics, int x, int y, net.neoforged.neoforge.fluids.FluidStack fluid, int fluidAmount, int fluidCapacity) {
        // 固定渲染背景
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) FLUID_TANK_BG_U, (float) FLUID_TANK_BG_V,
            FLUID_TANK_BG_WIDTH, FLUID_TANK_BG_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
        
        // 当机器存在流体时渲染流体槽与流体
        if (fluidAmount > 0 && fluidCapacity > 0 && fluid != null) {
            // 渲染标准流体槽
            guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
                (float) FLUID_TANK_U, (float) FLUID_TANK_V,
                FLUID_TANK_WIDTH, FLUID_TANK_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
            
            // 渲染流体
            mio_icif_GuiUtils.renderFluidBar(guiGraphics, x + 4, y + 4,
                FLUID_WIDTH, FLUID_HEIGHT,
                fluid, fluidCapacity);
        }
        
        // 刻度在最顶层，避免被流体渲染纹理覆盖
        guiGraphics.blit(ATLAS_TEXTURE, x + 7, y + 9, 0,
            (float) FLUID_TANK_SCALE_U, (float) FLUID_TANK_SCALE_V,
            FLUID_TANK_SCALE_WIDTH, FLUID_TANK_SCALE_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    // === 闪电能量图标（先渲染背景，再从下往上填充）===
    protected void drawLightningEnergy(GuiGraphics guiGraphics, int x, int y, int energy, int maxEnergy) {
        // 先渲染背景
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) LIGHTNING_BG_U, (float) LIGHTNING_BG_V,
            LIGHTNING_BG_WIDTH, LIGHTNING_BG_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
        
        // 再渲染能量图标
        if (energy <= 0 || maxEnergy <= 0) return;
        int energyHeight = (int) ((long) energy * LIGHTNING_HEIGHT / maxEnergy);
        if (energyHeight <= 0) return;
        int drawY = y + LIGHTNING_HEIGHT - energyHeight;
        int textureV = LIGHTNING_V + LIGHTNING_HEIGHT - energyHeight;
        guiGraphics.blit(ATLAS_TEXTURE, x, drawY, 0,
            (float) LIGHTNING_U, (float) textureV,
            LIGHTNING_WIDTH, energyHeight, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    // === 火焰能量图标（先渲染背景，再从下往上填充）===
    protected void drawFlameEnergy(GuiGraphics guiGraphics, int x, int y, int energy, int maxEnergy) {
        // 先渲染背景
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) FLAME_ICON_BG_U, (float) FLAME_ICON_BG_V,
            FLAME_ICON_BG_WIDTH, FLAME_ICON_BG_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
        
        // 再渲染能量图标
        if (energy <= 0 || maxEnergy <= 0) return;
        int energyHeight = (int) ((long) energy * FLAME_ICON_HEIGHT / maxEnergy);
        if (energyHeight <= 0) return;
        int drawY = y + FLAME_ICON_HEIGHT - energyHeight;
        int textureV = FLAME_ICON_V + FLAME_ICON_HEIGHT - energyHeight;
        guiGraphics.blit(ATLAS_TEXTURE, x, drawY, 0,
            (float) FLAME_ICON_U, (float) textureV,
            FLAME_ICON_WIDTH, energyHeight, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    // === 横向能量条（从左往右填充）===
    protected void drawHorizontalEnergyBar(GuiGraphics guiGraphics, int x, int y, int energy, int maxEnergy) {
        if (energy <= 0 || maxEnergy <= 0) return;
        int energyWidth = (int) ((long) energy * ENERGY_BAR2_WIDTH / maxEnergy);
        if (energyWidth <= 0) return;
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) ENERGY_BAR2_U, (float) ENERGY_BAR2_V,
            energyWidth, ENERGY_BAR2_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    // === 动能条（从左往右填充）===
    protected void drawKineticBar(GuiGraphics guiGraphics, int x, int y, int kinetic, int maxKinetic) {
        if (kinetic <= 0 || maxKinetic <= 0) return;
        int kineticWidth = (int) ((long) kinetic * KINETIC_BAR_WIDTH / maxKinetic);
        if (kineticWidth <= 0) return;
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) KINETIC_BAR_U, (float) KINETIC_BAR_V,
            kineticWidth, KINETIC_BAR_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    // === 动能条（从左往右填充，使用已计算的像素值）===
    protected void drawKineticBar(GuiGraphics guiGraphics, int x, int y, int progressPixels) {
        if (progressPixels <= 0) return;
        int progressToDraw = Math.min(progressPixels, KINETIC_BAR_WIDTH);
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) KINETIC_BAR_U, (float) KINETIC_BAR_V,
            progressToDraw, KINETIC_BAR_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    // === 工作状态图标 ===
    protected void drawWorkStatusIcon(GuiGraphics guiGraphics, int x, int y, boolean isActive) {
        if (isActive) {
            guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
                (float) WORK_ACTIVE_U, (float) WORK_ACTIVE_V,
                WORK_ICON_WIDTH, WORK_ICON_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
        } else {
            guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
                (float) WORK_INACTIVE_U, (float) WORK_INACTIVE_V,
                WORK_ICON_WIDTH, WORK_ICON_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
        }
    }

 // === 高热量进度条（从左往右填充）===
    protected void drawHeatBar(GuiGraphics guiGraphics, int x, int y, int heat, int maxHeat) {
        if (heat <= 0 || maxHeat <= 0) return;
        int heatPixels = (int) ((long) heat * HEAT_BAR_WIDTH / maxHeat);
        if (heatPixels <= 0) return;
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) HEAT_BAR_U, (float) HEAT_BAR_V,
            heatPixels, HEAT_BAR_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

 // === 高热量允许标志 ===
    protected void drawHeatOkIcon(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) HEAT_OK_U, (float) HEAT_OK_V,
            HEAT_OK_WIDTH, HEAT_OK_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

 // === 高压缩空气允许标志 ===
    protected void drawAirOkIcon(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) AIR_OK_U, (float) AIR_OK_V,
            AIR_OK_WIDTH, AIR_OK_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

 // === 高冶炼进度条（从下往上填充，无背景，背景在GUI纹理中）===
    protected void drawBlastProgress(GuiGraphics guiGraphics, int x, int y, int progress, int maxProgress) {
        if (progress <= 0 || maxProgress <= 0) return;
        int progressPixels = (int) ((long) progress * BLAST_PROGRESS_HEIGHT / maxProgress);
        if (progressPixels <= 0) return;
        // 从下往上填充
        int drawY = y + BLAST_PROGRESS_HEIGHT - progressPixels;
        int srcY = BLAST_PROGRESS_V + BLAST_PROGRESS_HEIGHT - progressPixels;
        guiGraphics.blit(ATLAS_TEXTURE, x, drawY, 0,
            (float) BLAST_PROGRESS_U, (float) srcY,
            BLAST_PROGRESS_WIDTH, progressPixels, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    // === 热能离心机进度条（从下往上填充，无背景，背景在GUI纹理中）===
    protected void drawCentrifugeProgress(GuiGraphics guiGraphics, int x, int y, int progress, int maxProgress) {
        if (progress <= 0 || maxProgress <= 0) return;
        int progressPixels = (int) ((long) progress * HEAT_ENERGY_CENTRIFUGE_HEIGHT / maxProgress);
        if (progressPixels <= 0) return;
        // 从下往上填充
        int drawY = y + HEAT_ENERGY_CENTRIFUGE_HEIGHT - progressPixels;
        int srcY = HEAT_ENERGY_CENTRIFUGE_V + HEAT_ENERGY_CENTRIFUGE_HEIGHT - progressPixels;
        guiGraphics.blit(ATLAS_TEXTURE, x, drawY, 0,
            (float) HEAT_ENERGY_CENTRIFUGE_U, (float) srcY,
            HEAT_ENERGY_CENTRIFUGE_WIDTH, progressPixels, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    // === 悬浮提示工具方法 ===
    protected boolean isHovering(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    protected void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, int energy, int maxEnergy) {
        guiGraphics.renderTooltip(this.font,
            Component.literal(energy + "/" + maxEnergy + " EU"),
            mouseX, mouseY);
    }

    protected void renderProgressTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, int progress, int maxProgress) {
        guiGraphics.renderTooltip(this.font,
            Component.literal(progress + "/" + maxProgress),
            mouseX, mouseY);
    }
    
    // === 红石模式按钮渲染 ===
    /**
     * 绘制红石模式按钮
     * @param guiGraphics GUI图形上下文
     * @param x 按钮X坐标（相对于GUI左上角）
     * @param y 按钮Y坐标（相对于GUI左上角）
     * @param isPressed 是否被按下（鼠标按下状态）
     */
    protected void drawRedstoneButton(GuiGraphics guiGraphics, int x, int y, boolean isPressed) {
        if (isPressed) {
            // 绘制被点击的纹理
            guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
                (float) REDSTONE_BUTTON_CLICKED_TEXTURE_X, (float) REDSTONE_BUTTON_CLICKED_TEXTURE_Y,
                REDSTONE_BUTTON_CLICKED_TEXTURE_WIDTH, REDSTONE_BUTTON_CLICKED_TEXTURE_HEIGHT, 
                ATLAS_WIDTH, ATLAS_HEIGHT);
        } else {
            // 绘制未被点击的纹理
            guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
                (float) REDSTONE_BUTTON_TEXTURE_X, (float) REDSTONE_BUTTON_TEXTURE_Y,
                REDSTONE_BUTTON_TEXTURE_WIDTH, REDSTONE_BUTTON_TEXTURE_HEIGHT, 
                ATLAS_WIDTH, ATLAS_HEIGHT);
        }
    }
    
    /**
     * 检查鼠标是否悬停在红石按钮上
     * @param mouseX 鼠标X坐标（屏幕坐标）
     * @param mouseY 鼠标Y坐标（屏幕坐标）
     * @param buttonX 按钮X坐标（GUI坐标）
     * @param buttonY 按钮Y坐标（GUI坐标）
     * @return 是否悬停
     */
    protected boolean isHoveringRedstoneButton(int mouseX, int mouseY, int buttonX, int buttonY) {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;
        int actualButtonX = guiLeft + buttonX;
        int actualButtonY = guiTop + buttonY;
        return mouseX >= actualButtonX && mouseX < actualButtonX + REDSTONE_BUTTON_TEXTURE_WIDTH &&
               mouseY >= actualButtonY && mouseY < actualButtonY + REDSTONE_BUTTON_TEXTURE_HEIGHT;
    }
    
    /**
     * 渲染红石模式的工具提示
     * @param guiGraphics GUI图形上下文
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     * @param redstoneMode 当前红石模式（0-7）
     */
    protected void renderRedstoneModeTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, int redstoneMode) {
        String translationKey = switch (redstoneMode) {
            case 0 -> "mode.redstone.none";
            case 1 -> "mode.redstone.full";
            case 2 -> "mode.redstone.partial";
            case 3 -> "mode.redstone.not_full";
            case 4 -> "mode.redstone.empty";
            case 5 -> "mode.redstone.inverted";
            case 6 -> "mode.redstone.conditional";
            case 7 -> "mode.redstone.redstone_on";
            default -> "mode.redstone.unknown";
        };
        guiGraphics.renderTooltip(this.font, Component.translatable(translationKey), mouseX, mouseY);
    }

    public static class AtlasComponent {
        public final int u;
        public final int v;
        public final int w;
        public final int h;

        public AtlasComponent(int u, int v, int w, int h) {
            this.u = u;
            this.v = v;
            this.w = w;
            this.h = h;
        }
    }

    // === 绘制滑块（用于存储箱等可滚动GUI）===
    protected void drawScrollbar(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) SCROLLBAR_TEXTURE_X, (float) SCROLLBAR_TEXTURE_Y,
            SCROLLBAR_TEXTURE_WIDTH, SCROLLBAR_TEXTURE_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }
}