package com.singularity_iteration.mio_icif.Menu.Base;

import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.util.mio_icif_gui_global_variables;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@SuppressWarnings("null")
public abstract class mio_icif_base_menu extends AbstractContainerMenu {

    /** 标准能量条的像素宽度 */
    protected static final int ENERGY_BAR_WIDTH = mio_icif_gui_global_variables.ENERGY_BAR_WIDTH;

    /** 动能发电机能量条的像素宽度 */
    protected static final int KINETIC_ENERGY_BAR2_WIDTH = mio_icif_gui_global_variables.KINETIC_ENERGY_BAR2_WIDTH;

    protected final int machineSlotCount;

    protected final int playerInventoryStart;

    private final List<SlotMoveRule> customMoveRules = new ArrayList<>();

    /**
     * 标记槽位是否属于机器槽位（非玩家物品栏）
     * 用于处理有显示槽混入的情况（如BatBoxMenu、MagnetizerMenu�?
     */
    private final List<Boolean> machineSlotFlags = new ArrayList<>();

    protected mio_icif_base_menu(net.minecraft.world.inventory.MenuType<?> type, int containerId,
                                  int machineSlotCount, int playerInventoryStart) {
        super(type, containerId);
        this.machineSlotCount = machineSlotCount;
        this.playerInventoryStart = playerInventoryStart;
    }

    /**
     * 新版构造函数：不再需要手动传入machineSlotCount和playerInventoryStart
     * 子类应在注册完所有槽位后调用finishSlotRegistration()
     */
    protected mio_icif_base_menu(net.minecraft.world.inventory.MenuType<?> type, int containerId) {
        super(type, containerId);
        this.machineSlotCount = -1;
        this.playerInventoryStart = -1;
    }

    /**
     * 标记接下来的n个槽位为机器槽位
     */
    protected void markMachineSlots(int count) {
        for (int i = 0; i < count; i++) {
            machineSlotFlags.add(true);
        }
    }

    /**
     * 标记接下来的n个槽位为玩家物品栏槽位?
     */
    protected void markPlayerSlots(int count) {
        for (int i = 0; i < count; i++) {
            machineSlotFlags.add(false);
        }
    }

    /**
     * 判断指定索引的槽位是否是机器槽位
     * 优先使用显式标记，如果没有标记则回退到旧的逻辑
     */
    protected boolean isMachineSlot(int index) {
        if (index >= 0 && index < machineSlotFlags.size()) {
            return machineSlotFlags.get(index);
        }
        // 回退到旧的逻辑
        return index < this.machineSlotCount;
    }

    /**
     * 获取玩家物品栏起始索引?
     * 优先使用显式标记查找第一个玩家槽位，如果没有则回退到旧的逻辑
     */
    protected int getPlayerInventoryStart() {
        for (int i = 0; i < machineSlotFlags.size(); i++) {
            if (!machineSlotFlags.get(i)) {
                return i;
            }
        }
        // 回退到旧的逻辑
        return this.playerInventoryStart;
    }

    /**
     * 获取所有机器槽位的索引范围（用于快速移动目标）
     */
    protected List<int[]> getMachineSlotRanges() {
        List<int[]> ranges = new ArrayList<>();
        int start = -1;

        for (int i = 0; i < machineSlotFlags.size(); i++) {
            if (machineSlotFlags.get(i)) {
                if (start == -1) start = i;
            } else {
                if (start != -1) {
                    ranges.add(new int[]{start, i});
                    start = -1;
                }
            }
        }
        if (start != -1) {
            ranges.add(new int[]{start, machineSlotFlags.size()});
        }

        // 如果没有显式标记，使用旧的逻辑
        if (ranges.isEmpty() && this.machineSlotCount > 0) {
            ranges.add(new int[]{0, this.machineSlotCount});
        }

        return ranges;
    }

    protected abstract boolean isBattery(ItemStack stack);

    protected boolean isFuel(ItemStack stack) {
        return false;
    }

    protected boolean isChargeable(ItemStack stack) {
        return false;
    }

    protected boolean isUpgrade(ItemStack stack) {
        return MioIcifAPI.instance().getItemAPI().isUpgrade(stack);
    }

    protected int getBatterySlotIndex() {
        return 0;
    }

    protected int getFuelSlotIndex() {
        return 0;
    }

    protected int getChargeSlotIndex() {
        return 1;
    }

    protected int getUpgradeSlotStart() {
        return -1;
    }

    protected int getUpgradeSlotCount() {
        return 0;
    }

    /**
     * 注册自定义快速移动规则?
     * 子类可以在构造函数中调用此方法添加额外的槽位移动规则
     * 规则按注册顺序执行，先注册的先尝�?
     *
     * @param validator 物品验证�?
     * @param slotStart 目标槽位起始索引（包含）
     * @param slotEnd   目标槽位结束索引（不包含�?
     */
    protected void addCustomMoveRule(Predicate<ItemStack> validator, int slotStart, int slotEnd) {
        customMoveRules.add(new SlotMoveRule(validator, slotStart, slotEnd));
    }

    /**
     * 清空自定义快速移动规则?
     */
    protected void clearCustomMoveRules() {
        customMoveRules.clear();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemstack = itemStack1.copy();

            if (isMachineSlot(index)) {
                // 从机器槽位移出到玩家物品栈?
                if (!this.moveItemStackTo(itemStack1, getPlayerInventoryStart(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家物品栏移入到机器槽位?
                boolean moved = false;

                if (!moved && isUpgrade(itemStack1)) {
                    int upgradeStart = getUpgradeSlotStart();
                    int upgradeCount = getUpgradeSlotCount();
                    if (upgradeStart >= 0 && upgradeCount > 0) {
                        if (this.moveItemStackTo(itemStack1, upgradeStart, upgradeStart + upgradeCount, false)) {
                            moved = true;
                        }
                    }
                }

                if (!moved && isChargeable(itemStack1)) {
                    int chargeSlot = getChargeSlotIndex();
                    if (isValidMachineSlot(chargeSlot) && this.moveItemStackTo(itemStack1, chargeSlot, chargeSlot + 1, false)) {
                        moved = true;
                    }
                }

                if (!moved && isBattery(itemStack1)) {
                    int batterySlot = getBatterySlotIndex();
                    if (isValidMachineSlot(batterySlot) && this.moveItemStackTo(itemStack1, batterySlot, batterySlot + 1, false)) {
                        moved = true;
                    }
                }

                if (!moved && isFuel(itemStack1)) {
                    int fuelSlot = getFuelSlotIndex();
                    if (isValidMachineSlot(fuelSlot) && this.moveItemStackTo(itemStack1, fuelSlot, fuelSlot + 1, false)) {
                        moved = true;
                    }
                }

                if (!moved) {
                    for (SlotMoveRule rule : customMoveRules) {
                        if (rule.validator.test(itemStack1)) {
                            if (this.moveItemStackTo(itemStack1, rule.slotStart, rule.slotEnd, false)) {
                                moved = true;
                                break;
                            }
                        }
                    }
                }

                if (!moved) {
                    // 尝试移动到机器槽位（所有机器槽位范围）
                    moved = tryMoveToMachineSlots(itemStack1);
                }

                if (!moved) {
                    // Do not move within the player inventory here: this range contains the
                    // source slot, so an unsupported stack can be reported as a successful
                    // self-merge even though no machine slot accepted it.
                    return ItemStack.EMPTY;
                }
            }

            if (itemStack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setByPlayer(itemStack1);
            }

            // A non-empty return tells the client that the shift-click succeeded.
            // Never report success when a slot implementation accepted no items.
            if (itemStack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
        }

        return itemstack;
    }

    /**
     * 检查指定索引是否是有效的机器槽位?
     */
    protected boolean isValidMachineSlot(int index) {
        if (index < 0 || index >= this.slots.size()) return false;
        return isMachineSlot(index);
    }

    /**
     * 尝试将物品移动到任意机器槽位
     */
    protected boolean tryMoveToMachineSlots(ItemStack stack) {
        List<int[]> ranges = getMachineSlotRanges();
        for (int[] range : ranges) {
            if (this.moveItemStackTo(stack, range[0], range[1], false)) {
                return true;
            }
        }
        return false;
    }

    protected void addPlayerInventory(Inventory playerInventory, int startY, int hotbarY) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, startY + i * 18));
            }
        }

        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, hotbarY));
        }
    }

    /**
     * 添加玩家物品栏槽位（重载方法，兼容旧代码�?
     */
    protected void addPlayerInventorySlots(Inventory playerInventory, int startY, int hotbarY) {
        addPlayerInventory(playerInventory, startY, hotbarY);
    }

    /**
     * 添加玩家物品栏槽位（默认位置，兼容旧代码�?
     */
    protected void addPlayerInventorySlots(Inventory playerInventory) {
        addPlayerInventory(playerInventory, 84, 142);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
    }

    /**
     * 设置同步数据（子类应重写此方法）
     */
    public void setSyncData(int index, int value) {
        // 基类空实现，有data的子类应重写
    }

    /**
     * 自定义槽位移动规则?
     */
    private record SlotMoveRule(Predicate<ItemStack> validator, int slotStart, int slotEnd) {
    }

    protected void addUpgradeSlot(net.neoforged.neoforge.items.IItemHandler itemHandler, int index, int x, int y) {
        this.addSlot(new net.neoforged.neoforge.items.SlotItemHandler(itemHandler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isUpgrade(stack);
            }

            @Override
            public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
                ItemStack stack = getItem();
                if (isTransformerUpgrade(stack)) {
                    return com.singularity_iteration.mio_icif.network.mio_icif_KeyboardManager.isSafetyKeyDown(player);
                }
                return super.mayPickup(player);
            }
        });
    }

    protected boolean isTransformerUpgrade(ItemStack stack) {
        if (MioIcifAPI.instance() == null) return false;
        return MioIcifAPI.instance().getUpgradeAPI().getUpgradeType(stack)
                == com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.UpgradeType.TRANSFORMER;
    }
}