package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_powder_advanced_elc;
import com.miophas.singularity_iteration.core.api.machine.ISlotLayout;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class PowderAdvancedElcMenu extends mio_icif_machine_menu {

    public static final int SLOT_COUNT = 7;

    private static final int INPUT_X = 10;
    private static final int INPUT_Y = 18;
    private static final int BATTERY_X = 10;
    private static final int BATTERY_Y = 38;
    private static final int OUTPUT_X = 10;
    private static final int OUTPUT_Y = 58;
    private static final int UPGRADE_X = 152;
    private static final int[] UPGRADE_Y = {8, 26, 44, 62};

    private final ContainerLevelAccess access;
    public final mio_icif_powder_advanced_elc blockEntity;

    public PowderAdvancedElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public PowderAdvancedElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_powder_advanced_elc blockEntity) {
        super(mio_icif_menus.POWDER_ADVANCED_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT,
            playerInventory,
            blockEntity != null ? (IItemHandler) blockEntity.getItemHandler() : null,
            null, DATA_COUNT, blockEntity);

        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;

        registerMachineMoveRules();
    }

    /**
     * 0.1.11.109: data 5 rotor speed ‰, 6 hold speed on, 7 held now (the first five as before); 0.1.11.110: 8 status
     * (ProcessStatus), 9 EU per operation, 10 work index x10.
     */
    public static final int DATA_COUNT = 11;

    public int getStatus() { return data.get(8); }
    public int getEnergyPerOperation() { return data.get(9); }
    public int getWorkIndexTenths() { return data.get(10); }
    /** 0.1.11.109: menu button that switches hold speed. */
    public static final int BUTTON_HOLD_SPEED = 0;

    private ISlotLayout getSlotLayout() {
        return mio_icif_powder_advanced_elc.getOrCreateLayout();
    }

    public int getRotorSpeedPermille() { return data.get(5); }
    public boolean isHoldSpeedOn() { return data.get(6) != 0; }
    public boolean isHoldingNow() { return data.get(7) != 0; }

    @Override
    public boolean clickMenuButton(net.minecraft.world.entity.player.Player player, int id) {
        if (id != BUTTON_HOLD_SPEED) return false;
        if (blockEntity != null) blockEntity.toggleHoldSpeed();
        return true;
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
            level.getBlockState(pos).getBlock() instanceof com.miophas.singularity_iteration.common.block.producer.mio_icif_block_powder_advanced_elc
                && player.distanceToSqr((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5) <= 64.0,
            true);
    }


    // ---- 0.1.11.110: the instrument screen layout (248 x 204; docs/gui_processing_machines.md) ----

    public static final int GUI_WIDTH = 248, GUI_HEIGHT = 204, INVENTORY_X = 44, INVENTORY_Y = 124, HOTBAR_Y = 182;

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
            this.setSyncData(5, (int) Math.round(blockEntity.getRotorSpeed() * 1000));
            this.setSyncData(6, blockEntity.isHoldingSpeed() ? 1 : 0);
            this.setSyncData(7, blockEntity.isHolding() ? 1 : 0);
            this.setSyncData(8, blockEntity.processStatus());
            this.setSyncData(9, (int) Math.min(Short.MAX_VALUE, blockEntity.processCost().energy()));
            this.setSyncData(10, blockEntity.workIndexTenths());
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