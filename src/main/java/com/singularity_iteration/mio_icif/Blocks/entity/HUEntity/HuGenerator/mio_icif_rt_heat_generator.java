package com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator;

import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.mio_icif_HeatU_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 放射性同位素温差加热?(RTG Heat Generator) 方块实体? *
 * 特点? * - 6个槽位用于放置RTG燃料靶丸
 * - 靶丸无限耐久，不需要更? * - 只在正面输出热量
 * - 正面有需要受热的机器才输出热? * - 产热量与放入的靶丸数量有关：
 *   1?= 2 HU/t
 *   2?= 4 HU/t
 *   3?= 8 HU/t
 *   4?= 16 HU/t
 *   5?= 32 HU/t
 *   6?= 64 HU/t
 */
@SuppressWarnings("null")
public class mio_icif_rt_heat_generator extends mio_icif_HeatU_Generator {

    // 槽位数量?个靶丸槽位
public static final int SLOT_COUNT = 6;
    // 热能容量
    public static final int HEAT_CAPACITY = 1000;
    // 最大输出速率
    public static final int MAX_EXTRACT = 64;
    // 最高温
public static final int MAX_TEMP = 200;

 // 当前放入的靶丸数量（存，避免每tick重新计算
private int cachedPelletCount = -1;
    // 当前产热
private int currentHeatRate = 0;

    // 产热量表 - 根据靶丸数量对应的产热量 (HU/t)
    private static final int[] HEAT_RATES = {0, 2, 4, 8, 16, 32, 64};

    private static final SlotLayout RTG_LAYOUT = SlotLayout.builder()
        .rtgPellet(SLOT_COUNT)
        .build();

    /**
     * 构造函数
 */
    public mio_icif_rt_heat_generator(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.RT_HEAT_GENERATOR.get(), pos, state,
              RTG_LAYOUT, 0, HEAT_CAPACITY, MAX_EXTRACT, MAX_TEMP);

        this.itemHandler.setValidator((slot, stack, slotType) -> stack.is(mio_icif_resources.RTG_PELLET.get()));
    }

    /**
     * 每tick更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_rt_heat_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 计算当前放入的靶丸数
    int pelletCount = blockEntity.countPellets();

        // 如果靶丸数量改变，更新产热量
        if (pelletCount != blockEntity.cachedPelletCount) {
            blockEntity.cachedPelletCount = pelletCount;
            blockEntity.currentHeatRate = HEAT_RATES[pelletCount];
        }

        // 检查正面是否有需要热量的机器
        boolean hasConsumer = blockEntity.hasHeatConsumerInFront();

        // 如果有靶丸且正面有需要热量的机器，输出热
    if (blockEntity.currentHeatRate > 0 && hasConsumer) {
            blockEntity.outputHeatToFront();
        }

        // 更新方块active状态（用于模型切换
    boolean shouldBeActive = blockEntity.currentHeatRate > 0 && hasConsumer;
        boolean currentActive = state.getValue(com.singularity_iteration.mio_icif.Blocks.HUGenerator.mio_icif_block_rt_heat_generator.ACTIVE);
        if (currentActive != shouldBeActive) {
            level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.HUGenerator.mio_icif_block_rt_heat_generator.ACTIVE, shouldBeActive), 3);
        }

        // 标记方块实体已更新
    blockEntity.setChanged();
    }

    /**
     * 统计放入的RTG靶丸数量
     */
    private int countPellets() {
        int count = 0;
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(mio_icif_resources.RTG_PELLET.get())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 向正面输出热
 */
    private void outputHeatToFront() {
        if (level == null || currentHeatRate <= 0) return;

        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos frontPos = worldPosition.relative(facing);

        IMioIcifCapabilities.IHeatStorage frontHeat = level.getCapability(
                IMioIcifCapabilities.HEAT_STORAGE_BLOCK, frontPos, facing.getOpposite());
        if (frontHeat == null) {
            frontHeat = MioIcifAPI.instance().getCapabilities().adaptHeatStorage(
                level.getBlockEntity(frontPos));
        }

        if (frontHeat != null && frontHeat.canReceiveHeat()) {
            long heatToOutput = Math.min((long)currentHeatRate,
                    frontHeat.getMaxHeatStored() - frontHeat.getHeatStored());
            if (heatToOutput > 0) {
                frontHeat.receiveHeat(heatToOutput, false);
            }
        }
    }

    /**
     * 获取当前产热
 */
    public int getCurrentHeatRate() {
        return currentHeatRate;
    }

    /**
     * 获取燃料燃烧时间 - RTG不使用传统燃料燃烧机
 */
    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        // RTG不使用传统燃料燃烧机制，返回0
        return 0;
    }

    /**
     * 获取当前放入的靶丸数
 */
    public int getPelletCount() {
        return cachedPelletCount >= 0 ? cachedPelletCount : countPellets();
    }

    /**
     * 获取物品处理器
 */
    public MachineItemHandler getItemHandler() {
        return itemHandler;
    }

    /**
     * 获取物品处理器（用于Capability
 */
    public IItemHandler getItemHandlerCapability(@Nullable Direction direction) {
        return itemHandler;
    }

    // ==================== WorldlyContainer 实现 ====================

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
        return stack.is(mio_icif_resources.RTG_PELLET.get());
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        // 允许取出
        return true;
    }

    // ==================== Container 实现 ====================

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return itemHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return itemHandler.extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        itemHandler.setStackInSlot(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    // ==================== NBT 数据保存/读取 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CachedPelletCount", cachedPelletCount);
        tag.putInt("CurrentHeatRate", currentHeatRate);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.cachedPelletCount = tag.getInt("CachedPelletCount");
        this.currentHeatRate = tag.getInt("CurrentHeatRate");
        if (tag.contains("RTGItems")) {
            com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler tempHandler = new com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler(RTG_LAYOUT);
            tempHandler.deserializeNBT(registries, tag.getCompound("RTGItems"));
            for (int i = 0; i < SLOT_COUNT; i++) {
                itemHandler.setStackInSlot(i, tempHandler.getStackInSlot(i));
            }
        }
    }

    // ==================== MenuProvider 实现 ====================

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return Component.translatable("container.mio_icif.rt_heat_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.HUEntity.RTHeatGeneratorMenu(containerId, playerInventory, this);
    }

    // ==================== IHeatGeneratorBlock 接口实现 ====================

    @Override
    public int getHeatOutput() {
        return currentHeatRate;
    }

    @Override
    public boolean isGenerating() {
        return currentHeatRate > 0;
    }

    @Override
    public int getBurnTime() {
        return 0;
    }

    @Override
    public int getBurnDuration() {
        return 0;
    }

    @Override
    public int getHeatGenerationRate() {
        return currentHeatRate;
    }
}