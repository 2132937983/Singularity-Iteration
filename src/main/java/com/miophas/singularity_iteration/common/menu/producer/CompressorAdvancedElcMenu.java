package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.menu.MergedSlots;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_compressor_advanced_elc;
import com.miophas.singularity_iteration.core.api.machine.ISlotLayout;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class CompressorAdvancedElcMenu extends mio_icif_machine_menu {

    public static final int SLOT_COUNT = 7;

    private static final int INPUT_X = MergedSlots.Press.IN_X;
    private static final int INPUT_Y = MergedSlots.Press.IN_Y;
    private static final int BATTERY_X = MergedSlots.BATTERY_X;
    private static final int BATTERY_Y = MergedSlots.BATTERY_Y;   // 0.1.11.118: the furnace block (bolt between input and battery)
    private static final int OUTPUT_X = MergedSlots.Press.OUT_X;
    private static final int OUTPUT_Y = MergedSlots.Press.OUT_Y;
    private static final int UPGRADE_X = 152;
    private static final int[] UPGRADE_Y = {8, 26, 44, 62};

    private final ContainerLevelAccess access;
    public final mio_icif_compressor_advanced_elc blockEntity;

    public CompressorAdvancedElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public CompressorAdvancedElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_compressor_advanced_elc blockEntity) {
        super(mio_icif_menus.COMPRESSOR_ADVANCED_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT,
            playerInventory,
            blockEntity != null ? (IItemHandler) blockEntity.getItemHandler() : null,
            null, DATA_COUNT, blockEntity);

        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;

        registerMachineMoveRules();
    }

    private ISlotLayout getSlotLayout() {
        return blockEntity != null ? blockEntity.getSlotLayout() : mio_icif_compressor_advanced_elc.getOrCreateLayout();
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
        addOutputSlot(outputSlots[0], OUTPUT_X, OUTPUT_Y);
        for (int i = 0; i < upgradeSlots.length; i++) {
            addUpgradeSlot(upgradeSlots[i], UPGRADE_X, UPGRADE_Y[i]);
        }
    }

    @Override
    protected int getInputSlotStart() { return getSlotLayout().getInputSlots()[0]; }

    @Override
    protected int getInputSlotCount() { return getSlotLayout().getInputCount(); }

    @Override
    protected int getOutputSlotStart() { return getSlotLayout().getOutputSlots()[0]; }

    @Override
    protected int getOutputSlotCount() { return getSlotLayout().getOutputCount(); }

    @Override
    protected int getBatterySlotIndex() { return getSlotLayout().getBatterySlots()[0]; }

    @Override
    protected int getUpgradeSlotStart() { return getSlotLayout().getUpgradeSlots()[0]; }

    @Override
    protected int getUpgradeSlotCount() { return getSlotLayout().getUpgradeCount(); }

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
            level.getBlockState(pos).getBlock() instanceof com.miophas.singularity_iteration.common.block.producer.mio_icif_block_compressor_advanced_elc
                && player.distanceToSqr((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5) <= 64.0,
            true);
    }

    /**
     * 0.1.11.112: data 5 status (ProcessStatus), 6 EU per stroke, 7 stroke EU/t, 8 oil pressure MPa x10, 9 pressure the
     * next stroke needs MPa x10 (the first five as before).
     */
    public static final int DATA_COUNT = 10;

    public int getStatus() { return data.get(5); }
    public int getEnergyPerOperation() { return data.get(6); }
    public int getPower() { return data.get(7); }
    public double getPressureMpa() { return data.get(8) / 10.0; }
    public double getRequiredPressureMpa() { return data.get(9) / 10.0; }

    // ---- 0.1.11.112: the instrument screen layout (248 x 204; docs/gui_processing_machines.md) ----

    public static final int GUI_WIDTH = 176, GUI_HEIGHT = 240, INVENTORY_X = 8, INVENTORY_Y = 160, HOTBAR_Y = 218;   // 0.1.11.180: the vanilla width

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
        if (blockEntity != null) {
            this.setSyncData(0, blockEntity.getProgress());
            this.setSyncData(1, blockEntity.getMaxProgress());
            this.setSyncData(2, blockEntity.isWorking() ? 1 : 0);
            int energy = (int) blockEntity.getEnergyStorage().getAmount();
            int maxEnergy = (int) blockEntity.getEnergyStorage().getCapacity();
            this.setSyncData(3, energy);
            this.setSyncData(4, maxEnergy);
            var cost = blockEntity.processCost();
            this.setSyncData(5, blockEntity.processStatus());
            this.setSyncData(6, (int) Math.min(Short.MAX_VALUE, cost.energy()));
            this.setSyncData(7, (int) Math.min(Short.MAX_VALUE, cost.power()));
            this.setSyncData(8, (int) Math.round(blockEntity.pressureMpa() * 10));
            this.setSyncData(9, (int) Math.round(blockEntity.requiredPressureMpa() * 10));
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