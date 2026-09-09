package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_advanced_miner_elc;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import com.singularity_iteration.mio_icif.Menu.Slot.PhantomSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class AdvancedMinerElcMenu extends mio_icif_machine_menu {

    // 槽位定义 - 匹配原版IC2高级采矿机GUI布局
    public static final int SLOT_BATTERY = 0;        // 电池槽 (8, 80)
    public static final int SLOT_SCANNER = 1;        // 扫描仪槽 (8, 26)
    public static final int SLOT_UPGRADE_START = 2;  // 升级槽起始(152, 26) - 4个(垂直排列)
    public static final int SLOT_UPGRADE_COUNT = 4;
    public static final int SLOT_UPGRADE_END = SLOT_UPGRADE_START + SLOT_UPGRADE_COUNT; // 6
    public static final int TOTAL_SLOTS = 6;         // itemHandler实际槽位数（电池、扫描仪、升级）

    // 过滤槽定义（幽灵槽，不占用itemHandler）
    public static final int FILTER_COUNT = 15;       // 15个(3行5列) - 用于黑白名单
    public static final int FILTER_ROWS = 3;
    public static final int FILTER_COLS = 5;
    public static final int FILTER_START_X = 36;
    public static final int FILTER_START_Y = 44;

    @Nullable
    private BlockPos blockPos;

    @Nullable
    private mio_icif_advanced_miner_elc minerBlockEntity;

    public AdvancedMinerElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, new SimpleContainerData(7));
    }

    public AdvancedMinerElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_advanced_miner_elc blockEntity) {
        this(containerId, playerInventory, blockEntity, new SimpleContainerData(7));
    }

    public AdvancedMinerElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_advanced_miner_elc blockEntity, ContainerData data) {
        super(mio_icif_menus.ADVANCED_MINER_ELC_MENU_TYPE.get(), containerId, playerInventory,
              blockEntity, TOTAL_SLOTS, 7);
        this.minerBlockEntity = blockEntity;
    }

    public AdvancedMinerElcMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, playerInventory, null, new SimpleContainerData(7));
        if (data != null) {
            this.blockPos = data.readBlockPos();
            if (this.blockPos != null && playerInventory.player.level().getBlockEntity(this.blockPos) instanceof mio_icif_advanced_miner_elc miner) {
                this.minerBlockEntity = miner;
            }
        }
    }

    @Override
    protected void addMachineSlots() {
        IItemHandler handler = this.itemHandler;

        // 电池槽 (0) - 位置 (8, 80) - 原版IC2位置
        addBatterySlot(SLOT_BATTERY, 8, 80);

        // 扫描仪槽 (1) - 位置 (8, 26) - 原版IC2位置
        this.addSlot(new SlotItemHandler(handler, SLOT_SCANNER, 8, 26));

        // 过滤槽 - 15个(3行5列), 第一个在 (36, 44) - 原版IC2过滤器槽位置
        // 使用幽灵槽（PhantomSlot），不占用itemHandler，用于设置黑白名单
        for (int row = 0; row < FILTER_ROWS; row++) {
            for (int col = 0; col < FILTER_COLS; col++) {
                final int filterIndex = row * FILTER_COLS + col;
                PhantomSlot phantomSlot = new PhantomSlot(FILTER_COUNT + filterIndex, FILTER_START_X + col * 18, FILTER_START_Y + row * 18, 1) {
                    @Override
                    public ItemStack getItem() {
                        mio_icif_advanced_miner_elc miner = getMinerBlockEntity();
                        if (miner != null) return miner.getFilterStack(filterIndex);
                        return super.getItem();
                    }

                    @Override
                    public void set(ItemStack stack) {
                        mio_icif_advanced_miner_elc miner = getMinerBlockEntity();
                        if (miner != null) {
                            miner.setFilterStack(filterIndex, stack);
                        } else {
                            super.set(stack);
                        }
                    }
                };
                this.addSlot(phantomSlot);
            }
        }

        // 升级槽(2-5) - 4个(垂直排列), 最顶上的(152, 26) - 原版IC2位置
        for (int i = 0; i < SLOT_UPGRADE_COUNT; i++) {
            addUpgradeSlot(handler, SLOT_UPGRADE_START + i, 152, 26 + i * 18);
        }
    }

    @Override
    protected int getPlayerInventoryY() {
        return 121; // 玩家背包起始Y坐标
    }

    @Override
    protected int getPlayerHotbarY() {
        return 179; // 玩家快捷栏起始Y坐标
    }

    @Override
    protected int getBatterySlotIndex() {
        return SLOT_BATTERY;
    }

    @Override
    protected int getUpgradeSlotStart() {
        return SLOT_UPGRADE_START;
    }

    @Override
    protected int getUpgradeSlotCount() {
        return SLOT_UPGRADE_COUNT;
    }

    public int getProgress() {
        return this.data.get(0);
    }

    public int getMaxProgress() {
        return this.data.get(1);
    }

    public boolean isWorking() {
        return this.data.get(2) == 1;
    }

    public int getEnergy() {
        return this.data.get(3);
    }

    public int getMaxEnergy() {
        return this.data.get(4);
    }

    public boolean isSilkTouchMode() {
        return this.data.get(5) == 1;
    }

    public boolean isWhitelistMode() {
        return this.data.get(6) == 1;
    }

    public int getProgressPixels() {
        int progress = getProgress();
        int maxProgress = getMaxProgress();
        if (maxProgress == 0) return 0;
        return (progress * 24) / maxProgress;
    }

    @Nullable
    public mio_icif_advanced_miner_elc getBlockEntity() { return minerBlockEntity; }

    @Nullable
    public mio_icif_advanced_miner_elc getMinerBlockEntity() { return minerBlockEntity; }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        // 处理按钮点击事件
        if (minerBlockEntity != null) {
            minerBlockEntity.handleButtonClick(id);
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            if (slot instanceof PhantomSlot phantomSlot) {
                if (clickType == ClickType.PICKUP && (dragType == 0 || dragType == 1)) {
                    phantomSlot.onSlotClick(dragType, player);
                }
                return;
            }
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot instanceof PhantomSlot) {
            return ItemStack.EMPTY;
        }

        ItemStack itemstack = ItemStack.EMPTY;
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // 判断是否是机器槽位（电池槽、扫描仪槽、升级槽）
            // 在slots列表中：电池槽=0, 扫描仪槽=1, 升级槽=20-23
            boolean isMachineSlot = (index == SLOT_BATTERY || 
                                     index == SLOT_SCANNER || 
                                     (index >= 20 && index < 24));
            
            // 从机器槽位移出到玩家背包
            if (isMachineSlot) {
                if (!this.moveItemStackTo(itemstack1, 24, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 从玩家背包移入到机器槽位
            else {
                boolean moved = false;

                // 升级物品优先放入升级槽（在slots列表中索引20-23）
                if (itemstack1.getItem() instanceof com.singularity_iteration.mio_icif.Items.Upgrade.mio_icif_upgrade) {
                    if (this.moveItemStackTo(itemstack1, 20, 24, false)) {
                        moved = true;
                    }
                }
                
                // 扫描仪放入扫描仪槽
                if (!moved && minerBlockEntity != null && minerBlockEntity.isItemValidForSlot(SLOT_SCANNER, itemstack1)) {
                    if (this.moveItemStackTo(itemstack1, SLOT_SCANNER, SLOT_SCANNER + 1, false)) {
                        moved = true;
                    }
                }
                
                // 电池放入电池槽
                if (!moved && minerBlockEntity != null && minerBlockEntity.isItemValidForSlot(SLOT_BATTERY, itemstack1)) {
                    if (this.moveItemStackTo(itemstack1, SLOT_BATTERY, SLOT_BATTERY + 1, false)) {
                        moved = true;
                    }
                }

                if (!moved) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        // 同步数据到客户端
        if (minerBlockEntity != null) {
            this.data.set(0, minerBlockEntity.getProgress());
            this.data.set(1, minerBlockEntity.getMaxProgress());
            this.data.set(2, minerBlockEntity.isWorking() ? 1 : 0);
            this.data.set(3, (int) minerBlockEntity.getEnergyStorage().getAmount());
            this.data.set(4, (int) minerBlockEntity.getEnergyStorage().getCapacity());
            this.data.set(5, minerBlockEntity.isSilkTouchMode() ? 1 : 0);
            this.data.set(6, minerBlockEntity.isWhitelistMode() ? 1 : 0);
        }
    }
}