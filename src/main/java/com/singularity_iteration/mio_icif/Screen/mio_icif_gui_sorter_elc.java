package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_sorter_elc;
import com.singularity_iteration.mio_icif.Menu.Producer.SorterElcMenu;
import com.singularity_iteration.mio_icif.network.SorterDefaultDirPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_sorter_elc extends mio_icif_screen<SorterElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_sorter_elc.png");

    // 标准闪电标志位置
    private static final int LIGHTNING_X = 171;
    private static final int LIGHTNING_Y = 219;

    private static final int DIR_BUTTON_X = 42;
    private static final int DIR_BUTTON_Y_START = 18;
    private static final int DIR_BUTTON_SIZE = 18;
    private static final int DIR_BUTTON_SPACING = 20;

    private static final int ADJ_INDICATOR_X = 60;
    private static final int ADJ_INDICATOR_Y_START = 18;
    private static final int ADJ_INDICATOR_SIZE = 18;
    private static final int ADJ_INDICATOR_SPACING = 20;

    @SuppressWarnings("unused")
    private static final int BUTTON_TEXTURE_X = 230;
    private static final int BUTTON_VS_NORMAL = 15;
    private static final int BUTTON_VS_DEFAULT = 33;
    private static final int INDICATOR_VS_HAS_INV = 15;
    private static final int INDICATOR_VS_NO_INV = 33;
    @SuppressWarnings("unused")
    private static final int INDICATOR_TEXTURE_X = 212;

    private static final Direction[] DIRECTIONS = Direction.values();

    private static final Component DEFAULT_TOOLTIP = Component.translatable("gui.mio_icif.sorter.default_output");
    private static final Component WHITELIST_TOOLTIP = Component.translatable("gui.mio_icif.sorter.whitelist");

    public mio_icif_gui_sorter_elc(SorterElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 212;
        this.imageHeight = 243;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        SorterElcMenu menu = this.getMenu();
        if (menu != null) {
            // 使用基类标准方法绘制闪电能量标志
            drawLightningEnergy(guiGraphics, x + LIGHTNING_X, y + LIGHTNING_Y, menu.getEnergy(), menu.getMaxEnergy());

            int defaultDir = menu.getDefaultOutputDirection();

            for (int dir = 0; dir < 6; dir++) {
                int btnY = y + DIR_BUTTON_Y_START + dir * DIR_BUTTON_SPACING;
                int vs = (dir == defaultDir) ? BUTTON_VS_DEFAULT : BUTTON_VS_NORMAL;
guiGraphics.blit(ATLAS_TEXTURE, x + DIR_BUTTON_X, btnY, 0, (float) vs == BUTTON_VS_DEFAULT ? 104 : 84, (float) 164, DIR_BUTTON_SIZE, DIR_BUTTON_SIZE, ATLAS_WIDTH, ATLAS_HEIGHT);

                int indY = y + ADJ_INDICATOR_Y_START + dir * ADJ_INDICATOR_SPACING;
                boolean hasInv = false;
                if (menu.getBlockEntity() instanceof mio_icif_sorter_elc sorter) {
                    hasInv = sorter.hasAdjacentInventory(DIRECTIONS[dir]);
                } else if (menu.getBlockPos() != null && minecraft != null && minecraft.level != null) {
                    net.minecraft.world.level.block.entity.BlockEntity be = minecraft.level.getBlockEntity(menu.getBlockPos());
                    if (be instanceof mio_icif_sorter_elc sorter) {
                        hasInv = sorter.hasAdjacentInventory(DIRECTIONS[dir]);
                    }
                }
                int indVs = hasInv ? INDICATOR_VS_HAS_INV : INDICATOR_VS_NO_INV;
guiGraphics.blit(ATLAS_TEXTURE, x + ADJ_INDICATOR_X, indY, 0, (float) indVs == INDICATOR_VS_HAS_INV ? 124 : 144, (float) 164, ADJ_INDICATOR_SIZE, ADJ_INDICATOR_SIZE, ATLAS_WIDTH, ATLAS_HEIGHT);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // 不绘制玩家物品栏标题
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        for (int dir = 0; dir < 6; dir++) {
            int btnX = x + DIR_BUTTON_X;
            int btnY = y + DIR_BUTTON_Y_START + dir * DIR_BUTTON_SPACING;
            if (mouseX >= btnX && mouseX < btnX + DIR_BUTTON_SIZE &&
                mouseY >= btnY && mouseY < btnY + DIR_BUTTON_SIZE) {
                if (button == 0) {
                    SorterElcMenu menu = this.getMenu();
                    if (menu != null && menu.getBlockPos() != null) {
                        PacketDistributor.sendToServer(new SorterDefaultDirPacket(
                            menu.getBlockPos(), dir));
                    }
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 闪电标志 tooltip
        if (mouseX >= x + LIGHTNING_X && mouseX <= x + LIGHTNING_X + LIGHTNING_WIDTH &&
            mouseY >= y + LIGHTNING_Y && mouseY <= y + LIGHTNING_Y + LIGHTNING_HEIGHT) {
            SorterElcMenu menu = this.getMenu();
            if (menu != null) {
                guiGraphics.renderTooltip(this.font,
                    Component.literal("Energy: " + menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                    mouseX, mouseY);
            }
            return;
        }

        for (int dir = 0; dir < 6; dir++) {
            int btnX = x + DIR_BUTTON_X;
            int btnY = y + DIR_BUTTON_Y_START + dir * DIR_BUTTON_SPACING;
            if (mouseX >= btnX && mouseX < btnX + DIR_BUTTON_SIZE &&
                mouseY >= btnY && mouseY < btnY + DIR_BUTTON_SIZE) {
                SorterElcMenu menu = this.getMenu();
                if (menu != null) {
                    int defaultDir = menu.getDefaultOutputDirection();
                    if (dir == defaultDir) {
                        guiGraphics.renderTooltip(this.font, DEFAULT_TOOLTIP, mouseX, mouseY);
                    } else {
                        guiGraphics.renderTooltip(this.font, WHITELIST_TOOLTIP, mouseX, mouseY);
                    }
                }
                return;
            }
        }
    }
}