package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_memory;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 模式存储机方块实体类
 * 用于存储从模式扫描机扫描到的蓝图数据
 *
 * 每个存储机可以为不同的机器使用
 * 每次存储/删除操作需要1000 EU
 */
@SuppressWarnings("null")
public class mio_icif_pattern_storage extends mio_icif_Energy_Block {
    private static final Logger LOGGER = LogUtils.getLogger();

    // 最大存储模式数据
    public static final int MAX_PATTERNS = 64;
    // 每次操作消耗的能量
    public static final long ENERGY_PER_OPERATION = 100;
    // 能量存储配置
    public static final long DEFAULT_CAPACITY = 100000; // 100k EU
    public static final long DEFAULT_MAX_RECEIVE = 128; // 输入上限128
    public static final long DEFAULT_MAX_EXTRACT = 0;   // 不输出电力

    // 槽位布局
    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .memory()
        .build();
    public static final int SLOT_COUNT = 1;
    public static final int MEMORY_SLOT = 0; // 记忆水晶槽位(MEMORY)

    // 存储的模式数据列表
    private final List<mio_icif_scanner_elc.ScanResult> storedPatterns = new ArrayList<>();
    // 当前浏览中的索引
    private int currentIndex = 0;
    // 物品栏处理器
    protected MachineItemHandler itemHandler;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) energyStorage.getAmount();
                case 1 -> (int) energyStorage.getCapacity();
                case 2 -> currentIndex;
                case 3 -> storedPatterns.size();
                case 4 -> (int) (Double.doubleToRawLongBits(getCurrentUuCost()) >> 32);
                case 5 -> (int) Double.doubleToRawLongBits(getCurrentUuCost());
                case 6 -> (int) getCurrentEuCost() & 0xFFFF;
                case 7 -> (int) (getCurrentEuCost() >>> 16) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 8;
        }
    };

    public mio_icif_pattern_storage(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.PATTERN_STORAGE_ENTITY_TYPE.get());
    }

    public mio_icif_pattern_storage(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type, DEFAULT_CAPACITY, DEFAULT_MAX_RECEIVE, DEFAULT_MAX_EXTRACT, CableTier.LV);

        // 创建物品栏处理器
        this.itemHandler = new MachineItemHandler(LAYOUT) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_pattern_storage.this.isItemValidForSlot(slot, stack));
    }

    /**
     * 检查槽位是否存放适合特定槽位
     */
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == MEMORY_SLOT) {
            // 记忆水晶槽只接受记忆水晶
            return stack.getItem() instanceof mio_icif_memory;
        }
        return false;
    }

    /**
     * 检查是否有足够能量执行操作
     */
    public boolean hasEnoughEnergy() {
        return energyStorage.getAmount() >= ENERGY_PER_OPERATION;
    }

    /**
     * 消耗操作能量
     * @return 是否有足够能量消耗
     */
    public boolean consumeOperationEnergy() {
        if (hasEnoughEnergy()) {
            apiUseEnergy(ENERGY_PER_OPERATION, false);
            return true;
        }
        return false;
    }

    /**
     * 存储扫描结果到模式列表
     * @param result 扫描结果
     * @return 是否成功存储
     */
    public boolean storePattern(mio_icif_scanner_elc.ScanResult result) {
        LOGGER.info("[模式存储机] 尝试存储模式，当前数据量: " + storedPatterns.size() + "/" + MAX_PATTERNS);

        if (storedPatterns.size() >= MAX_PATTERNS) {
            return false;
        }

        // 检查能量
        LOGGER.info("[模式存储机] 当前能量: " + energyStorage.getAmount() + "/" + ENERGY_PER_OPERATION);
        if (!consumeOperationEnergy()) {
            return false;
        }
        LOGGER.info("[模式存储机] 能量消耗完成，剩余能量: " + energyStorage.getAmount());

        // 检查是否已存在相同物品
        for (mio_icif_scanner_elc.ScanResult existing : storedPatterns) {
            if (ItemStack.isSameItem(existing.item, result.item)) {
                // 已存在，更新为新扫描结果
                LOGGER.info("[模式存储机] 物品已存在，更新为新扫描模式");
                storedPatterns.remove(existing);
                break;
            }
        }

        storedPatterns.add(result);
        LOGGER.info("[模式存储机] 模式已添加，当前数据量: " + storedPatterns.size());
        setChanged();

        return true;
    }

    /**
     * 移除指定索引处的模式
     */
    public boolean removePattern(int index) {
        if (index < 0 || index >= storedPatterns.size()) {
            return false;
        }

        // 检查能量
        if (!consumeOperationEnergy()) {
            return false;
        }

        storedPatterns.remove(index);
        // 修正当前索引
        if (currentIndex >= storedPatterns.size() && currentIndex > 0) {
            currentIndex--;
        }
        setChanged();

        return true;
    }

    /**
     * 获取所有存储的模式列表
     */
    public List<mio_icif_scanner_elc.ScanResult> getStoredPatterns() {
        return new ArrayList<>(storedPatterns);
    }

    /**
     * 获取指定索引处的模式
     */
    @Nullable
    public mio_icif_scanner_elc.ScanResult getPattern(int index) {
        if (index < 0 || index >= storedPatterns.size()) {
            return null;
        }
        return storedPatterns.get(index);
    }

    /**
     * 查找匹配物品的模式
     */
    @Nullable
    public mio_icif_scanner_elc.ScanResult findPattern(ItemStack item) {
        for (mio_icif_scanner_elc.ScanResult result : storedPatterns) {
            if (ItemStack.isSameItem(result.item, item)) {
                return result;
            }
        }
        return null;
    }

    /**
     * 是否已存储物品数据
     */
    public boolean hasPattern(ItemStack item) {
        return findPattern(item) != null;
    }

    /**
     * 获取已存储数量
     */
    public int getStoredCount() {
        return storedPatterns.size();
    }

    /**
     * 是否已存满
     */
    public boolean isFull() {
        return storedPatterns.size() >= MAX_PATTERNS;
    }

    /**
     * 清空所有数据
     */
    public void clear() {
        storedPatterns.clear();
        currentIndex = 0;
        setChanged();
    }

    /**
     * 从服务端接收同步数据到客户端缓存
     */
    public void receiveSyncData(List<mio_icif_scanner_elc.ScanResult> patterns, int index) {
        LOGGER.info("[模式存储机] 收到同步数据，模式数量: " + patterns.size() + ", 当前索引: " + index);
        this.storedPatterns.clear();
        this.storedPatterns.addAll(patterns);
        this.currentIndex = index;
        LOGGER.info("[模式存储机] 同步完成，当前数据量: " + storedPatterns.size());
    }

    /**
     * 获取当前浏览的索引
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * 设置当前浏览的索引
     */
    public void setCurrentIndex(int index) {
        if (storedPatterns.isEmpty()) {
            currentIndex = 0;
        } else {
            currentIndex = Math.max(0, Math.min(index, storedPatterns.size() - 1));
        }
    }

    /**
     * 切换到上一个模式
     */
    public void previousPattern() {
        if (storedPatterns.isEmpty()) return;
        currentIndex = (currentIndex - 1 + storedPatterns.size()) % storedPatterns.size();
        setChanged();
    }

    /**
     * 切换到下一个模式
     */
    public void nextPattern() {
        if (storedPatterns.isEmpty()) return;
        currentIndex = (currentIndex + 1) % storedPatterns.size();
        setChanged();
    }

    /**
     * 获取当前模式物品
     */
    public ItemStack getCurrentPattern() {
        if (storedPatterns.isEmpty() || currentIndex < 0 || currentIndex >= storedPatterns.size()) {
            return ItemStack.EMPTY;
        }
        return storedPatterns.get(currentIndex).item;
    }

    /**
     * 获取当前模式的UU成本
     */
    public double getCurrentUuCost() {
        if (storedPatterns.isEmpty() || currentIndex < 0 || currentIndex >= storedPatterns.size()) {
            return 0;
        }
        return storedPatterns.get(currentIndex).uuMatterCostBuckets;
    }

    /**
     * 获取当前模式的EU成本
     */
    public long getCurrentEuCost() {
        if (storedPatterns.isEmpty() || currentIndex < 0 || currentIndex >= storedPatterns.size()) {
            return 0;
        }
        return storedPatterns.get(currentIndex).energyCost;
    }

    /**
     * 获取物品栏处理器
     */
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public ContainerData getContainerData() {
        return dataAccess;
    }

    /**
     * 获取特定方向的物品栏能力
     */
    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return itemHandler;
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        ListTag listTag = new ListTag();
        for (mio_icif_scanner_elc.ScanResult result : storedPatterns) {
            listTag.add(result.serializeNBT());
        }
        tag.put("patterns", listTag);
        tag.putInt("current_index", currentIndex);
        tag.put("inventory", itemHandler.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        storedPatterns.clear();
        if (tag.contains("patterns")) {
            ListTag listTag = tag.getList("patterns", 10); // 10 = CompoundTag
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag patternTag = listTag.getCompound(i);
                storedPatterns.add(mio_icif_scanner_elc.ScanResult.deserializeNBT(patternTag));
            }
        }
        currentIndex = tag.getInt("current_index");

        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.pattern_storage");
    }

    // ==================== 客户端同步 ====================

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);

        LOGGER.info("[模式存储机服务端] getUpdateTag 被调用，当前数据量: " + storedPatterns.size());

        // 同步模式列表到客户端
        ListTag listTag = new ListTag();
        for (mio_icif_scanner_elc.ScanResult result : storedPatterns) {
            listTag.add(result.serializeNBT());
        }
        tag.put("patterns", listTag);
        tag.putInt("current_index", currentIndex);

        LOGGER.info("[模式存储机服务端] 发送 " + listTag.size() + " 个模式到客户端");

        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);

        LOGGER.info("[模式存储机客户端] handleUpdateTag 被调用");

        // 从服务端同步模式列表
        storedPatterns.clear();
        if (tag.contains("patterns")) {
            ListTag listTag = tag.getList("patterns", 10); // 10 = CompoundTag
            LOGGER.info("[模式存储机客户端] 接收到 " + listTag.size() + " 个模式");
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag patternTag = listTag.getCompound(i);
                storedPatterns.add(mio_icif_scanner_elc.ScanResult.deserializeNBT(patternTag));
            }
        } else {
            LOGGER.info("[模式存储机客户端] 没有接收到patterns 数据");
        }
        currentIndex = tag.getInt("current_index");
        LOGGER.info("[模式存储机客户端] 当前索引: " + currentIndex + ", 存储数量: " + storedPatterns.size());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.PatternStorageMenu(
            containerId, playerInventory, this);
    }

    // ==================== Tick ====================

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_pattern_storage blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 调用父类tick方法处理充放电等逻辑
        mio_icif_Energy_Block.tick(level, pos, state, blockEntity);
    }
}