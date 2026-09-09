package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;


/**
 * 火力发电机的GUI类
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_Thermal_Generator extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Generator.ThermalGeneratorMenu> {
    /* private static final Logger LOGGER = LogUtils.getLogger(); */
    
    private static final ResourceLocation GUI_TEXTURE = 
        ResourceLocation.parse("mio_icif:textures/gui/gui_generator.png");

    // 火力发电机方块实体引用
    /* private final mio_icif_Thermal_Generator blockEntity; */
    
    public mio_icif_gui_Thermal_Generator(com.singularity_iteration.mio_icif.Menu.Generator.ThermalGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        /* this.blockEntity = menu.getBlockEntity(); */
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // 绘制背景
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        
        com.singularity_iteration.mio_icif.Menu.Generator.ThermalGeneratorMenu menu = this.menu;
        if (menu != null) {
            drawFlameEnergy(guiGraphics, x + 65, y + 36, menu.getBurnTime(), menu.getBurnDuration());
        }
        
        // 绘制能量条填充（当方块有能量时显示）- 从左往右填充
        if (menu != null) {
            int energyProgressPixels = menu.getEnergyProgressPixels();
            
            drawEnergyBar(guiGraphics, x + 94, y + 35, energyProgressPixels);
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        
        // 绘制标题
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        
        // 绘制物品栏标题
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        
        // 绘制能量信息提示（当鼠标悬停在能量条上时）
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // 检查鼠标是否在能量条区域（x + 94, y + 35, 宽度25, 高度17） 横向矩形
        if (mouseX >= x + 94 && mouseX <= x + 94 + 25 && mouseY >= y + 35 && mouseY <= y + 35 + 17) {
            com.singularity_iteration.mio_icif.Menu.Generator.ThermalGeneratorMenu menu = this.menu;
            if (menu != null) {
                int energy = menu.getEnergy();
                int maxEnergy = menu.getMaxEnergy();
                guiGraphics.renderTooltip(this.font, 
                    Component.literal(energy + "/" + maxEnergy + " EU"), 
                    mouseX - x, mouseY - y);
            }
        }
    }
}