package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_matter_elc;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * UU物质生成机的容器菜单 - 对齐原版IC2
 * 
 * 槽位布局（对齐原版）:
 * - 0: 增幅器槽 (废料槽) - (72, 40)
 * - 1: 输出槽 - (125, 59)
 * - 2: 容器槽 (空单元输入) - (125, 23)
 * - 3-6: 4个升级槽 - (152, 8 + i * 18)
 */
@SuppressWarnings({"null"}) 
public class MatterElcMenu extends mio_icif_machine_menu {

    public static final int AMPLIFIER_SLOT = 0;      // 增幅器槽（废料槽）
    public static final int OUTPUT_SLOT = 1;         // 输出槽
    public static final int CONTAINER_SLOT = 2;      // 容器槽（空单元输入）
    public static final int UPGRADE_SLOT_START = 3;  // 升级槽起始
    public static final int UPGRADE_SLOT_COUNT = 4;  // 升级槽数量
    public static final int SLOT_COUNT = 7;          // 总槽位数

    // 槽位位置（对齐原版IC2）
    private static final int AMPLIFIER_X = 72;
    private static final int AMPLIFIER_Y = 40;
    private static final int OUTPUT_X = 125;
    private static final int OUTPUT_Y = 59;
    private static final int CONTAINER_X = 125;
    private static final int CONTAINER_Y = 23;
    private static final int UPGRADE_X = 152;
    private static final int UPGRADE_START_Y = 8;

    // 数据槽索引
    private static final int DATA_PROGRESS = 0;      // 当前能量
    private static final int DATA_MAX_PROGRESS = 1;  // 最大能量容量
    private static final int DATA_ENERGY = 2;        // 当前能量（与progress相同，用于兼容）
    private static final int DATA_MAX_ENERGY = 3;    // 最大能量
    private static final int DATA_FLUID_AMOUNT = 4;  // UU物质液体量
    private static final int DATA_FLUID_CAPACITY = 5;// UU物质液体容量
    private static final int DATA_SCRAP = 6;         // 增幅器数值（废料值）
    private static final int DATA_COUNT = 7;

    public MatterElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public MatterElcMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public MatterElcMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.MATTER_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public MatterElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_matter_elc blockEntity) {
        super(mio_icif_menus.MATTER_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, DATA_COUNT, blockEntity);
    }

    // GUI高度166（对齐原版IC2），玩家物品栏从y=166开始
    // 背包Y = 166 - 82 = 84, 快捷栏Y = 166 - 24 = 142
    @Override
    protected int getPlayerInventoryY() { return 84; }

    @Override
    protected int getPlayerHotbarY() { return 142; }

    @Override
    protected void addMachineSlots() {
        // 增幅器槽（废料槽）- (72, 40)
        this.addSlot(new SlotItemHandler(itemHandler, AMPLIFIER_SLOT, AMPLIFIER_X, AMPLIFIER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(mio_icif_normal.SCRAP.get()) || stack.is(mio_icif_normal.SCRAPBOX.get()) || stack.is(mio_icif_resources.THORIUM_SCRAP.get());
            }
        });

        // 输出槽 - (125, 59)
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // 容器槽（空单元输入）- (125, 23)
        this.addSlot(new SlotItemHandler(itemHandler, CONTAINER_SLOT, CONTAINER_X, CONTAINER_Y));

        // 4个升级槽 - (152, 8 + i * 18)
        for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
            this.addUpgradeSlot(UPGRADE_SLOT_START + i, UPGRADE_X, UPGRADE_START_Y + i * 18);
        }
    }

    public int getProgress() {
        return this.data.get(DATA_PROGRESS);
    }

    public int getMaxProgress() {
        return this.data.get(DATA_MAX_PROGRESS);
    }

    public int getEnergy() {
        return this.data.get(DATA_ENERGY);
    }

    public int getMaxEnergy() {
        return this.data.get(DATA_MAX_ENERGY);
    }

    public int getFluidAmount() {
        return this.data.get(DATA_FLUID_AMOUNT);
    }

    public int getFluidCapacity() {
        return this.data.get(DATA_FLUID_CAPACITY);
    }

    public int getScrap() {
        return this.data.get(DATA_SCRAP);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemstack = itemStack1.copy();
            
            if (index < SLOT_COUNT) {
                // 从机器槽移动到玩家背包
                if (!this.moveItemStackTo(itemStack1, SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包移动到机器槽
                if (itemStack1.is(mio_icif_normal.SCRAP.get()) || itemStack1.is(mio_icif_normal.SCRAPBOX.get())) {
                    // 废料/废料盒 -> 增幅器槽
                    if (!this.moveItemStackTo(itemStack1, AMPLIFIER_SLOT, AMPLIFIER_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // 其他物品 -> 容器槽或升级槽
                    if (!this.moveItemStackTo(itemStack1, CONTAINER_SLOT, CONTAINER_SLOT + 1, false)) {
                        if (!this.moveItemStackTo(itemStack1, UPGRADE_SLOT_START, UPGRADE_SLOT_START + UPGRADE_SLOT_COUNT, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }
            
            if (itemStack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }
}