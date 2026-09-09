package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_fluid_distributor_elc;
import com.singularity_iteration.mio_icif.Menu.Producer.FluidDistributorElcMenu;
import com.singularity_iteration.mio_icif.network.FluidDistributorModePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_fluid_distributor_elc extends mio_icif_screen<FluidDistributorElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_fluid_distributor_elc.png");

    private static final int TANK_X = 29;
    private static final int TANK_Y = 38;
    private static final int TANK_WIDTH = 55;
    private static final int TANK_HEIGHT = 47;

    private static final int MODE_BUTTON_X = 117;
    private static final int MODE_BUTTON_Y = 58;
    private static final int MODE_BUTTON_WIDTH = 18;
    private static final int MODE_BUTTON_HEIGHT = 8;

    private mio_icif_fluid_distributor_elc distributor;

    public mio_icif_gui_fluid_distributor_elc(FluidDistributorElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 184;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;

        BlockPos pos = menu.getBlockPos();
        if (pos != null && minecraft != null && minecraft.level != null) {
            BlockEntity be = minecraft.level.getBlockEntity(pos);
            if (be instanceof mio_icif_fluid_distributor_elc dist) {
                this.distributor = dist;
            }
        }
    }

    private boolean isHoveringModeButton(int screenMouseX, int screenMouseY) {
        int relX = screenMouseX - leftPos;
        int relY = screenMouseY - topPos;
        return relX >= MODE_BUTTON_X && relX <= MODE_BUTTON_X + MODE_BUTTON_WIDTH &&
               relY >= MODE_BUTTON_Y && relY <= MODE_BUTTON_Y + MODE_BUTTON_HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        if (relX >= MODE_BUTTON_X && relX <= MODE_BUTTON_X + MODE_BUTTON_WIDTH &&
            relY >= MODE_BUTTON_Y && relY <= MODE_BUTTON_Y + MODE_BUTTON_HEIGHT) {
            FluidDistributorElcMenu m = this.getMenu();
            if (m != null && m.getBlockPos() != null) {
                PacketDistributor.sendToServer(new FluidDistributorModePacket(m.getBlockPos()));
                if (distributor != null) {
                    distributor.toggleMode();
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        if (distributor != null) {
            int fluidAmount = menu.getFluidAmount();
            int fluidCapacity = menu.getFluidCapacity();
            int fluidId = menu.getFluidId();
            if (fluidAmount > 0 && fluidCapacity > 0 && fluidId >= 0) {
                FluidStack displayFluid = new FluidStack(BuiltInRegistries.FLUID.byId(fluidId), fluidAmount);
                mio_icif_GuiUtils.renderFluidBar(guiGraphics, x + TANK_X, y + TANK_Y,
                    TANK_WIDTH, TANK_HEIGHT, displayFluid, fluidCapacity);
            }
        }

        if (isHoveringModeButton(mouseX, mouseY)) {
            guiGraphics.fill(x + MODE_BUTTON_X, y + MODE_BUTTON_Y,
                x + MODE_BUTTON_X + MODE_BUTTON_WIDTH, y + MODE_BUTTON_Y + MODE_BUTTON_HEIGHT,
                0x40FFFFFF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        guiGraphics.drawString(this.font, Component.translatable("gui.mio_icif.fluid_distributor.mode_info"), 112, 47, 0x57C6DA, false);

        boolean active = (distributor != null && distributor.isActive()) || menu.isActive();
        if (active) {
            guiGraphics.drawString(this.font, Component.translatable("gui.mio_icif.fluid_distributor.mode_concentrate"), 95, 71, 0x57C6DA, false);
        } else {
            guiGraphics.drawString(this.font, Component.translatable("gui.mio_icif.fluid_distributor.mode_distribute"), 95, 71, 0x57C6DA, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (isHoveringModeButton(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.mio_icif.fluid_distributor.toggle_mode"), mouseX, mouseY);
        }
    }
}