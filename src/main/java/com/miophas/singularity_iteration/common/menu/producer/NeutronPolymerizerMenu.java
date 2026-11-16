package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_neutron_polymerizer;
import com.miophas.singularity_iteration.core.api.machine.ISlotLayout;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class NeutronPolymerizerMenu extends mio_icif_machine_menu {

    public static final int SLOT_COUNT = 7;

    /** 0.1.11.8: 248 x 212 with the implosion-photography window; input, energy and output on the left. */
    public static final int GUI_WIDTH = 248, GUI_HEIGHT = 212;
    public static final int INPUT_X = 10, INPUT_Y = 29;
    public static final int OUTPUT_X = 10, OUTPUT_Y = 81;
    public static final int BATTERY_X = 10, BATTERY_Y = 112;
    public static final int INVENTORY_X = 44, INVENTORY_Y = 130, HOTBAR_Y = 188;
    private static final int UPGRADE_X = 152;
    private static final int[] UPGRADE_Y = {8, 26, 44, 62};

    private final ContainerLevelAccess access;
    public final mio_icif_neutron_polymerizer blockEntity;

    public NeutronPolymerizerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public NeutronPolymerizerMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_neutron_polymerizer blockEntity) {
        super(mio_icif_menus.NEUTRON_POLYMERIZER_MENU_TYPE.get(), containerId, SLOT_COUNT,
            playerInventory,
            blockEntity != null ? (IItemHandler) blockEntity.getItemHandler() : null,
            blockEntity != null ? blockEntity.getContainerData() : null, mio_icif_neutron_polymerizer.DATA_COUNT, blockEntity);

        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;

        registerMachineMoveRules();
    }

    private ISlotLayout getSlotLayout() {
        return blockEntity != null ? blockEntity.getSlotLayout() : mio_icif_neutron_polymerizer.getOrCreateLayout();
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
        this.addSlot(new LargeSlot(itemHandler, outputSlots[0], OUTPUT_X, OUTPUT_Y, 16, 16));
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
            level.getBlockState(pos).getBlock() instanceof com.miophas.singularity_iteration.common.block.producer.mio_icif_block_neutron_polymerizer
                && player.distanceToSqr((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5) <= 64.0,
            true);
    }

    /** Synced value {@code i} (see {@code mio_icif_neutron_polymerizer.getContainerData}). */
    public int value(int i) {
        return this.data.get(i);
    }

    /** Button 0: the next chain target. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || blockEntity == null || !stillValid(player)) return false;
        return blockEntity.cycleTarget();
    }

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