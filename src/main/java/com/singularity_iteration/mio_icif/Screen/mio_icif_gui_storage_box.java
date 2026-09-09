package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Storage.StorageBoxMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_storage_box extends mio_icif_screen<StorageBoxMenu> {

    private static final ResourceLocation CHEST_GUI_TEXTURE =
            ResourceLocation.parse("minecraft:textures/gui/container/generic_54.png");

    // 滑块尺寸
    private static final int SCROLLBAR_THUMB_HEIGHT = 15; // 滑块贴图高度

    private static final int HEADER_HEIGHT = 17;
    private static final int ROW_HEIGHT = 18;
    private static final int PLAYER_INV_HEIGHT = 96;
    private static final int MAX_VISIBLE_ROWS = 6;
    private static final int COLS = 9;

    private final int totalRows;
    private final int visibleRows;
    private final boolean canScroll;
    private float scrollOffset;
    private boolean draggingScrollbar;

    public mio_icif_gui_storage_box(StorageBoxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.totalRows = menu.getTotalRows();
        this.visibleRows = Math.min(this.totalRows, MAX_VISIBLE_ROWS);
        this.canScroll = this.totalRows > MAX_VISIBLE_ROWS;
        this.scrollOffset = 0.0f;
        this.imageWidth = COLS * 18 + 14;
        this.imageHeight = 114 + this.visibleRows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
        this.inventoryLabelX = 8;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private int getMaxScroll() {
        return Math.max(0, this.totalRows - MAX_VISIBLE_ROWS);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.canScroll) {
            int maxScroll = this.getMaxScroll();
            this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset - (float) verticalAmount));
            this.menu.updateSlotPositions((int) this.scrollOffset);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int textureHeight = this.visibleRows * ROW_HEIGHT + HEADER_HEIGHT;
        guiGraphics.blit(CHEST_GUI_TEXTURE, x, y, 0, 0, this.imageWidth, textureHeight);
        guiGraphics.blit(CHEST_GUI_TEXTURE, x, y + textureHeight, 0, 126, this.imageWidth, PLAYER_INV_HEIGHT);

        if (this.canScroll) {
            renderScrollbar(guiGraphics, x, y);
        }
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int x, int y) {
        int scrollbarX = x + this.imageWidth + 4;
        int scrollbarTop = y + HEADER_HEIGHT;
        int scrollbarHeight = this.visibleRows * ROW_HEIGHT;
        int maxScroll = this.getMaxScroll();

        // 只在有滚动需求时绘制滑块
        if (maxScroll > 0) {
            // 计算滑块位置（在轨道范围内上下滑动）
            float scrollRatio = this.scrollOffset / maxScroll;
            int thumbY = scrollbarTop + (int) ((scrollbarHeight - SCROLLBAR_THUMB_HEIGHT) * scrollRatio);
            
            // 使用基类方法绘制滑块（从组件图集获取）
            drawScrollbar(guiGraphics, scrollbarX, thumbY);
        }
    }



    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            guiGraphics.renderTooltip(this.font, this.hoveredSlot.getItem(), x, y);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        if (this.canScroll) {
            // 计算滑块当前位置
            int scrollbarX = guiLeft + this.imageWidth + 4;
            int scrollbarTop = guiTop + HEADER_HEIGHT;
            int scrollbarHeight = this.visibleRows * ROW_HEIGHT;
            int maxScroll = this.getMaxScroll();
            float scrollRatio = this.scrollOffset / maxScroll;
            int thumbY = scrollbarTop + (int) ((scrollbarHeight - SCROLLBAR_THUMB_HEIGHT) * scrollRatio);
            
            // 检测是否在滑块上
            if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_TEXTURE_WIDTH && 
                mouseY >= thumbY && mouseY < thumbY + SCROLLBAR_THUMB_HEIGHT) {
                return false;
            }
        }
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.canScroll && button == 0) {
            int scrollbarX = this.leftPos + this.imageWidth + 4;
            int scrollbarTop = this.topPos + HEADER_HEIGHT;
            int scrollbarHeight = this.visibleRows * ROW_HEIGHT;
            int maxScroll = this.getMaxScroll();
            float scrollRatio = this.scrollOffset / maxScroll;
            int thumbY = scrollbarTop + (int) ((scrollbarHeight - SCROLLBAR_THUMB_HEIGHT) * scrollRatio);

            // 检测是否点击在滑块上
            if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_TEXTURE_WIDTH &&
                mouseY >= thumbY && mouseY < thumbY + SCROLLBAR_THUMB_HEIGHT) {
                this.draggingScrollbar = true;
                return true;
            }

            // 检测是否点击在滑块轨道区域（用于直接跳转）
            if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_TEXTURE_WIDTH &&
                mouseY >= scrollbarTop && mouseY < scrollbarTop + scrollbarHeight) {
                if (maxScroll > 0) {
                    float clickY = (float) (mouseY - scrollbarTop) / (scrollbarHeight - SCROLLBAR_THUMB_HEIGHT);
                    this.scrollOffset = Math.max(0, Math.min(maxScroll, (int) (clickY * maxScroll)));
                    this.menu.updateSlotPositions((int) this.scrollOffset);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingScrollbar && this.canScroll && button == 0) {
            int scrollbarTop = this.topPos + HEADER_HEIGHT;
            int scrollbarHeight = this.visibleRows * ROW_HEIGHT;
            int maxScroll = this.getMaxScroll();

            if (maxScroll > 0) {
                // 根据鼠标Y位置计算滚动偏移（与原版创造模式物品栏对齐）
                float dragYRelative = (float) (mouseY - scrollbarTop);
                float scrollRatio = dragYRelative / (scrollbarHeight - SCROLLBAR_THUMB_HEIGHT);
                this.scrollOffset = Math.max(0, Math.min(maxScroll, scrollRatio * maxScroll));
                this.menu.updateSlotPositions((int) this.scrollOffset);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.draggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}