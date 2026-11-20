package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_compressor_elc;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Menu for the compressor.
 */
@SuppressWarnings("null")
public class CompressorElcMenu extends mio_icif_machine_menu {

    // 槽位索引
    public static final int INPUT_SLOT = 0;
    public static final int BATTERY_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int UPGRADE_SLOT_START = 4;
    public static final int SLOT_COUNT = 8;

    // 槽位位置
    private static final int INPUT_X = 10;
    private static final int INPUT_Y = 18;
    private static final int BATTERY_X = 10;
    private static final int BATTERY_Y = 56;   // 0.1.11.118: the furnace block (bolt between input and battery)
    private static final int OUTPUT_X = 61;
    private static final int OUTPUT_Y = 37;
    private static final int[] UPGRADE_SLOT_X = {152, 152, 152, 152};
    private static final int[] UPGRADE_SLOT_Y = {8, 26, 44, 62};

    private final ContainerLevelAccess access;

    public CompressorElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public CompressorElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_compressor_elc blockEntity) {
        super(mio_icif_menus.COMPRESSOR_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT,
            playerInventory,
            blockEntity != null ? (IItemHandler) blockEntity.getItemHandler() : null,
            null, DATA_COUNT, blockEntity);

        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());

        // Register the quick-move rules.
        registerMachineMoveRules();
    }

    @Override
    protected void addMachineSlots() {
        // Input slot
        addInputSlot(INPUT_SLOT, INPUT_X, INPUT_Y);

        // Battery slot
        addBatterySlot(BATTERY_SLOT, BATTERY_X, BATTERY_Y);

        // Output slot (large slot)
        addOutputSlot(OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y);

        // 4个升级槽
        for (int i = 0; i < 4; i++) {
            addUpgradeSlot(UPGRADE_SLOT_START + i, UPGRADE_SLOT_X[i], UPGRADE_SLOT_Y[i]);
        }
    }

    @Override
    protected int getInputSlotStart() {
        return INPUT_SLOT;
    }

    @Override
    protected int getInputSlotCount() {
        return 1;
    }

    @Override
    protected int getOutputSlotStart() {
        return OUTPUT_SLOT;
    }

    @Override
    protected int getOutputSlotCount() {
        return 1;
    }

    @Override
    protected int getBatterySlotIndex() {
        return BATTERY_SLOT;
    }

    @Override
    protected int getUpgradeSlotStart() {
        return UPGRADE_SLOT_START;
    }

    @Override
    protected int getUpgradeSlotCount() {
        return 4;
    }

    @Override
    public mio_icif_compressor_elc getBlockEntity() {
        return this.blockEntity instanceof mio_icif_compressor_elc be ? be : null;
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        var be = getBlockEntity();
        if (be != null) {
            return be.isItemValidForSlot(INPUT_SLOT, stack);
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) ->
            level.getBlockState(pos).getBlock() instanceof com.miophas.singularity_iteration.common.block.producer.mio_icif_block_compressor_elc
                && player.distanceToSqr((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5) <= 64.0,
            true);
    }

    /** 0.1.11.112: data 5 status (ProcessStatus), 6 EU per pressing, 7 EU/t, 8 stored energy ‰ (the first five as before). */
    public static final int DATA_COUNT = 9;

    public int getStatus() { return data.get(5); }
    public int getEnergyPerOperation() { return data.get(6); }
    public int getPower() { return data.get(7); }
    public int getEnergyPermille() { return data.get(8); }

    // ---- 0.1.11.112: the instrument screen layout (248 x 204; docs/gui_processing_machines.md) ----

    public static final int GUI_WIDTH = 248, GUI_HEIGHT = 240, INVENTORY_X = 44, INVENTORY_Y = 160, HOTBAR_Y = 218;

    @Override
    protected int getPlayerInventoryY() { return INVENTORY_Y; }

    @Override
    protected int getPlayerHotbarY() { return HOTBAR_Y; }

    @Override
    protected void addPlayerInventory(Inventory playerInventory, int startY, int hotbarY) {
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j)
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, j + i * 9 + 9, INVENTORY_X + j * 18, startY + i * 18));
        for (int i = 0; i < 9; ++i) this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, i, INVENTORY_X + i * 18, hotbarY));
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        var be = getBlockEntity();
        if (be != null) {
            this.setSyncData(0, be.getProgress());
            this.setSyncData(1, be.getMaxProgress());
            this.setSyncData(2, be.isWorking() ? 1 : 0);
            int energy = (int) be.getEnergyStorage().getAmount();
            int maxEnergy = (int) be.getEnergyStorage().getCapacity();
            this.setSyncData(3, energy);
            this.setSyncData(4, maxEnergy);
            if (be instanceof mio_icif_compressor_elc c) {
                var cost = c.processCost();
                this.setSyncData(5, c.processStatus());
                this.setSyncData(6, (int) Math.min(Short.MAX_VALUE, cost.energy()));
                this.setSyncData(7, (int) Math.min(Short.MAX_VALUE, cost.power()));
                this.setSyncData(8, (int) Math.round(1000.0 * be.getEnergyStorage().getAmount() / Math.max(1, be.getEnergyStorage().getCapacity())));
            }
        }
    }

    /**
     * Large slot type (26x26).
     */
    public static class LargeSlot extends SlotItemHandler {
        @SuppressWarnings("unused")
        private final int slotWidth;
        @SuppressWarnings("unused")
        private final int slotHeight;

        public LargeSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition, int width, int height) {
            super(itemHandler, index, xPosition, yPosition);
            this.slotWidth = width;
            this.slotHeight = height;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}