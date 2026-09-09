package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_metal_former_advanced;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import com.singularity_iteration.mio_icif.api.machine.IMachineAPI;
import com.singularity_iteration.mio_icif.api.machine.ISlotLayout;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class MetalFormerAdvancedMenu extends mio_icif_machine_menu {

    public static final int SLOT_COUNT = 7;

    private static final int INPUT_X = 17;
    private static final int INPUT_Y = 17;
    private static final int BATTERY_X = 17;
    private static final int BATTERY_Y = 53;
    private static final int OUTPUT_X = 116;
    private static final int OUTPUT_Y = 35;

    private static final int UPGRADE_X = 152;
    private static final int[] UPGRADE_Y = {8, 26, 44, 62};

    private final ContainerLevelAccess access;

    public MetalFormerAdvancedMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public MetalFormerAdvancedMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_metal_former_advanced blockEntity) {
        super(mio_icif_menus.METAL_FORMER_ADVANCED_MENU_TYPE.get(), containerId, SLOT_COUNT,
            playerInventory,
            blockEntity != null ? (IItemHandler) blockEntity.getItemHandler() : null,
            null, 6, blockEntity);

        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());

        registerMachineMoveRules();
    }

    private ISlotLayout getSlotLayout() {
        var be = getBlockEntity();
        return be != null ? be.getSlotLayout() : mio_icif_metal_former_advanced.getOrCreateLayout();
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
    public mio_icif_metal_former_advanced getBlockEntity() {
        return this.blockEntity instanceof mio_icif_metal_former_advanced be ? be : null;
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        var be = getBlockEntity();
        if (be != null) {
            return be.isItemValidForSlot(getSlotLayout().getInputSlots()[0], stack);
        }
        return true;
    }

    public IMachineAPI.MetalFormerMode getMode() {
        int modeIndex = data.get(5);
        return IMachineAPI.MetalFormerMode.fromId(modeIndex % 3);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) ->
            level.getBlockEntity(pos) instanceof mio_icif_metal_former_advanced
                && player.distanceToSqr((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5) <= 64.0,
            true);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        var be = getBlockEntity();
        if (be != null) {
            this.setSyncData(0, be.getProgress());
            this.setSyncData(1, be.getMaxProgress());
            this.setSyncData(2, be.isWorking() ? 1 : 0);
            this.setSyncData(3, (int) be.getEnergyStorage().getAmount());
            this.setSyncData(4, (int) be.getEnergyStorage().getCapacity());
            this.setSyncData(5, be.getMode().getId());
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        var be = getBlockEntity();
        if (be == null || id != 0) {
            return false;
        }

        IMachineAPI.MetalFormerMode currentMode = be.getMode();
        IMachineAPI.MetalFormerMode nextMode = switch (currentMode) {
            case ROLLING -> IMachineAPI.MetalFormerMode.CUTTING;
            case CUTTING -> IMachineAPI.MetalFormerMode.EXTRUDING;
            case EXTRUDING -> IMachineAPI.MetalFormerMode.ROLLING;
        };

        be.setMode(nextMode);

        return true;
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