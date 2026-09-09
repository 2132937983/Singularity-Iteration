package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_items_tools;
import com.singularity_iteration.mio_icif.Menu.Producer.MetalFormerAdvancedMenu;
import com.singularity_iteration.mio_icif.api.machine.IMachineAPI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_metal_former_advanced extends mio_icif_screen<MetalFormerAdvancedMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_metal_former.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int PROGRESS_X = 50;
    private static final int PROGRESS_Y = 35;

    private static final int ENERGY_ICON_X = 17;
    private static final int ENERGY_ICON_Y = 36;

    private static final int MODE_BUTTON_SIZE = 16;
    private static final int MODE_BUTTON_X = 67;
    private static final int MODE_BUTTON_Y = 58;

    private static final ResourceLocation BUTTON_SPRITE = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation BUTTON_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/button_highlighted");

    public mio_icif_gui_metal_former_advanced(MetalFormerAdvancedMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MetalFormerAdvancedMenu menu = this.getMenu();
        if (menu != null) {
            int progressPixels = menu.getProgress() * METAL_FORMER_PROGRESS_WIDTH / Math.max(menu.getMaxProgress(), 1);
            drawMetalFormerProgress(guiGraphics, x + PROGRESS_X, y + PROGRESS_Y, progressPixels);

            drawLightningEnergy(guiGraphics, x + ENERGY_ICON_X, y + ENERGY_ICON_Y, menu.getEnergy(), menu.getMaxEnergy());

            boolean buttonHovered = isHoveringModeButton(mouseX, mouseY, x, y);
            renderModeButton(guiGraphics, x + MODE_BUTTON_X, y + MODE_BUTTON_Y, menu.getMode(), buttonHovered);
        }
    }

    private boolean isHoveringModeButton(int mouseX, int mouseY, int guiLeft, int guiTop) {
        return mouseX >= guiLeft + MODE_BUTTON_X && mouseX < guiLeft + MODE_BUTTON_X + MODE_BUTTON_SIZE &&
               mouseY >= guiTop + MODE_BUTTON_Y && mouseY < guiTop + MODE_BUTTON_Y + MODE_BUTTON_SIZE;
    }

    private void renderModeButton(GuiGraphics guiGraphics, int x, int y, IMachineAPI.MetalFormerMode mode, boolean hovered) {
        guiGraphics.blitSprite(hovered ? BUTTON_HIGHLIGHTED_SPRITE : BUTTON_SPRITE, x, y, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE);

        ItemStack iconStack = switch (mode) {
            case ROLLING -> new ItemStack(mio_icif_items_tools.TOOL_HAMMER.get());
            case CUTTING -> new ItemStack(mio_icif_resources.IRON_CUT_BLADE.get());
            case EXTRUDING -> new ItemStack(mio_icif_blocks.WIRE_MV.get().asItem());
        };

        guiGraphics.renderItem(iconStack, x, y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menu != null && this.minecraft != null && this.minecraft.gameMode != null) {
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;

            if (isHoveringModeButton((int) mouseX, (int) mouseY, x, y)) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        MetalFormerAdvancedMenu menu = this.getMenu();
        if (menu == null) return;

        if (isHovering(mouseX, mouseY, x + PROGRESS_X, y + PROGRESS_Y, METAL_FORMER_PROGRESS_WIDTH, METAL_FORMER_PROGRESS_HEIGHT)) {
            renderProgressTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getProgress(), menu.getMaxProgress());
        }

        if (isHovering(mouseX, mouseY, x + ENERGY_ICON_X, y + ENERGY_ICON_Y, LIGHTNING_WIDTH, LIGHTNING_HEIGHT)) {
            renderEnergyTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getEnergy(), menu.getMaxEnergy());
        }

        if (mouseX >= x + MODE_BUTTON_X && mouseX <= x + MODE_BUTTON_X + MODE_BUTTON_SIZE &&
            mouseY >= y + MODE_BUTTON_Y && mouseY <= y + MODE_BUTTON_Y + MODE_BUTTON_SIZE) {
            Component modeName = switch (menu.getMode()) {
                case ROLLING -> Component.translatable("gui.mio_icif.metal_former.mode_rolling");
                case CUTTING -> Component.translatable("gui.mio_icif.metal_former.mode_cutting");
                case EXTRUDING -> Component.translatable("gui.mio_icif.metal_former.mode_extruding");
            };
            guiGraphics.renderTooltip(this.font,
                Component.translatable("gui.mio_icif.metal_former.mode_tooltip", modeName),
                mouseX - x, mouseY - y);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();

        MetalFormerAdvancedMenu menu = this.getMenu();
        if (menu != null && menu.getBlockEntity() != null) {
            var be = menu.getBlockEntity();
            menu.setSyncData(0, be.getProgress());
            menu.setSyncData(1, be.getMaxProgress());
            menu.setSyncData(2, be.isWorking() ? 1 : 0);
            menu.setSyncData(3, (int) be.getEnergyStorage().getAmount());
            menu.setSyncData(4, (int) be.getEnergyStorage().getCapacity());
            menu.setSyncData(5, be.getMode().ordinal());
        }
    }
}