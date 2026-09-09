package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_washer_elc;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 洗矿石?Menu
 */
@SuppressWarnings("null")
public class WasherElcMenu extends mio_icif_machine_menu {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT_1 = 1;
    public static final int OUTPUT_SLOT_2 = 2;
    public static final int OUTPUT_SLOT_3 = 3;
    public static final int BATTERY_SLOT = 4;
    public static final int WATER_BUCKET_SLOT = 5;
    public static final int EMPTY_BUCKET_SLOT = 6;
    public static final int UPGRADE_SLOT_START = 7;
    public static final int SLOT_COUNT = 11;

    private static final int INPUT_X = 104;
    private static final int INPUT_Y = 17;
    private static final int[] OUTPUT_X = {86, 104, 122};
    private static final int OUTPUT_Y = 62;
    private static final int BATTERY_X = 8;
    private static final int BATTERY_Y = 62;
    private static final int WATER_BUCKET_X = 38;
    private static final int WATER_BUCKET_Y = 17;
    private static final int EMPTY_BUCKET_X = 38;
    private static final int EMPTY_BUCKET_Y = 62;
    private static final int UPGRADE_X = 152;
    private static final int[] UPGRADE_Y = {8, 26, 44, 62};

    private final ContainerLevelAccess access;
    public final mio_icif_washer_elc blockEntity;

    public WasherElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public WasherElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_washer_elc blockEntity) {
        super(mio_icif_menus.WASHER_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT,
            playerInventory,
            blockEntity != null ? (IItemHandler) blockEntity.getItemHandler() : null,
            null, 7, blockEntity);

        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;

        addCustomMoveRule(
            stack -> stack.is(Items.WATER_BUCKET) || mio_icif_cells.isCellContainingFluid(stack, Fluids.WATER),
            WATER_BUCKET_SLOT, WATER_BUCKET_SLOT + 1);
        registerMachineMoveRules();
    }

    @Override
    protected void addMachineSlots() {
        addInputSlot(INPUT_SLOT, INPUT_X, INPUT_Y);
        for (int i = 0; i < 3; i++) {
            addOutputSlot(OUTPUT_SLOT_1 + i, OUTPUT_X[i], OUTPUT_Y);
        }
        addBatterySlot(BATTERY_SLOT, BATTERY_X, BATTERY_Y);
        this.addSlot(new SlotItemHandler(itemHandler, WATER_BUCKET_SLOT, WATER_BUCKET_X, WATER_BUCKET_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                boolean isWaterContainer = stack.is(Items.WATER_BUCKET) || mio_icif_cells.isCellContainingFluid(stack, Fluids.WATER);
                if (!isWaterContainer) return false;
                if (blockEntity != null && blockEntity.getFluidAmount() >= blockEntity.getFluidCapacity()) {
                    return false;
                }
                return true;
            }
        });
        addOutputSlot(EMPTY_BUCKET_SLOT, EMPTY_BUCKET_X, EMPTY_BUCKET_Y);
        for (int i = 0; i < 4; i++) {
            addUpgradeSlot(UPGRADE_SLOT_START + i, UPGRADE_X, UPGRADE_Y[i]);
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
        return OUTPUT_SLOT_1;
    }

    @Override
    protected int getOutputSlotCount() {
        return 3;
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

    public int getFluidAmount() {
        return data.get(5);
    }

    public int getFluidCapacity() {
        return data.get(6);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) ->
            level.getBlockState(pos).getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_washer_elc
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
            this.setSyncData(5, blockEntity.getFluidAmount());
            this.setSyncData(6, blockEntity.getFluidCapacity());
        }
    }
}