package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 放射性同位素温差发电机(RTG) 方块实体
 *
 * 特点：
 * - 6个槽位用于放置RTG燃料靶丸
 * - 靶丸无限耐久，不需要更换
 * - 发电量与放入的靶丸数量有关（IC2原版 efficiency=2.0）：
 *   1个= 2 EU/t
 *   2个= 4 EU/t
 *   3个= 8 EU/t
 *   4个= 16 EU/t
 *   5个= 32 EU/t
 *   6个= 64 EU/t
 */
@SuppressWarnings("null")
public class mio_icif_rt_generator extends mio_icif_Energy_Generator {

    // 槽位数量：6个靶丸槽位
    public static final int SLOT_COUNT = 6;
    // 能量容量
    public static final long ENERGY_CAPACITY = 10000L;
    // 最大输出速率 (LV)
    public static final long MAX_EXTRACT = 32L;
    // 最大接收速率 (RTG不接收外部能量)
    public static final long MAX_RECEIVE = 0L;
    // 基础发电速率 (会在tick中根据靶丸数量动态调整)
    public static final long BASE_GENERATION_RATE = 0L;

 // 当前放入的靶丸数量（缓存，避免每tick重新计算）
    private int cachedPelletCount = -1;
    // 当前发电量
    private long currentGenerationRate = 0;

    // 发电量表 - 根据靶丸数量对应的发电量（IC2原版: 2^(n-1) × efficiency, efficiency=2.0）
    private static final long[] GENERATION_RATES = {0L, 2L, 4L, 8L, 16L, 32L, 64L};

    /**
     * 构造函数（用于 BlockEntityType.Builder）
     */
    public mio_icif_rt_generator(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    /**
     * 构造函数
     */
    public mio_icif_rt_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type != null ? type : mio_icif_block_entities.RT_GENERATOR_ENTITY_TYPE.get(),
              SlotLayout.builder().rtgPellet(6).build(), BASE_GENERATION_RATE, ENERGY_CAPACITY, MAX_RECEIVE, MAX_EXTRACT, CableTier.LV);
    }

    /**
     * 每tick更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_rt_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 计算当前放入的靶丸数量
        int pelletCount = blockEntity.countPellets();

        // 如果靶丸数量改变，更新发电量
        if (pelletCount != blockEntity.cachedPelletCount) {
            blockEntity.cachedPelletCount = pelletCount;
            blockEntity.currentGenerationRate = GENERATION_RATES[pelletCount];
            // 更新电源输出功率
            blockEntity.setAsPowerSource(blockEntity.currentGenerationRate);
        }

        // 如果有靶丸且能量未满，生成能量
        if (blockEntity.currentGenerationRate > 0 &&
            blockEntity.getEnergyStorage().getAmount() < blockEntity.getEnergyStorage().getCapacity()) {
            blockEntity.generateEnergyInternal();
        }

        // 分配能量到相邻方块
        blockEntity.distributeEnergy();

        // 更新方块active状态（用于模型切换）
        boolean shouldBeActive = blockEntity.currentGenerationRate > 0;
        boolean currentActive = state.getValue(com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_RT_Generator.ACTIVE);
        if (currentActive != shouldBeActive) {
            level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_RT_Generator.ACTIVE, shouldBeActive), 3);
        }

        // 标记方块实体已更新
        blockEntity.setChanged();
    }

    /**
     * 生成能量（内部方法）
     */
    private void generateEnergyInternal() {
        if (currentGenerationRate > 0) {
            apiGenerateEnergy(currentGenerationRate, false);
        }
    }

    /**
     * 统计放入的RTG靶丸数量
     */
    private int countPellets() {
        int count = 0;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(mio_icif_resources.RTG_PELLET.get())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取当前发电量
     */
    public long getCurrentGenerationRate() {
        return currentGenerationRate;
    }

    /**
     * 获取当前放入的靶丸数量
     */
    public int getPelletCount() {
        return cachedPelletCount >= 0 ? cachedPelletCount : countPellets();
    }

    /**
     * RTG不使用燃烧机制，始终返回0
     */
    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        // RTG不使用传统燃料燃烧机制，返回0
        return 0;
    }

    /**
     * 检查物品是否是RTG靶丸（用于漏斗交互等）
     */
    public boolean isPellet(ItemStack stack) {
        return stack.is(mio_icif_resources.RTG_PELLET.get());
    }

    // ==================== WorldlyContainer 重写 ====================

    @Override
    public int[] getSlotsForFace(Direction side) {
        // 所有面都可以访问所有槽位
        int[] slots = new int[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        // 只允许放入RTG靶丸
        return isPellet(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        // 允许取出
        return true;
    }

    @Override
    public int getMaxStackSize() {
        return 1; // 每个槽位只能放一个靶丸
    }

    // ==================== NBT 数据保存/读取 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CachedPelletCount", cachedPelletCount);
        tag.putLong("CurrentGenerationRate", currentGenerationRate);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.cachedPelletCount = tag.getInt("CachedPelletCount");
        this.currentGenerationRate = tag.getLong("CurrentGenerationRate");
    }

    // ==================== MenuProvider 接口实现 ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.rt_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Generator.RTGeneratorMenu(containerId, playerInventory, this, this.getItemHandler(), null);
    }
}