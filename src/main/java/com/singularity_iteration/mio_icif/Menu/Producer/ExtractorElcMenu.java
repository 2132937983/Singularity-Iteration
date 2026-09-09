package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_extrator_elc;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 提取得?Menu
 */
@SuppressWarnings("null")
public class ExtractorElcMenu extends mio_icif_machine_menu {

    // 槽位索引
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int BATTERY_SLOT = 2;
    public static final int UPGRADE_SLOT_START = 3;
    public static final int SLOT_COUNT = 7;

    // 槽位位置
    private static final int INPUT_X = 56;
    private static final int INPUT_Y = 17;
    private static final int BATTERY_X = 56;
    private static final int BATTERY_Y = 53;
    private static final int OUTPUT_X = 116;
    private static final int OUTPUT_Y = 35;
    private static final int[] UPGRADE_SLOT_X = {152, 152, 152, 152};
    private static final int[] UPGRADE_SLOT_Y = {8, 26, 44, 62};

    private final ContainerLevelAccess access;
    public final mio_icif_extrator_elc blockEntity;

    public ExtractorElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public ExtractorElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_extrator_elc blockEntity) {
        super(mio_icif_menus.EXTRACTOR_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT,
            playerInventory,
            blockEntity != null ? (IItemHandler) blockEntity.getItemHandler() : null,
            null, 5, blockEntity);

        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;

        registerMachineMoveRules();
    }

    @Override
    protected void addMachineSlots() {
        addInputSlot(INPUT_SLOT, INPUT_X, INPUT_Y);
        this.addSlot(new LargeSlot(itemHandler, OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y, 26, 26));
        addBatterySlot(BATTERY_SLOT, BATTERY_X, BATTERY_Y);
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
    protected boolean isValidInput(ItemStack stack) {
        if (blockEntity != null) {
            return blockEntity.isItemValidForSlot(INPUT_SLOT, stack);
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) ->
            level.getBlockState(pos).getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_extrator_elc
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

    /**
     * 大尺寸槽位类�?6x26�?
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