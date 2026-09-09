package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.singularity_iteration.mio_icif.Menu.Producer.CannerElcMenu;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 装罐机方块的 GUI类
 */
@OnlyIn(Dist.CLIENT)
public class mio_icif_gui_canner_elc extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Producer.CannerElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_canner_elc.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 184;

    // 进度条位置(74, 22)
    private static final int PROGRESS_X = 74;
    private static final int PROGRESS_Y = 22;
    private static final int PROGRESS_WIDTH = 24;
    private static final int PROGRESS_HEIGHT = 15;

    private static final int ENERGY_ICON_X = 9;
    private static final int ENERGY_ICON_Y = 61;

    // 输入液体槽位置
    private static final int INPUT_FLUID_TANK_X = 39;
    private static final int INPUT_FLUID_TANK_Y = 42;

    // 输出液体槽位置
    private static final int OUTPUT_FLUID_TANK_X = 117;
    private static final int OUTPUT_FLUID_TANK_Y = 42;

    // 模式按钮位置和尺寸
    private static final int MODE_BUTTON_X = 63;
    private static final int MODE_BUTTON_Y = 81;
    private static final int MODE_BUTTON_WIDTH = 50;
    private static final int MODE_BUTTON_HEIGHT = 14;

    // 模式图标纹理位置（4种模式对应的图标）
    // 模式0（装罐）：(96, 20)
    private static final int[] MODE_ICON_TEXTURE_Y = {211, 211, 211, 227};
    private static final int[] MODE_ICON_TEXTURE_X = {62, 114, 166, 2};
    private static final int MODE_ICON_WIDTH = 50;
    private static final int MODE_ICON_HEIGHT = 14;

    // 交换液体按钮位置和尺寸(77, 64)
    private static final int SWAP_BUTTON_X = 77;
    private static final int SWAP_BUTTON_Y = 64;
    private static final int SWAP_BUTTON_WIDTH = 22;
    private static final int SWAP_BUTTON_HEIGHT = 13;

    // 清空左侧水槽按钮位置 (25, 82)
    private static final int CLEAR_INPUT_BUTTON_X = 25;
    private static final int CLEAR_INPUT_BUTTON_Y = 82;
    private static final int CLEAR_BUTTON_WIDTH = 14;
    private static final int CLEAR_BUTTON_HEIGHT = 14;

    // 清空右侧水槽按钮位置 (137, 82)
    private static final int CLEAR_OUTPUT_BUTTON_X = 137;
    private static final int CLEAR_OUTPUT_BUTTON_Y = 82;

    public mio_icif_gui_canner_elc(CannerElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        CannerElcMenu menu = this.getMenu();
        if (menu != null) {
            int inputFluidAmount = menu.getInputFluidAmount();
            int inputFluidCapacity = menu.getInputFluidCapacity();
            FluidStack inputFluid = menu.getInputFluid();
            drawFluidTank(guiGraphics, x + INPUT_FLUID_TANK_X, y + INPUT_FLUID_TANK_Y,
                inputFluid, inputFluidAmount, inputFluidCapacity);

            int outputFluidAmount = menu.getOutputFluidAmount();
            int outputFluidCapacity = menu.getOutputFluidCapacity();
            FluidStack outputFluid = menu.getOutputFluid();
            drawFluidTank(guiGraphics, x + OUTPUT_FLUID_TANK_X, y + OUTPUT_FLUID_TANK_Y,
                outputFluid, outputFluidAmount, outputFluidCapacity);

            int progressPixels = menu.getProgressPixels();
            guiGraphics.blit(ATLAS_TEXTURE, x + PROGRESS_X, y + PROGRESS_Y, 0,
                (float) PROGRESS_BAR_BG_U, (float) PROGRESS_BAR_BG_V,
                PROGRESS_BAR_BG_WIDTH, PROGRESS_BAR_BG_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
            if (progressPixels > 0) {
                guiGraphics.blit(ATLAS_TEXTURE, x + PROGRESS_X, y + PROGRESS_Y, 0, (float) ARROW_U, (float) ARROW_V, progressPixels, PROGRESS_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
            }

            drawLightningEnergy(guiGraphics, x + ENERGY_ICON_X, y + ENERGY_ICON_Y, menu.getEnergy(), menu.getMaxEnergy());

            int mode = menu.getMode();
            if (mode >= 0 && mode < 4) {
                int iconTextureY = MODE_ICON_TEXTURE_Y[mode];
                int iconTextureX = MODE_ICON_TEXTURE_X[mode];
                guiGraphics.blit(ATLAS_TEXTURE, x + MODE_BUTTON_X, y + MODE_BUTTON_Y, 0, (float) iconTextureX, (float) iconTextureY, MODE_ICON_WIDTH, MODE_ICON_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
            }

            if (mode == 0) {
                guiGraphics.fill(x + 59, y + 53, x + 59 + 12, y + 53 + 18, 0xFFc6c6c6);
                guiGraphics.fill(x + 100, y + 53, x + 100 + 17, y + 53 + 23, 0xFFc6c6c6);
            }

            if (mode == 1) {
                guiGraphics.fill(x + 59, y + 53, x + 59 + 12, y + 53 + 18, 0xFFc6c6c6);
                guiGraphics.blit(ATLAS_TEXTURE, x + 71, y + 43, 0, (float) 56, (float) 164, 26, 18, ATLAS_WIDTH, ATLAS_HEIGHT);
            }

            if (mode == 2) {
                guiGraphics.fill(x + 100, y + 53, x + 100 + 17, y + 53 + 23, 0xFFc6c6c6);
                guiGraphics.blit(ATLAS_TEXTURE, x + 71, y + 43, 0, (float) 56, (float) 164, 26, 18, ATLAS_WIDTH, ATLAS_HEIGHT);
            }

            if (isHoveringModeButton(mouseX, mouseY, x, y)) {
                guiGraphics.fill(x + MODE_BUTTON_X, y + MODE_BUTTON_Y,
                    x + MODE_BUTTON_X + MODE_BUTTON_WIDTH, y + MODE_BUTTON_Y + MODE_BUTTON_HEIGHT,
                    0x40FFFFFF);
            }

            if (isHoveringSwapButton(mouseX, mouseY, x, y)) {
                guiGraphics.fill(x + SWAP_BUTTON_X, y + SWAP_BUTTON_Y,
                    x + SWAP_BUTTON_X + SWAP_BUTTON_WIDTH, y + SWAP_BUTTON_Y + SWAP_BUTTON_HEIGHT,
                    0x40FFFFFF);
            }

            // 渲染清空按钮（使用工业工作台的取消按钮纹理）
            guiGraphics.blit(ATLAS_TEXTURE, x + CLEAR_INPUT_BUTTON_X, y + CLEAR_INPUT_BUTTON_Y, 0,
                (float) 34, (float) 243, CLEAR_BUTTON_WIDTH, CLEAR_BUTTON_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);

            guiGraphics.blit(ATLAS_TEXTURE, x + CLEAR_OUTPUT_BUTTON_X, y + CLEAR_OUTPUT_BUTTON_Y, 0,
                (float) 34, (float) 243, CLEAR_BUTTON_WIDTH, CLEAR_BUTTON_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);

            // 清空按钮悬停高亮
            if (isHoveringClearInputButton(mouseX, mouseY, x, y)) {
                guiGraphics.fill(x + CLEAR_INPUT_BUTTON_X, y + CLEAR_INPUT_BUTTON_Y,
                    x + CLEAR_INPUT_BUTTON_X + CLEAR_BUTTON_WIDTH, y + CLEAR_INPUT_BUTTON_Y + CLEAR_BUTTON_HEIGHT,
                    0x80FFFFFF);
            }
            if (isHoveringClearOutputButton(mouseX, mouseY, x, y)) {
                guiGraphics.fill(x + CLEAR_OUTPUT_BUTTON_X, y + CLEAR_OUTPUT_BUTTON_Y,
                    x + CLEAR_OUTPUT_BUTTON_X + CLEAR_BUTTON_WIDTH, y + CLEAR_OUTPUT_BUTTON_Y + CLEAR_BUTTON_HEIGHT,
                    0x80FFFFFF);
            }
        }
    }

    private boolean isHoveringModeButton(int mouseX, int mouseY, int guiX, int guiY) {
        return mouseX >= guiX + MODE_BUTTON_X && mouseX <= guiX + MODE_BUTTON_X + MODE_BUTTON_WIDTH &&
               mouseY >= guiY + MODE_BUTTON_Y && mouseY <= guiY + MODE_BUTTON_Y + MODE_BUTTON_HEIGHT;
    }

    private boolean isHoveringSwapButton(int mouseX, int mouseY, int guiX, int guiY) {
        return mouseX >= guiX + SWAP_BUTTON_X && mouseX <= guiX + SWAP_BUTTON_X + SWAP_BUTTON_WIDTH &&
               mouseY >= guiY + SWAP_BUTTON_Y && mouseY <= guiY + SWAP_BUTTON_Y + SWAP_BUTTON_HEIGHT;
    }

    private boolean isHoveringClearInputButton(int mouseX, int mouseY, int guiX, int guiY) {
        return mouseX >= guiX + CLEAR_INPUT_BUTTON_X && mouseX <= guiX + CLEAR_INPUT_BUTTON_X + CLEAR_BUTTON_WIDTH &&
               mouseY >= guiY + CLEAR_INPUT_BUTTON_Y && mouseY <= guiY + CLEAR_INPUT_BUTTON_Y + CLEAR_BUTTON_HEIGHT;
    }

    private boolean isHoveringClearOutputButton(int mouseX, int mouseY, int guiX, int guiY) {
        return mouseX >= guiX + CLEAR_OUTPUT_BUTTON_X && mouseX <= guiX + CLEAR_OUTPUT_BUTTON_X + CLEAR_BUTTON_WIDTH &&
               mouseY >= guiY + CLEAR_OUTPUT_BUTTON_Y && mouseY <= guiY + CLEAR_OUTPUT_BUTTON_Y + CLEAR_BUTTON_HEIGHT;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        CannerElcMenu menu = this.getMenu();
        if (menu == null) return;

        // 检查鼠标是否在进度条区域
        if (isHovering(mouseX, mouseY, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_WIDTH, PROGRESS_HEIGHT)) {
            renderProgressTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getProgress(), menu.getMaxProgress());
        }

        // 检查鼠标是否在电能标志区域
        if (isHovering(mouseX, mouseY, x + ENERGY_ICON_X, y + ENERGY_ICON_Y, LIGHTNING_WIDTH, LIGHTNING_HEIGHT)) {
            renderEnergyTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getEnergy(), menu.getMaxEnergy());
        }

        // 检查鼠标是否在输入流体槽区域
        if (mouseX >= x + INPUT_FLUID_TANK_X && mouseX <= x + INPUT_FLUID_TANK_X + FLUID_TANK_BG_WIDTH &&
            mouseY >= y + INPUT_FLUID_TANK_Y && mouseY <= y + INPUT_FLUID_TANK_Y + FLUID_TANK_BG_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal((!menu.getInputFluid().isEmpty() ? menu.getInputFluid().getFluid().getFluidType().getDescription().getString() + ": " : "") + menu.getInputFluidAmount() + "/" + menu.getInputFluidCapacity() + " mB"),
                mouseX - x, mouseY - y);
        }

        // 检查鼠标是否在输出流体槽区域
        if (mouseX >= x + OUTPUT_FLUID_TANK_X && mouseX <= x + OUTPUT_FLUID_TANK_X + FLUID_TANK_BG_WIDTH &&
            mouseY >= y + OUTPUT_FLUID_TANK_Y && mouseY <= y + OUTPUT_FLUID_TANK_Y + FLUID_TANK_BG_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal((!menu.getOutputFluid().isEmpty() ? menu.getOutputFluid().getFluid().getFluidType().getDescription().getString() + ": " : "") + menu.getOutputFluidAmount() + "/" + menu.getOutputFluidCapacity() + " mB"),
                mouseX - x, mouseY - y);
        }

        // 检查鼠标是否在模式按钮区域
        if (mouseX >= x + MODE_BUTTON_X && mouseX <= x + MODE_BUTTON_X + MODE_BUTTON_WIDTH &&
            mouseY >= y + MODE_BUTTON_Y && mouseY <= y + MODE_BUTTON_Y + MODE_BUTTON_HEIGHT) {
            Component modeName = switch (menu.getMode()) {
                case 0 -> Component.translatable("gui.mio_icif.canner.mode_canning");
                case 1 -> Component.translatable("gui.mio_icif.canner.mode_empty_to_tank");
                case 2 -> Component.translatable("gui.mio_icif.canner.mode_fill_from_tank");
                case 3 -> Component.translatable("gui.mio_icif.canner.mode_mix");
                default -> Component.translatable("gui.mio_icif.canner.mode_unknown");
            };
            guiGraphics.renderTooltip(this.font,
                Component.translatable("gui.mio_icif.canner.mode_tooltip", modeName),
                mouseX - x, mouseY - y);
        }

        // 检查鼠标是否在交换液体按钮区域
        if (mouseX >= x + SWAP_BUTTON_X && mouseX <= x + SWAP_BUTTON_X + SWAP_BUTTON_WIDTH &&
            mouseY >= y + SWAP_BUTTON_Y && mouseY <= y + SWAP_BUTTON_Y + SWAP_BUTTON_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.translatable("gui.mio_icif.canner.swap_fluids"),
                mouseX - x, mouseY - y);
        }

        // 检查鼠标是否在清空左侧水槽按钮区域
        if (mouseX >= x + CLEAR_INPUT_BUTTON_X && mouseX <= x + CLEAR_INPUT_BUTTON_X + CLEAR_BUTTON_WIDTH &&
            mouseY >= y + CLEAR_INPUT_BUTTON_Y && mouseY <= y + CLEAR_INPUT_BUTTON_Y + CLEAR_BUTTON_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.translatable("gui.mio_icif.canner.clear_input_tank"),
                mouseX - x, mouseY - y);
        }

        // 检查鼠标是否在清空右侧水槽按钮区域
        if (mouseX >= x + CLEAR_OUTPUT_BUTTON_X && mouseX <= x + CLEAR_OUTPUT_BUTTON_X + CLEAR_BUTTON_WIDTH &&
            mouseY >= y + CLEAR_OUTPUT_BUTTON_Y && mouseY <= y + CLEAR_OUTPUT_BUTTON_Y + CLEAR_BUTTON_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.translatable("gui.mio_icif.canner.clear_output_tank"),
                mouseX - x, mouseY - y);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (mouseX >= x + MODE_BUTTON_X && mouseX <= x + MODE_BUTTON_X + MODE_BUTTON_WIDTH &&
            mouseY >= y + MODE_BUTTON_Y && mouseY <= y + MODE_BUTTON_Y + MODE_BUTTON_HEIGHT) {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                net.minecraft.client.resources.sounds.SimpleSoundInstance clickSound =
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F);
                this.minecraft.getSoundManager().play(clickSound);
                this.menu.clickMenuButton(this.minecraft.player, 0);
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
            return true;
        }

        if (mouseX >= x + SWAP_BUTTON_X && mouseX <= x + SWAP_BUTTON_X + SWAP_BUTTON_WIDTH &&
            mouseY >= y + SWAP_BUTTON_Y && mouseY <= y + SWAP_BUTTON_Y + SWAP_BUTTON_HEIGHT) {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                net.minecraft.client.resources.sounds.SimpleSoundInstance clickSound =
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F);
                this.minecraft.getSoundManager().play(clickSound);
                this.menu.clickMenuButton(this.minecraft.player, 1);
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
            }
            return true;
        }

        if (mouseX >= x + CLEAR_INPUT_BUTTON_X && mouseX <= x + CLEAR_INPUT_BUTTON_X + CLEAR_BUTTON_WIDTH &&
            mouseY >= y + CLEAR_INPUT_BUTTON_Y && mouseY <= y + CLEAR_INPUT_BUTTON_Y + CLEAR_BUTTON_HEIGHT) {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                net.minecraft.client.resources.sounds.SimpleSoundInstance clickSound =
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F);
                this.minecraft.getSoundManager().play(clickSound);
                this.menu.clickMenuButton(this.minecraft.player, 2);
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
            }
            return true;
        }

        if (mouseX >= x + CLEAR_OUTPUT_BUTTON_X && mouseX <= x + CLEAR_OUTPUT_BUTTON_X + CLEAR_BUTTON_WIDTH &&
            mouseY >= y + CLEAR_OUTPUT_BUTTON_Y && mouseY <= y + CLEAR_OUTPUT_BUTTON_Y + CLEAR_BUTTON_HEIGHT) {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                net.minecraft.client.resources.sounds.SimpleSoundInstance clickSound =
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F);
                this.minecraft.getSoundManager().play(clickSound);
                this.menu.clickMenuButton(this.minecraft.player, 3);
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 3);
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void containerTick() {
        super.containerTick();
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        // 在单元水槽模式下隐藏材料槽
        if (slot.index == CannerElcMenu.MATERIAL_SLOT && this.menu.getMode() == 1) {
            // 不渲染材料槽
            return;
        }
        super.renderSlot(guiGraphics, slot);
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        // 在单元水槽模式下禁用材料槽的高光效果
        if (slot.index == CannerElcMenu.MATERIAL_SLOT && this.menu.getMode() == 1) {
            // 不渲染高光
            return;
        }
        super.renderSlotHighlight(guiGraphics, slot, mouseX, mouseY, partialTick);
    }

}