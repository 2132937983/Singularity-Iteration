package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_item_distributor_elc;
import com.singularity_iteration.mio_icif.Menu.Producer.ItemDistributorElcMenu;
import com.singularity_iteration.mio_icif.network.WeightedDistributorPriorityPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_item_distributor_elc extends mio_icif_screen<ItemDistributorElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_weighted_item_distributor.png");

    private static final Direction[] BUTTON_DIRECTIONS = {
        Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH
    };

    private static final String[] DIRECTION_NAMES = {
        "W", "E", "D", "U", "N", "S"
    };

    private mio_icif_item_distributor_elc distributor;

    public mio_icif_gui_item_distributor_elc(ItemDistributorElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 211;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;

        BlockPos pos = menu.getBlockPos();
        if (pos != null && minecraft != null && minecraft.level != null) {
            BlockEntity be = minecraft.level.getBlockEntity(pos);
            if (be instanceof mio_icif_item_distributor_elc dist) {
                this.distributor = dist;
            }
        }
    }

    private boolean isButtonOn(int row, int col) {
        if (distributor == null) return false;
        List<Direction> priority = distributor.getPriority();
        if (row < priority.size()) {
            return priority.get(row) == BUTTON_DIRECTIONS[col];
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (distributor == null) return super.mouseClicked(mouseX, mouseY, button);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int relX = (int) mouseX - x;
        int relY = (int) mouseY - y;

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 6; col++) {
                int btnX = 63 + col * 18;
                int btnY = 17 + row * 18;
                if (relX >= btnX && relX < btnX + 16 && relY >= btnY && relY < btnY + 16) {
                    Direction facing = BUTTON_DIRECTIONS[col];
                    if (distributor.getFacing() == facing) return super.mouseClicked(mouseX, mouseY, button);
                    handleButtonClick(row, col);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleButtonClick(int row, int col) {
        Direction clickedDir = BUTTON_DIRECTIONS[col];
        List<Direction> priority = distributor.getPriority();

        boolean switchingOff = false;
        int aimCol = findColumnForDirection(clickedDir);
        for (int i = 0; i < 5; i++) {
            if (isButtonOn(i, aimCol)) {
                priority.remove(i);
                switchingOff = (i == row);
                break;
            }
        }

        if (!switchingOff) {
            int emptyRow = findNextEmptyRow(row);
            priority.add(emptyRow, clickedDir);
        }

        sendPriorityToServer();
    }

    private int findNextEmptyRow(int start) {
        while (start-- > 0) {
            if (start < distributor.getPriority().size()) return start + 1;
        }
        return 0;
    }

    private static int findColumnForDirection(Direction dir) {
        for (int i = 0; i < BUTTON_DIRECTIONS.length; i++) {
            if (BUTTON_DIRECTIONS[i] == dir) return i;
        }
        return -1;
    }

    private void sendPriorityToServer() {
        List<Integer> priorities = new ArrayList<>();
        for (Direction dir : distributor.getPriority()) {
            priorities.add(dir.get3DDataValue());
        }
        PacketDistributor.sendToServer(new WeightedDistributorPriorityPacket(distributor.getBlockPos(), priorities));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 6; col++) {
                int btnX = x + 63 + col * 18;
                int btnY = y + 17 + row * 18;

                Direction facing = BUTTON_DIRECTIONS[col];
                boolean isFacing = (distributor != null && distributor.getFacing() == facing);
                boolean isOn = isButtonOn(row, col);

                if (isFacing) {
                    guiGraphics.fill(btnX, btnY, btnX + 16, btnY + 16, 0x40404040);
                } else if (isOn) {
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
}

