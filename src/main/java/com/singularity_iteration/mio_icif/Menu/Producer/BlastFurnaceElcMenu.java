package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_blast_furnace_elc;
import com.singularity_iteration.mio_icif.api.machine.ISlotLayout;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class BlastFurnaceElcMenu extends mio_icif_machine_menu {

    public static final int SLOT_COUNT = 7;

    private static final int INPUT_X = 56;
    private static final int INPUT_Y = 17;
    private static final int BATTERY_X = 56;
    private static final int BATTERY_Y = 53;
    private static final int OUTPUT_X = 116;
    private static final int OUTPUT_Y = 35;
    private static final int UPGRADE_X = 152;
    private static final int[] UPGRADE_Y = {8, 26, 44, 62};

    private final ContainerLevelAccess access;
    public final mio_icif_blast_furnace_elc blockEntity;

    public BlastFurnaceElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public BlastFurnaceElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_blast_furnace_elc blockEntity) {
        super(mio_icif_menus.BLAST_FURNACE_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT,
            playerInventory,
            blockEntity != null ? (IItemHandler) blockEntity.getItemHandler() : null,
            null, 5, blockEntity);

        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;

        registerMachineMoveRules();
    }

    private ISlotLayout getSlotLayout() {
        return blockEntity != null ? blockEntity.getSlotLayout() : mio_icif_blast_furnace_elc.getOrCreateLayout();
    }

    @Override
    protected void addMachineSlots() {
        ISlotLayout layout = getSlotLayout();
        int inputSlot = layout.getInputSlots()[0];
        int batterySlot = layout.getBatterySlots()[0];
        int[] outputSlots = layout.getOutputSlots();
        int[] upgradeSlots = layout.getUpgradeSlots();

        addInputSlot(inputSlot, INPUT_X, INPUT_Y);
        addBatterySlot(batterySlot, BATTERY_X, BATTERY_Y);
        this.addSlot(new LargeSlot(itemHandler, outputSlots[0], OUTPUT_X, OUTPUT_Y, 26, 26));
        for (int i = 0; i < upgradeSlots.length; i++) {
            addUpgradeSlot(upgradeSlots[i], UPGRADE_X, UPGRADE_Y[i]);
        }
    }

    @Override
    protected int getInputSlotStart() {
        return getSlotLayout().getInputSlots()[0];
    }

    @Override
    protected int getInputSlotCount() {
        return getSlotLayout().getInputCount();
    }

    @Override
    protected int getOutputSlotStart() {
        return getSlotLayout().getOutputSlots()[0];
    }

    @Override
    protected int getOutputSlotCount() {
        return getSlotLayout().getOutputCount();
    }

    @Override
    protected int getBatterySlotIndex() {
        return getSlotLayout().getBatterySlots()[0];
    }

    @Override
    protected int getUpgradeSlotStart() {
        return getSlotLayout().getUpgradeSlots()[0];
    }

    @Override
    protected int getUpgradeSlotCount() {
        return getSlotLayout().getUpgradeCount();
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        if (blockEntity != null) {
            return blockEntity.isItemValidForSlot(getSlotLayout().getInputSlots()[0], stack);
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) ->
            level.getBlockState(pos).getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_blast_furnace_elc
                && player.distanceToSqr((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5) <= 64.0,
            true);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity != null) {
            this.setSyncData(0, blockEntity.getProgress());
            this.setSyncData(1, blockEntity.getMaxProgress());
            this.setSyncData(2, blockEntity.isWorking() ? 1 : 0);
            int energy = (int) blockEntity.getEnergyStorage().getAmount();
            int maxEnergy = (int) blockEntity.getEnergyStorage().getCapacity();
            this.setSyncData(3, energy);
            this.setSyncData(4, maxEnergy);
        }
    }

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