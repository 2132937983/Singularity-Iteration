package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.util.mio_icif_gui_global_variables;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import java.util.ArrayList;
import java.util.List;

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
    protected static final int LIGHTNING_BG_TEXTURE_X = mio_icif_gui_global_variables.LIGHTNING_BG_TEXTURE_X;
    protected static final int LIGHTNING_BG_TEXTURE_Y = mio_icif_gui_global_variables.LIGHTNING_BG_TEXTURE_Y;

    protected static final int LIGHTNING_WIDTH = mio_icif_gui_global_variables.LIGHTNING_WIDTH;
    protected static final int LIGHTNING_HEIGHT = mio_icif_gui_global_variables.LIGHTNING_HEIGHT;
    protected static final int LIGHTNING_TEXTURE_X = mio_icif_gui_global_variables.LIGHTNING_TEXTURE_X;
    protected static final int LIGHTNING_TEXTURE_Y = mio_icif_gui_global_variables.LIGHTNING_TEXTURE_Y;

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
        dockButtons.clear();
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawTitle(guiGraphics);
    }

    /** Left edge (GUI-relative) and scale of the title so it never overflows the panel. */
    private float[] titleLayout() {
        int margin = hasUpgradeHint() ? 18 : 6;
        // slots sharing the title row on the right (e.g. an upgrade column starting at y=8) end the title early
        int right = this.imageWidth - 6;
        for (var slot : this.menu.slots) {
            if (slot.x > this.imageWidth / 2 && slot.y <= this.titleLabelY + 8 && slot.y + 17 >= this.titleLabelY) right = Math.min(right, slot.x - 3);
        }
        int avail = right - margin - 10;                           // leave room for the status lamp
        int w = this.font.width(this.title);
        if (this.titleLabelX >= margin + 9 && this.titleLabelX + w <= right) {
            return new float[]{this.titleLabelX, 1F};
        }
        float scale = w > avail ? Math.max(0.5F, (float) avail / w) : 1F;
        return new float[]{Math.max(margin + 10, (margin + right - w * scale) / 2F + 5F), scale};
    }

    private boolean hasUpgradeHint() {
        return this.menu instanceof com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu machine
                && machine.hasUpgradeHint() && showUpgradeHint();
    }

    /** Draws the GUI title in the theme text colour, scaled down if it would overflow. */
    protected void drawTitle(GuiGraphics guiGraphics) {
        float[] l = titleLayout();
        if (l[1] >= 1F) {
            guiGraphics.drawString(this.font, this.title, (int) l[0], this.titleLabelY, SiGuiTheme.TEXT, false);
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(l[0], this.titleLabelY + (8 - 8 * l[1]) / 2F, 0);
        guiGraphics.pose().scale(l[1], l[1], 1F);
        guiGraphics.drawString(this.font, this.title, 0, 0, SiGuiTheme.TEXT, false);
        guiGraphics.pose().popPose();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);   // blur + renderBg
        renderStatusLamp(guiGraphics);
        renderDock(guiGraphics, mouseX, mouseY);
    }

    // ================================================================ left utility dock
    /*
     * Machine-specific controls (upgrade slots, work-area toggle, XP collector, ...) sit in a
     * slim dock outside the panel's LEFT edge. Inventory-sorting mods (Inventory Profiles Next,
     * Mouse Tweaks, ...) put their buttons around the panel's upper-right corner, which is
     * therefore kept completely clear.
     */
    public interface DockIcon { void draw(GuiGraphics g, int x, int y, boolean hovered); }

    public record DockButton(DockIcon icon, java.util.function.Supplier<List<Component>> tooltip, Runnable onClick) { }

    private final List<DockButton> dockButtons = new ArrayList<>();
    public static final int DOCK_W = 24, DOCK_BUTTON = 18;

    /** Adds a utility button to the dock (call from init(), or from a ScreenEvent.Init.Post listener). */
    public void addDockButton(DockButton button) { dockButtons.add(button); }

    private int dockSlots() {
        return this.menu instanceof com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu m ? m.dockedUpgradeSlots() : 0;
    }

    public boolean hasDock() { return dockSlots() > 0 || !dockButtons.isEmpty(); }

    /** Dock bounds in screen coordinates: x, y, w, h (w = 0 when there is no dock). */
    public int[] dockBounds() {
        if (!hasDock()) return new int[]{leftPos, topPos, 0, 0};
        int slots = dockSlots();
        int h = 4 + slots * 18 + (slots > 0 && !dockButtons.isEmpty() ? 4 : 0) + dockButtons.size() * (DOCK_BUTTON + 2) + 2;
        return new int[]{leftPos - DOCK_W - 1, topPos + 3, DOCK_W, h};
    }

    private int dockButtonY(int i) {
        int slots = dockSlots();
        return topPos + 7 + slots * 18 + (slots > 0 ? 4 : 0) + i * (DOCK_BUTTON + 2);
    }

    private int dockButtonAt(double mx, double my) {
        int bx = leftPos - DOCK_W + 2;
        for (int i = 0; i < dockButtons.size(); i++) {
            int by = dockButtonY(i);
            if (mx >= bx && mx < bx + DOCK_BUTTON && my >= by && my < by + DOCK_BUTTON) return i;
        }
        return -1;
    }

    private void renderDock(GuiGraphics g, int mouseX, int mouseY) {
        if (!hasDock()) return;
        int[] b = dockBounds();
        int x = b[0], y = b[1], w = b[2], h = b[3];
        // slim tab hugging the panel: outline, bevel, light face, cyan index stripe on the outer edge
        g.fill(x, y, x + w + 2, y + h, SiGuiTheme.OUTLINE);
        g.fill(x + 1, y + 1, x + w + 1, y + h - 1, SiGuiTheme.BEVEL_HI);
        g.fillGradient(x + 2, y + 2, x + w + 1, y + h - 1, SiGuiTheme.PANEL_TOP, SiGuiTheme.PANEL_BOTTOM);
        g.fill(x + 1, y + 3, x + 2, y + h - 3, 0xFF5FD3F5);
        int slots = dockSlots();
        for (int i = 0; i < slots; i++) {
            SiGuiTheme.slot(g, leftPos + com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu.DOCK_SLOT_X - 1,
                topPos + com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu.DOCK_SLOT_Y - 1 + i * 18);
        }
        if (slots > 0 && !dockButtons.isEmpty()) SiGuiTheme.groove(g, x + 4, topPos + 7 + slots * 18 + 1, w - 6);
        int hovered = dockButtonAt(mouseX, mouseY);
        int bx = leftPos - DOCK_W + 2;
        for (int i = 0; i < dockButtons.size(); i++) {
            int by = dockButtonY(i);
            boolean hov = i == hovered;
            g.fill(bx, by, bx + DOCK_BUTTON, by + DOCK_BUTTON, SiGuiTheme.OUTLINE);
            g.fillGradient(bx + 1, by + 1, bx + DOCK_BUTTON - 1, by + DOCK_BUTTON - 1,
                hov ? 0xFFF3F8FC : 0xFFE6E9EC, hov ? 0xFFC9DCEE : 0xFFC4C8CD);
            if (hov) g.renderOutline(bx, by, DOCK_BUTTON, DOCK_BUTTON, SiGuiTheme.ACCENT);
            dockButtons.get(i).icon().draw(g, bx + 1, by + 1, hov);
        }
    }

    private void renderDockTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int i = dockButtonAt(mouseX, mouseY);
        if (i >= 0) g.renderComponentTooltip(this.font, dockButtons.get(i).tooltip().get(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int i = button == 0 ? dockButtonAt(mouseX, mouseY) : -1;
        if (i >= 0) {
            dockButtons.get(i).onClick().run();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        int[] b = dockBounds();
        if (b[2] > 0 && mouseX >= b[0] && mouseX < b[0] + b[2] + 2 && mouseY >= b[1] && mouseY < b[1] + b[3]) return false;
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton);
    }

    /** UI click sound used by dock buttons. */
    protected void playClick() {
        if (this.minecraft == null) return;
        this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
            com.miophas.singularity_iteration.common.registry.mio_icif_sounds.UI_CLICK.get(), 1.0F, 0.6F));
    }

    /** XP collector as a dock button: orb icon (green when at least 1 XP is banked) + tooltip. */
    protected void addXpDockButton(java.util.function.IntSupplier xpTenths) {
        addDockButton(new DockButton((g, x, y, hov) -> {
            boolean ready = xpTenths.getAsInt() >= 10;
            int cx = x + 8, cy = y + 7;
            int rim = ready ? 0xFF4E8C1E : 0xFF8C9298, core = ready ? 0xFFB6F25A : 0xFFC0C4C8;
            g.fill(cx - 3, cy - 4, cx + 4, cy + 5, rim);
            g.fill(cx - 4, cy - 3, cx + 5, cy + 4, rim);
            g.fill(cx - 3, cy - 3, cx + 4, cy + 4, core);
            g.fill(cx - 1, cy - 2, cx + 1, cy, 0xFFFFFFFF);
            String n = xpTenths.getAsInt() >= 100 ? String.valueOf(xpTenths.getAsInt() / 10) : String.format("%.1f", xpTenths.getAsInt() / 10F);
            DspUi.small(g, this.font, n, x + 15 - DspUi.smallWidth(this.font, n), y + 11, ready ? 0xFF2D5A12 : 0xFF6B737D);
        }, () -> java.util.List.of(
            Component.translatable("gui.mio_icif.xp.title", String.format("%.1f", xpTenths.getAsInt() / 10F)),
            Component.translatable("gui.mio_icif.xp.tip").withStyle(net.minecraft.ChatFormatting.GRAY)), this::clickXpButton));
    }

    /**
     * Theme chrome shared by every machine GUI: a small round status lamp left of the
     * title (green = running, amber = no energy, grey = idle). Drawn after the
     * background and before slots/labels, so subclass layouts are untouched.
     */
    private void renderStatusLamp(GuiGraphics guiGraphics) {
        int color = statusLampColor();
        if (color == 0) return;
        float[] l = titleLayout();
        int lx = leftPos + (int) l[0] - 8;
        int ly = topPos + titleLabelY + 1;
        if ((int) l[0] - 8 < (hasUpgradeHint() ? 17 : 4)) return;   // keep clear of the upgrade hint at (5,5)
        SiGuiTheme.led(guiGraphics, lx, ly, color);
    }

    /** ARGB lamp colour for the machine state, or 0 to hide the lamp. */
    protected int statusLampColor() {
        Object be = null;
        Boolean working = null;
        int energy = -1, maxEnergy = -1;
        if (this.menu instanceof com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu m) {
            be = m.getBlockEntity();
            if (m.getData() != null && m.getData().getCount() > 4) {
                working = m.isWorking(); energy = m.getEnergy(); maxEnergy = m.getMaxEnergy();
            }
        } else if (this.menu instanceof com.miophas.singularity_iteration.common.menu.base.mio_icif_generator_menu g) {
            be = g.getBlockEntity();
            if (g.getData() != null && g.getData().getCount() > 3) working = g.isGenerating();
        }
        Boolean active = null;
        if (be instanceof net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
            net.minecraft.world.level.block.state.BlockState state = blockEntity.getBlockState();
            if (com.miophas.singularity_iteration.common.client.machine.MachineRunState.hasRunState(state)) {
                active = com.miophas.singularity_iteration.common.client.machine.MachineRunState.isRunning(state);
            }
        }
        if (active == null && working == null) return 0;
        if (Boolean.TRUE.equals(active) || (active == null && Boolean.TRUE.equals(working))) return SiGuiTheme.GOOD;
        if (maxEnergy > 0 && energy == 0) return SiGuiTheme.WARN;
        return SiGuiTheme.LED_OFF;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderUpgradeHint(guiGraphics, mouseX, mouseY);
        // 子类可以通过重写 renderCustomTooltips 来添加自定义悬浮提示
        renderCustomTooltips(guiGraphics, mouseX, mouseY);
        // 最后渲染物品栏的默认 tooltip
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        // 空格子的升级槽默认提示该机器支持的升级（放在最后以覆盖在物品 tooltip 之上）
        renderUpgradeSlotTooltip(guiGraphics, mouseX, mouseY);
        renderDockTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * Vanilla-like compact upgrade marker.  It stays in the GUI's upper-left
     * corner and only appears for menus with a real upgrade-slot range.
     */
    /** Screens with their own upgrade panel can hide the corner marker. */
    protected boolean showUpgradeHint() { return true; }

    private void renderUpgradeHint(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!(this.menu instanceof com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu machine)
                || !machine.hasUpgradeHint() || !showUpgradeHint()) return;

        final int hintX = leftPos + 5;
        final int hintY = topPos + 5;
        final int hintSize = 10;
        boolean hovered = mouseX >= hintX && mouseX < hintX + hintSize
                && mouseY >= hintY && mouseY < hintY + hintSize;
        guiGraphics.fill(hintX, hintY, hintX + hintSize, hintY + hintSize,
                hovered ? 0xffdde6f0 : 0xffeef0f1);
        guiGraphics.renderOutline(hintX, hintY, hintSize, hintSize,
                hovered ? 0xff3a6ea5 : 0xff8a9096);
        guiGraphics.drawString(this.font, Component.literal("i"), hintX + 4, hintY + 1,
                hovered ? 0xff3a6ea5 : 0xff2a2e33, false);

        if (!hovered) return;
        guiGraphics.renderTooltip(this.font, buildUpgradeHintTooltip(machine),
                java.util.Optional.empty(), mouseX, mouseY);
    }

    /**
     * Default tooltip for an empty upgrade slot: hovering the slot itself lists
     * the upgrades this machine accepts.  Occupied slots keep the vanilla item
     * tooltip so the installed upgrade's own description is not shadowed.
     */
    private void renderUpgradeSlotTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!(this.menu instanceof com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu machine)) return;
        if (this.hoveredSlot == null || this.hoveredSlot.hasItem()) return;
        if (!machine.isUpgradeSlot(this.hoveredSlot.index)) return;

        guiGraphics.renderTooltip(this.font, buildUpgradeHintTooltip(machine),
                java.util.Optional.empty(), mouseX, mouseY);
    }

    /**
     * Builds the shared "compatible upgrades" tooltip used by both the corner
     * hint icon and the upgrade-slot hover tooltip.
     */
    private static List<Component> buildUpgradeHintTooltip(
            com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu machine) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("gui.mio_icif.upgrades.compatible")
                .withStyle(ChatFormatting.AQUA));
        List<net.minecraft.world.item.ItemStack> compatible = machine.getCompatibleUpgradeHints();
        if (compatible.isEmpty()) {
            tooltip.add(Component.translatable("gui.mio_icif.upgrades.none")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            for (net.minecraft.world.item.ItemStack stack : compatible) {
                tooltip.add(Component.literal(" • ").append(stack.getHoverName())
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        return tooltip;
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

    // === banked smelting XP: readout + "collect" button (points go straight into the XP bar) ===
    protected static final int XP_BUTTON_W = 48, XP_BUTTON_H = 12;

    protected void drawXpButton(GuiGraphics g, int x, int y, int xpTenths, boolean hovered) {
        boolean ready = xpTenths >= 10;
        g.fill(x, y, x + XP_BUTTON_W, y + XP_BUTTON_H, 0xFF585D63);
        g.fillGradient(x + 1, y + 1, x + XP_BUTTON_W - 1, y + XP_BUTTON_H - 1,
            ready ? (hovered ? 0xFFE9F7DF : 0xFFDDEFD2) : 0xFFD9DDE1, ready ? (hovered ? 0xFFB9DFA0 : 0xFFA8D18D) : 0xFFC4C8CD);
        // XP orb
        int cx = x + 6, cy = y + 6;
        g.fill(cx - 2, cy - 3, cx + 3, cy + 4, ready ? 0xFF4E8C1E : 0xFF8C9298);
        g.fill(cx - 3, cy - 2, cx + 4, cy + 3, ready ? 0xFF4E8C1E : 0xFF8C9298);
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, ready ? 0xFFB6F25A : 0xFFC0C4C8);
        g.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFFFFFFFF);
        String text = String.format("%.1f XP", xpTenths / 10F);
        g.pose().pushPose();
        g.pose().translate(x + 12, y + 3, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(this.font, text, 0, 0, ready ? 0xFF2D5A12 : 0xFF6B737D, false);
        g.pose().popPose();
    }

    protected void renderXpTooltip(GuiGraphics g, int mouseX, int mouseY, int xpTenths) {
        g.renderComponentTooltip(this.font, java.util.List.of(
            Component.translatable("gui.mio_icif.xp.title", String.format("%.1f", xpTenths / 10F)),
            Component.translatable("gui.mio_icif.xp.tip").withStyle(net.minecraft.ChatFormatting.GRAY)), mouseX, mouseY);
    }

    protected void clickXpButton() {
        if (this.minecraft == null || this.minecraft.gameMode == null) return;
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
        this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
            com.miophas.singularity_iteration.common.registry.mio_icif_sounds.UI_CLICK.get(), 1.0F, 0.6F));
    }

    /** Molecular-Transformer-style segmented energy gauge (98x17), used by the advanced machines. */
    protected void drawModernEnergyBar(GuiGraphics guiGraphics, int x, int y, long energy, long maxEnergy) {
        int pixels = (int) (Math.max(0, energy) * KINETIC_ENERGY_BAR2_WIDTH / Math.max(1, maxEnergy));
        drawKineticEnergyBar(guiGraphics, x, y, pixels);
    }

    // === 进度箭头（先渲染背景，再从左往右填充）===
    protected void drawProgressArrow(GuiGraphics guiGraphics, int x, int y, int progressPixels) {
        // 先渲染背景
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) PROGRESS_BAR_BG_U, (float) PROGRESS_BAR_BG_V,
            PROGRESS_BAR_BG_WIDTH, PROGRESS_BAR_BG_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);

        drawProgressArrowFill(guiGraphics, x, y, progressPixels);
    }

    // === 进度箭头填充层（drawProgressArrow 内部调用；仅贴图自带底框的机器才需要单独用它）===
    protected void drawProgressArrowFill(GuiGraphics guiGraphics, int x, int y, int progressPixels) {
        if (progressPixels <= 0) return;
        // 同样夹取到箭头宽度，避免调用方算出超宽像素时向右取到图集里的其它组件
        int progressToDraw = Math.min(progressPixels, ARROW_WIDTH);
        guiGraphics.blit(ATLAS_TEXTURE, x, y, 0,
            (float) ARROW_U, (float) ARROW_V,
            progressToDraw, ARROW_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
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
            (float) LIGHTNING_BG_TEXTURE_X, (float) LIGHTNING_BG_TEXTURE_Y,
            LIGHTNING_BG_WIDTH, LIGHTNING_BG_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);

        drawLightningEnergyFill(guiGraphics, x, y, energy, maxEnergy);
    }

    // === 闪电能量填充层（drawLightningEnergy 内部调用；仅贴图自带底框的机器才需要单独用它）===
    // LIGHTNING_WIDTH/HEIGHT 是图标长宽；LIGHTNING_TEXTURE_X/Y 是图标左上角在图集中的像素位置
    protected void drawLightningEnergyFill(GuiGraphics guiGraphics, int x, int y, int energy, int maxEnergy) {
        // 再渲染能量图标
        if (energy <= 0 || maxEnergy <= 0) return;
        // 必须夹取到图标高度：调用方若传进 energy > maxEnergy（数据索引对不上/存档残留），
        // 未夹取时高度会超出图标，既会从图集里图标"上方"的其他组件取样，也会向上溢出图标框。
        int energyHeight = (int) Math.min((long) energy * LIGHTNING_HEIGHT / maxEnergy, LIGHTNING_HEIGHT);
        if (energyHeight <= 0) return;
        int drawY = y + LIGHTNING_HEIGHT - energyHeight;
        int textureV = LIGHTNING_TEXTURE_Y + LIGHTNING_HEIGHT - energyHeight;
        guiGraphics.blit(ATLAS_TEXTURE, x, drawY, 0,
            (float) LIGHTNING_TEXTURE_X, (float) textureV,
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
