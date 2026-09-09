package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_electrolyzer_elc;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 电解�?Menu
 */
@SuppressWarnings("null")
public class ElectrolyzerElcMenu extends mio_icif_machine_menu {

    // 槽位索引
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 2;

    // 槽位位置
    private static final int INPUT_X = 54;
    private static final int INPUT_Y = 35;
    private static final int OUTPUT_X = 112;
    private static final int OUTPUT_Y = 35;

    private final ContainerLevelAccess access;

    public ElectrolyzerElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public ElectrolyzerElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_electrolyzer_elc blockEntity) {
        super(mio_icif_menus.ELECTROLYZER_MENU_TYPE.get(), containerId, SLOT_COUNT,
            playerInventory,
            blockEntity != null ? (IItemHandler) blockEntity.getItemHandler() : null,
            null, 6, blockEntity);

        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());

        // 注册快速移动规则?
        registerMachineMoveRules();
    }

    @Override
    protected void addMachineSlots() {
        // 输入槽（水单元）
        addInputSlot(INPUT_SLOT, INPUT_X, INPUT_Y);

        // 输出槽（空单元）
        addOutputSlot(OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y);
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
        return -1;
    }

    @Override
    protected int getUpgradeSlotStart() {
        return -1;
    }

    @Override
    protected int getUpgradeSlotCount() {
        return 0;
    }

    @Override
    public mio_icif_electrolyzer_elc getBlockEntity() {
        return this.blockEntity instanceof mio_icif_electrolyzer_elc be ? be : null;
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        var be = getBlockEntity();
        if (be != null) {
            return be.canPlaceItem(INPUT_SLOT, stack);
        }
        return true;
    }

    public int getStatus() {
        return data.get(5);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) ->
            level.getBlockState(pos).getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_electrolyzer_elc
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
            int status = 0;
            if (be.isCharging()) status = 1;
            else if (be.isDischarging()) status = 2;
            this.setSyncData(5, status);
        }
    }
}

