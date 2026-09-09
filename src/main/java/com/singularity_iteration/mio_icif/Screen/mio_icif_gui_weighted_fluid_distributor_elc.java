package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_weighted_fluid_distributor_elc;
import com.singularity_iteration.mio_icif.Menu.Producer.WeightedFluidDistributorElcMenu;
import com.singularity_iteration.mio_icif.network.WeightedDistributorPriorityPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_weighted_fluid_distributor_elc extends mio_icif_screen<WeightedFluidDistributorElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_weighted_fluid_distributor.png");

    private static final Direction[] BUTTON_DIRECTIONS = {
        Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN
    };

    private static final String[] DIRECTION_NAMES = {
        "U", "N", "S", "W", "E", "D"
    };

    private static final int TANK_X = 33;
    private static final int TANK_Y = 111;
    private static final int TANK_WIDTH = 110;
    private static final int TANK_HEIGHT = 10;

    private boolean[][] buttonStates = new boolean[5][6];
    private mio_icif_weighted_fluid_distributor_elc clientBotter;

    public mio_icif_gui_weighted_fluid_distributor_elc(WeightedFluidDistributorElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 211;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
        loadPriorityFromClient();
    }

    private mio_icif_weighted_fluid_distributor_elc getClientBotter() {
        if (clientBotter != null && !clientBotter.isRemoved()) return clientBotter;
        if (minecraft != null && minecraft.level != null && menu.getBlockPos() != null) {
            var be = minecraft.level.getBlockEntity(menu.getBlockPos());
            if (be instanceof mio_icif_weighted_fluid_distributor_elc botter) {
                clientBotter = botter;
                return clientBotter;
            }
        }
        return null;
    }

    private void loadPriorityFromClient() {
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 6; j++) {
                buttonStates[i][j] = false;
            }
        }
        mio_icif_weighted_fluid_distributor_elc botter = getClientBotter();
        if (botter != null) {
            List<Direction> priorities = botter.getPriority();
            for (int i = 0; i < priorities.size() && i < 5; i++) {
                int col = findColumnForDirection(priorities.get(i));
                if (col >= 0) {
                    buttonStates[i][col] = true;
                }
            }
        }
    }

    private static int findColumnForDirection(Direction dir) {
        for (int i = 0; i < BUTTON_DIRECTIONS.length; i++) {
            if (BUTTON_DIRECTIONS[i] == dir) return i;
        }
        return -1;
    }

    private Direction getFacingFromClient() {
        mio_icif_weighted_fluid_distributor_elc botter = getClientBotter();
        if (botter != null) return botter.getFacing();
        return menu.getFacing();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int relX = (int) mouseX - x;
        int relY = (int) mouseY - y;

        Direction facing = getFacingFromClient();

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 6; col++) {
                int btnX = 63 + col * 18;
                int btnY = 17 + row * 18;
                if (relX >= btnX && relX < btnX + 16 && relY >= btnY && relY < btnY + 16) {
                    Direction dir = BUTTON_DIRECTIONS[col];
                    if (dir == facing) return super.mouseClicked(mouseX, mouseY, button);
                    handleButtonClick(row, col);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleButtonClick(int row, int col) {
        boolean switchingOff = false;
        for (int i = 0; i < 5; i++) {
            if (buttonStates[i][col]) {
                buttonStates[i][col] = false;
                switchingOff = (i == row);
                rebalance(i);
                break;
            }
        }

        if (!switchingOff) {
            int emptyRow = findNextEmptyRow(row);
            for (int j = 0; j < 6; j++) {
                buttonStates[emptyRow][j] = (j == col);
            }
        }

        sendPriorityToServer();
    }

    private void rebalance(int change) {
        for (int i = change + 1; i < 5; i++) {
            for (int side = 0; side < 6; side++) {
                if (buttonStates[i][side]) {
                    buttonStates[i - 1][side] = true;
                    buttonStates[i][side] = false;
                    break;
                }
            }
        }
    }

    private int findNextEmptyRow(int start) {
        while (start-- > 0) {
            for (int j = 0; j < 6; j++) {
                if (buttonStates[start][j]) return start + 1;
            }
        }
        return 0;
    }

    private void sendPriorityToServer() {
        List<Integer> priorities = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 6; j++) {
                if (buttonStates[i][j]) {
                    priorities.add(BUTTON_DIRECTIONS[j].get3DDataValue());
                    break;
                }
            }
        }
        PacketDistributor.sendToServer(new WeightedDistributorPriorityPacket(menu.getBlockPos(), priorities));
    }

    @SuppressWarnings("unused")
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        mio_icif_weighted_fluid_distributor_elc botter = getClientBotter();
        int fluidAmount = menu.getFluidAmount();
        int fluidCapacity = menu.getFluidCapacity();
        int fluidId = menu.getFluidId();
        if (fluidAmount > 0 && fluidCapacity > 0 && fluidId >= 0) {
            FluidStack displayFluid = new FluidStack(BuiltInRegistries.FLUID.byId(fluidId), fluidAmount);
            mio_icif_GuiUtils.renderFluidBarHorizontal(guiGraphics, x + TANK_X, y + TANK_Y,
                TANK_WIDTH, TANK_HEIGHT, displayFluid, fluidCapacity);
        }

        Direction facing = getFacingFromClient();

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 6; col++) {
                int btnX = x + 63 + col * 18;
                int btnY = y + 17 + row * 18;

                Direction dir = BUTTON_DIRECTIONS[col];
                boolean isFacing = (dir == facing);

                if (isFacing) {
                    guiGraphics.fill(btnX, btnY, btnX + 16, btnY + 16, 0x40404040);
                } else if (buttonStates[row][col]) {
                    guiGraphics.fill(btnX, btnY, btnX + 16, btnY + 16, 0xFF404040);
                    guiGraphics.drawString(this.font, DIRECTION_NAMES[col], btnX + 4, btnY + 4, 0xFFFFFF, false);
                } else {
                    guiGraphics.fill(btnX, btnY, btnX + 16, btnY + 16, 0x20000000);
                    guiGraphics.drawString(this.font, DIRECTION_NAMES[col], btnX + 4, btnY + 4, 0x808080, false);
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        for (int row = 0; row < 5; row++) {
            String label;
            switch (row) {
                case 0: label = "Highest"; break;
                case 1: label = "\u2191"; break;
                case 2: label = "Priority"; break;
                case 3: label = "\u2193"; break;
                case 4: label = "Lowest"; break;
                default: label = ""; break;
            }
            guiGraphics.drawString(this.font, label, 8, 21 + row * 18, 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int relX = mouseX - x;
        int relY = mouseY - y;

        if (relX >= TANK_X && relX < TANK_X + TANK_WIDTH && relY >= TANK_Y && relY < TANK_Y + TANK_HEIGHT) {
            int amount = menu.getFluidAmount();
            int capacity = menu.getFluidCapacity();
            String tooltip;
            if (amount <= 0) {
                tooltip = Component.translatable("mio_icif.tooltip.empty_tank").getString();
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
    }
}