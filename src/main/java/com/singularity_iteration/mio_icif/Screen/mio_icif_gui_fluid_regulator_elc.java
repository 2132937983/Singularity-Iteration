package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_fluid_regulator_elc;
import com.singularity_iteration.mio_icif.Menu.Producer.FluidRegulatorElcMenu;
import com.singularity_iteration.mio_icif.network.FluidRegulatorFlowRatePacket;
import com.singularity_iteration.mio_icif.network.FluidRegulatorModePacket;
import net.minecraft.client.Minecraft;
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
public class mio_icif_gui_fluid_regulator_elc extends mio_icif_screen<FluidRegulatorElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_fluid_regulator_elc.png");

    private static final int TANK_X = 77;
    private static final int TANK_Y = 40;

    private static final int LIGHTNING_X = 9;
    private static final int LIGHTNING_Y = 38;

    @SuppressWarnings("unused")
    private mio_icif_fluid_regulator_elc regulator;

    public mio_icif_gui_fluid_regulator_elc(FluidRegulatorElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 184;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;

        BlockPos pos = menu.getBlockPos();
        if (pos != null && Minecraft.getInstance() != null && Minecraft.getInstance().level != null) {
            BlockEntity be = Minecraft.getInstance().level.getBlockEntity(pos);
            if (be instanceof mio_icif_fluid_regulator_elc reg) {
                this.regulator = reg;
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        BlockPos pos = menu.getBlockPos();
        if (pos != null && Minecraft.getInstance() != null && Minecraft.getInstance().level != null) {
            BlockEntity be = Minecraft.getInstance().level.getBlockEntity(pos);
            if (be instanceof mio_icif_fluid_regulator_elc reg) {
                this.regulator = reg;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;

        BlockPos pos = menu.getBlockPos();
        if (pos == null) return super.mouseClicked(mouseX, mouseY, button);

        if (relX >= 102 && relY >= 68 && relX <= 110 && relY <= 76) {
            PacketDistributor.sendToServer(new FluidRegulatorFlowRatePacket(pos, -1000));
            applyFlowRateChange(-1000);
        }
        if (relX >= 112 && relY >= 68 && relX <= 120 && relY <= 76) {
            PacketDistributor.sendToServer(new FluidRegulatorFlowRatePacket(pos, -100));
            applyFlowRateChange(-100);
        }
        if (relX >= 122 && relY >= 68 && relX <= 130 && relY <= 76) {
            PacketDistributor.sendToServer(new FluidRegulatorFlowRatePacket(pos, -10));
            applyFlowRateChange(-10);
        }
        if (relX >= 132 && relY >= 68 && relX <= 140 && relY <= 76) {
            PacketDistributor.sendToServer(new FluidRegulatorFlowRatePacket(pos, -1));
            applyFlowRateChange(-1);
        }

        if (relX >= 132 && relY >= 44 && relX <= 140 && relY <= 52) {
            PacketDistributor.sendToServer(new FluidRegulatorFlowRatePacket(pos, 1));
            applyFlowRateChange(1);
        }
        if (relX >= 122 && relY >= 44 && relX <= 130 && relY <= 52) {
            PacketDistributor.sendToServer(new FluidRegulatorFlowRatePacket(pos, 10));
            applyFlowRateChange(10);
        }
        if (relX >= 112 && relY >= 44 && relX <= 120 && relY <= 52) {
            PacketDistributor.sendToServer(new FluidRegulatorFlowRatePacket(pos, 100));
            applyFlowRateChange(100);
        }
        if (relX >= 102 && relY >= 44 && relX <= 110 && relY <= 52) {
            PacketDistributor.sendToServer(new FluidRegulatorFlowRatePacket(pos, 1000));
            applyFlowRateChange(1000);
        }

        if (relX >= 151 && relY >= 44 && relX <= 161 && relY <= 52) {
            PacketDistributor.sendToServer(new FluidRegulatorModePacket(pos, 1001));
            applyModeChange(1001);
        }
        if (relX >= 151 && relY >= 68 && relX <= 161 && relY <= 76) {
            PacketDistributor.sendToServer(new FluidRegulatorModePacket(pos, 1002));
            applyModeChange(1002);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void applyFlowRateChange(int delta) {
        int current = menu.getOutputmb();
        int newValue = Math.max(0, Math.min(1000, current + delta));
        menu.setSyncData(5, newValue);
    }

    private void applyModeChange(int event) {
        int currentMode = menu.getMode();
        if (event == 1001 && currentMode == 0) {
            menu.setSyncData(6, 1);
        } else if (event == 1002 && currentMode == 1) {
            menu.setSyncData(6, 0);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 渲染流体槽 - 固定渲染背景和刻度，有流体时渲染流体
        int fluidAmount = menu.getFluidAmount();
        int fluidCapacity = menu.getFluidCapacity();
        int fluidId = menu.getFluidId();
        if (fluidAmount > 0 && fluidCapacity > 0 && fluidId >= 0) {
            FluidStack displayFluid = new FluidStack(BuiltInRegistries.FLUID.byId(fluidId), fluidAmount);
            drawFluidTank(guiGraphics, x + TANK_X, y + TANK_Y, displayFluid, fluidAmount, fluidCapacity);
        } else {
            drawFluidTank(guiGraphics, x + TANK_X, y + TANK_Y, null, 0, fluidCapacity);
        }

        // 渲染闪电能量图标
        int energy = menu.getEnergy();
        int maxEnergy = menu.getMaxEnergy();
        drawLightningEnergy(guiGraphics, x + LIGHTNING_X, y + LIGHTNING_Y, energy, maxEnergy);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        int outputmb = menu.getOutputmb();
        int mode = menu.getMode();

        guiGraphics.drawString(this.font, outputmb + " mB", 105, 57, 0x20D3DE, false);
        guiGraphics.drawString(this.font, mode == 0 ? "秒" : "tick", 145, 57, 0x20D3DE, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int relX = mouseX - leftPos;
        int relY = mouseY - topPos;

        if (relX >= TANK_X && relX <= TANK_X + FLUID_TANK_BG_WIDTH &&
            relY >= TANK_Y && relY <= TANK_Y + FLUID_TANK_BG_HEIGHT) {
            int amount = menu.getFluidAmount();
            int capacity = menu.getFluidCapacity();
            String tooltip;
            if (amount <= 0) {
                tooltip = "空";
            } else {
                int fluidId = menu.getFluidId();
                if (fluidId < 0) {
                    tooltip = amount + "/" + capacity + " mB";
                } else {
                    FluidStack fluid = new FluidStack(BuiltInRegistries.FLUID.byId(fluidId), amount);
                    tooltip = fluid.getHoverName().getString() + ": " + amount + "/" + capacity + " mB";
                }
            }
            guiGraphics.renderTooltip(this.font, Component.literal(tooltip), mouseX, mouseY);
        }

        if (relX >= LIGHTNING_X && relX <= LIGHTNING_X + LIGHTNING_WIDTH &&
            relY >= LIGHTNING_Y && relY <= LIGHTNING_Y + LIGHTNING_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal(menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                mouseX, mouseY);
        }
    }
}