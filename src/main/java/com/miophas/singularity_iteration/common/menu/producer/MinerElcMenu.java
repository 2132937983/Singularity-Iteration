package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_miner_elc;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_iron_driller;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_diamond_driller;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_iridium_driller;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_od_scanner;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_ov_scanner;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Menu for the miner.
 */
@SuppressWarnings("null")
public class MinerElcMenu extends mio_icif_base_menu {

    // 槽位索引定义
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_UPGRADE = 1;
    public static final int SLOT_STORAGE_START = 2;
    public static final int SLOT_STORAGE_COUNT = 15;
    public static final int SLOT_STORAGE_END = SLOT_STORAGE_START + SLOT_STORAGE_COUNT;
    public static final int SLOT_DRILL = 17;
    public static final int SLOT_PIPE = 18;
    public static final int SLOT_SCANNER = 19;
    public static final int TOTAL_SLOTS = 20;

    // 槽位位置
    private static final int DRILL_X = 8;
    private static final int DRILL_Y = 22;
    private static final int PIPE_X = 8;
    private static final int PIPE_Y = 40;
    private static final int SCANNER_X = 8;
    private static final int SCANNER_Y = 58;
    private static final int STORAGE_START_X = 44;
    private static final int STORAGE_START_Y = 22;
    private static final int UPGRADE_X = 152;
    private static final int UPGRADE_Y = 22;
    private static final int BATTERY_X = 152;
    private static final int BATTERY_Y = 58;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final IItemHandler itemHandler;
    public final mio_icif_miner_elc blockEntity;

    public MinerElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public MinerElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_miner_elc blockEntity) {
        super(mio_icif_menus.MINER_ELC_MENU_TYPE.get(), containerId, TOTAL_SLOTS, TOTAL_SLOTS);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;
        this.itemHandler = blockEntity != null ? (IItemHandler) blockEntity.getItemHandler() : new ItemStackHandler(TOTAL_SLOTS);

        // Synced data: 0=progress, 1=maxProgress, 2=isWorking, 3=energy, 4=maxEnergy, 5=retract mode, 6=status (0.1.10.36)
        this.data = new ContainerData() {
            private final int[] data = new int[7];

            @Override
            public int get(int index) {
                return data[index];
            }

            @Override
            public void set(int index, int value) {
                data[index] = value;
            }

            @Override
            public int getCount() {
                return 7;
            }
        };
        this.addDataSlots(data);

        // Battery slot
        this.addSlot(new SlotItemHandler(itemHandler, SLOT_BATTERY, BATTERY_X, BATTERY_Y) {
            @Override
            public int getMaxStackSize(ItemStack stack) {
                if (stack.getItem() == net.minecraft.world.item.Items.REDSTONE) return 64;
                return 1;
            }
        });

        addUpgradeSlot(itemHandler, SLOT_UPGRADE, UPGRADE_X, UPGRADE_Y);

        // Drill slot
        this.addSlot(new SlotItemHandler(itemHandler, SLOT_DRILL, DRILL_X, DRILL_Y));

        // 扫描器槽
        this.addSlot(new SlotItemHandler(itemHandler, SLOT_SCANNER, SCANNER_X, SCANNER_Y));

        // Mining pipe slot
        this.addSlot(new SlotItemHandler(itemHandler, SLOT_PIPE, PIPE_X, PIPE_Y));

        // Storage slots (3 rows x 5 columns)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                int slotIndex = SLOT_STORAGE_START + row * 5 + col;
                int x = STORAGE_START_X + col * 18;
                int y = STORAGE_START_Y + row * 18;
                this.addSlot(new SlotItemHandler(itemHandler, slotIndex, x, y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false; // Players cannot put items into the storage slots manually
                    }
                });
            }
        }

        // Register custom quick-move rules.
        addCustomMoveRule(mio_icif_miner_elc::isDrill, SLOT_DRILL, SLOT_DRILL + 1);   // 0.1.10.36: the Mining Laser too
        addCustomMoveRule(
            stack -> stack.getItem() instanceof mio_icif_od_scanner ||
                     stack.getItem() instanceof mio_icif_ov_scanner,
            SLOT_SCANNER, SLOT_SCANNER + 1);
        addCustomMoveRule(
            stack -> stack.is(com.miophas.singularity_iteration.common.registry.mio_icif_blocks.BLOCK_MINING_PIPE.get().asItem()),
            SLOT_PIPE, SLOT_PIPE + 1);

        // Player inventory (3 rows x 9 columns). This follows the original IC2 layout: height=166, inventoryYOffset=-82, hotbarYOffset=-24.
        // First inventory row Y = 166 - 82 = 84. Hotbar Y = 166 - 24 = 142.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar (1 row x 9 columns)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return MioIcifAPI.instance().getItemAPI().isBattery(stack)
            || stack.getItem() == net.minecraft.world.item.Items.REDSTONE;
    }

    @Override
    protected int getBatterySlotIndex() {
        return SLOT_BATTERY;
    }

    @Override
    protected int getUpgradeSlotStart() {
        return SLOT_UPGRADE;
    }

    @Override
    protected int getUpgradeSlotCount() {
        return 1;
    }

    public int getProgressPixels(int maxWidth) {
        int progress = getProgress();
        int maxProgress = getMaxProgress();
        if (maxProgress == 0) return 0;
        return (progress * maxWidth) / maxProgress;
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getMaxProgress() {
        return data.get(1);
    }

    public boolean isWorking() {
        return data.get(2) == 1;
    }

    public int getEnergy() {
        return data.get(3);
    }

    public int getMaxEnergy() {
        return data.get(4);
    }

    /** 0.1.10.36: the Retract button is on. */
    public boolean retractMode() {
        return data.get(5) == 1;
    }

    /** 0.1.10.36: {@link mio_icif_miner_elc#status()}. */
    public int status() {
        return data.get(6);
    }

    /** Button 0: the Retract button (pull the pipes up / mine again). */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || blockEntity == null || !stillValid(player)) return false;
        blockEntity.toggleRetract();
        return true;
    }

    /**
     * Sets a synced data value. The Screen calls this from containerTick.
     */
    public void setSyncData(int index, int value) {
        if (index >= 0 && index < data.getCount()) {
            data.set(index, value);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) ->
            level.getBlockState(pos).getBlock() instanceof com.miophas.singularity_iteration.common.block.producer.mio_icif_block_miner_elc
                && player.distanceToSqr((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5) <= 64.0,
            true);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        // 同步数据到客户端
        if (blockEntity != null) {
            this.data.set(0, blockEntity.getProgress());
            this.data.set(1, blockEntity.getMaxProgress());
            this.data.set(2, blockEntity.isWorking() ? 1 : 0);
            this.data.set(3, (int) blockEntity.getEnergyStorage().getAmount());
            this.data.set(4, (int) blockEntity.getEnergyStorage().getCapacity());
            this.data.set(5, blockEntity.retractMode() ? 1 : 0);
            this.data.set(6, blockEntity.status());
        }
    }
}