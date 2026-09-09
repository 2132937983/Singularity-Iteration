package com.singularity_iteration.mio_icif.Menu.Base;

import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 通用机器 Menu 基类
 * 适用于有输入、输出、电池、升级槽的标准加工机器?
 */
@SuppressWarnings("null")
public abstract class mio_icif_machine_menu extends mio_icif_base_menu {

    protected final IItemHandler itemHandler;
    public final ContainerData data;
    @Nullable
    public final Object blockEntity;

    @Nullable
    public Object getBlockEntity() { return blockEntity; }
    public ContainerData getData() { return data; }

    /**
     * 标准构造函数?
     * @param type              MenuType
     * @param containerId       容器ID
     * @param machineSlotCount  机器槽位总数
     * @param playerInventory   玩家物品栈?
     * @param itemHandler       物品处理器（可为null，此时创建空handler�?
     * @param data              数据同步器（可为null，此时创建空data�?
     * @param dataCount         数据同步器大小（当data为null时使用）
     */
    protected mio_icif_machine_menu(MenuType<?> type, int containerId, int machineSlotCount,
                                     Inventory playerInventory, @Nullable IItemHandler itemHandler,
                                     @Nullable ContainerData data, int dataCount) {
        this(type, containerId, machineSlotCount, playerInventory, itemHandler, data, dataCount, null);
    }

    /**
     * 完整构造函数（带blockEntity�?
     * @param type              MenuType
     * @param containerId       容器ID
     * @param machineSlotCount  机器槽位总数
     * @param playerInventory   玩家物品栈?
     * @param itemHandler       物品处理器（可为null，此时创建空handler�?
     * @param data              数据同步器（可为null，此时创建空data�?
     * @param dataCount         数据同步器大小（当data为null时使用）
     * @param blockEntity       方块实体（可为null�?
     */
    protected mio_icif_machine_menu(MenuType<?> type, int containerId, int machineSlotCount,
                                     Inventory playerInventory, @Nullable IItemHandler itemHandler,
                                     @Nullable ContainerData data, int dataCount, @Nullable Object blockEntity) {
        super(type, containerId, machineSlotCount, machineSlotCount);
        this.itemHandler = itemHandler != null ? itemHandler : new ItemStackHandler(machineSlotCount);
        this.data = data != null ? data : new SimpleContainerData(dataCount);
        this.blockEntity = blockEntity;
        this.addDataSlots(this.data);
        this.addMachineSlots();
        if (shouldAddPlayerInventory()) {
            this.addPlayerInventory(playerInventory, getPlayerInventoryY(), getPlayerHotbarY());
        }
    }

    /**
     * 兼容旧代码的构造函数（带blockEntity参数据?
     * 子类应重�?getDataSlotCount() 并通过5参数版本调用此构造函数?
     */
    protected mio_icif_machine_menu(MenuType<?> type, int containerId, Inventory playerInventory,
                                     Object blockEntity, int machineSlotCount, int dataCount) {
        this(type, containerId, machineSlotCount, playerInventory, 
             blockEntity != null ? getItemHandlerFromBlockEntity(blockEntity) : null,
             null, dataCount, blockEntity);
    }

    /**
     * 兼容旧代码的构造函数（默认dataCount=5�?
     */
    protected mio_icif_machine_menu(MenuType<?> type, int containerId, Inventory playerInventory,
                                     Object blockEntity, int machineSlotCount) {
        this(type, containerId, machineSlotCount, playerInventory, 
             blockEntity != null ? getItemHandlerFromBlockEntity(blockEntity) : null,
             null, 5, blockEntity);
    }

    private static IItemHandler getItemHandlerFromBlockEntity(Object blockEntity) {
        try {
            var method = blockEntity.getClass().getMethod("getItemHandler");
            return (IItemHandler) method.invoke(blockEntity);
        } catch (Exception e) {
            try {
                var field = blockEntity.getClass().getField("itemHandler");
                return (IItemHandler) field.get(blockEntity);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /** 子类必须实现：添加机器槽位?*/
    protected abstract void addMachineSlots();

    /** 子类可以重写：是否添加玩家背包，默认true */
    protected boolean shouldAddPlayerInventory() { return true; }

    /** 子类可以重写：玩家背包起始Y坐标，默�?4 */
    protected int getPlayerInventoryY() { return 84; }

    /** 子类可以重写：玩家快捷栏Y坐标，默�?42 */
    protected int getPlayerHotbarY() { return 142; }

    /** 子类可以实现：返回数据同步器大小 */
    protected int getDataSlotCount() { return 5; }

    // ===== 子类必须实现的槽位配方?=====

    /** 输入槽起始索引，-1表示无输入槽 */
    protected int getInputSlotStart() {
        return -1;
    }

    /** 输入槽数据?*/
    protected int getInputSlotCount() {
        return 0;
    }

    /** 输出槽起始索引，-1表示无输出槽 */
    protected int getOutputSlotStart() {
        return -1;
    }

    /** 输出槽数据?*/
    protected int getOutputSlotCount() {
        return 0;
    }

    // ===== 通用数据访问方法 =====

    public int getProgress() {
        return data.getCount() > 0 ? data.get(0) : 0;
    }

    public int getMaxProgress() {
        return data.getCount() > 1 ? data.get(1) : 0;
    }

    public boolean isWorking() {
        return data.getCount() > 2 && data.get(2) == 1;
    }

    public int getEnergy() {
        return data.getCount() > 3 ? data.get(3) : 0;
    }

    public int getMaxEnergy() {
        return data.getCount() > 4 ? data.get(4) : 0;
    }

    /**
     * 获取进度条像素宽�?
     *
     * @param maxPixels 最大像素宽�?
     * @return 当前进度对应的像素宽�?
     */
    public int getProgressPixels(int maxPixels) {
        int progress = getProgress();
        int maxProgress = getMaxProgress();
        if (maxProgress == 0) return 0;
        return (progress * maxPixels) / maxProgress;
    }

    public int getProgressPixels() {
        return getProgressPixels(24);
    }

    /**
     * 获取能量条像素宽度
     *
     * @return 当前能量对应的像素宽度
     */
    public int getEnergyProgressPixels() {
        int energy = getEnergy();
        int maxEnergy = getMaxEnergy();
        if (maxEnergy <= 0) return 0;
        return (int) ((energy * ENERGY_BAR_WIDTH) / maxEnergy);
    }

    /**
     * 设置同步数据（供 Screen �?containerTick 中调用）
     */
    public void setSyncData(int index, int value) {
        if (index >= 0 && index < data.getCount()) {
            data.set(index, value);
        }
    }

    // ===== 快速移动逻辑扩展 =====

    @Override
    protected boolean isBattery(ItemStack stack) {
        return MioIcifAPI.instance().getItemAPI().isBattery(stack)
            || stack.getItem() == net.minecraft.world.item.Items.REDSTONE;
    }

    /**
     * 添加输入槽到容器
     */
    protected void addInputSlot(int index, int x, int y) {
        this.addSlot(new SlotItemHandler(itemHandler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isValidInput(stack);
            }
        });
    }

    /**
     * 添加输出槽到容器（不允许放入�?
     */
    protected void addOutputSlot(int index, int x, int y) {
        this.addSlot(new SlotItemHandler(itemHandler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    /**
     * 添加电池槽到容器
     */
    protected void addBatterySlot(int index, int x, int y) {
        this.addSlot(new SlotItemHandler(itemHandler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isBattery(stack);
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                if (stack.getItem() == net.minecraft.world.item.Items.REDSTONE) return 64;
                return 1;
            }
        });
    }

    protected void addUpgradeSlot(int index, int x, int y) {
        super.addUpgradeSlot(itemHandler, index, x, y);
    }

    /**
     * 验证物品是否可以放入输入�?
     * 子类可以重写此方法?
     */
    protected boolean isValidInput(ItemStack stack) {
        return true;
    }

    /**
     * 注册机器特有的快速移动规则?
     * 子类在构造函数中调用
     */
    protected void registerMachineMoveRules() {
        int inputStart = getInputSlotStart();
        int inputCount = getInputSlotCount();
        if (inputStart >= 0 && inputCount > 0) {
            addCustomMoveRule(this::isValidInput, inputStart, inputStart + inputCount);
        }
    }

    /** 电池槽索引，-1表示无电池槽 */
    protected int getBatterySlotIndex() {
        return -1;
    }

    /** 升级槽起始索引，-1表示无升级槽 */
    protected int getUpgradeSlotStart() {
        return -1;
    }

    /** 升级槽数据?*/
    protected int getUpgradeSlotCount() {
        return 0;
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }
}