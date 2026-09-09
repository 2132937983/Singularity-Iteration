package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_metal_former;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_metal_former;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 金属成型�?Menu
 */
@SuppressWarnings("null")
public class MetalFormerMenu extends mio_icif_machine_menu {

    public static final int INPUT_SLOT = 0;
    public static final int BATTERY_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int PLUGIN_SLOT_1 = 3;
    public static final int PLUGIN_SLOT_COUNT = 4;
    public static final int SLOT_COUNT = 7;

    private static final int INPUT_X = 17;
    private static final int INPUT_Y = 17;
    private static final int BATTERY_X = 17;
    private static final int BATTERY_Y = 53;
    private static final int OUTPUT_X = 116;
    private static final int OUTPUT_Y = 35;

    // 插件槽（空闲槽）位置
    private static final int PLUGIN_X = 152;
    private static final int PLUGIN_Y_START = 8;
    private static final int PLUGIN_SLOT_SPACING = 18;

    private final ContainerLevelAccess access;

    public MetalFormerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public MetalFormerMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_metal_former blockEntity) {
        super(mio_icif_menus.METAL_FORMER_MENU_TYPE.get(), containerId, SLOT_COUNT,
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
        // 输入�?
        addInputSlot(INPUT_SLOT, INPUT_X, INPUT_Y);

        // 电池�?
        addBatterySlot(BATTERY_SLOT, BATTERY_X, BATTERY_Y);

        // 输出�?
        addOutputSlot(OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y);

        for (int i = 0; i < PLUGIN_SLOT_COUNT; i++) {
            addUpgradeSlot(itemHandler, PLUGIN_SLOT_1 + i, PLUGIN_X, PLUGIN_Y_START + i * PLUGIN_SLOT_SPACING);
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
        return PLUGIN_SLOT_1;
    }

    @Override
    protected int getUpgradeSlotCount() {
        return PLUGIN_SLOT_COUNT;
    }

    @Override
    public mio_icif_metal_former getBlockEntity() {
        return this.blockEntity instanceof mio_icif_metal_former be ? be : null;
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        var be = getBlockEntity();
        if (be != null) {
            return be.isItemValidForSlot(INPUT_SLOT, stack);
        }
        return true;
    }

    public mio_icif_block_metal_former.MetalFormerMode getMode() {
        int modeIndex = data.get(5);
        return mio_icif_block_metal_former.MetalFormerMode.values()[modeIndex % 3];
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) ->
            level.getBlockState(pos).getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_metal_former
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
            this.setSyncData(5, be.getMode().ordinal());
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        var be = getBlockEntity();
        if (be == null || id != 0) {
            return false;
        }

        // 获取当前模式并切换到下一�?
        mio_icif_metal_former.MetalFormerMode currentMode = be.getMode();
        mio_icif_metal_former.MetalFormerMode nextMode = switch (currentMode) {
            case ROLLING -> mio_icif_metal_former.MetalFormerMode.CUTTING;
            case CUTTING -> mio_icif_metal_former.MetalFormerMode.EXTRUDING;
            case EXTRUDING -> mio_icif_metal_former.MetalFormerMode.ROLLING;
        };

        // 设置新模式?
        be.setMode(nextMode);

        // 更新方块状态?
        if (be.getLevel() != null) {
            be.getLevel().setBlockAndUpdate(be.getBlockPos(),
                be.getBlockState().setValue(mio_icif_block_metal_former.MODE,
                    mio_icif_block_metal_former.MetalFormerMode.valueOf(nextMode.name())));
            be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }

        return true;
    }
}