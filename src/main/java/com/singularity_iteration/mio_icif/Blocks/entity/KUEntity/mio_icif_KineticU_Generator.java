package com.singularity_iteration.mio_icif.Blocks.entity.KUEntity;

import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import com.singularity_iteration.mio_icif.api.machine.IBurnControl;
import com.singularity_iteration.mio_icif.api.machine.IKineticGeneratorBlock;

import com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * KU 动能发电机方块实体基类
 * 模仿 mio_icif_HeatU_Generator，但使用 KU 动能系统
 * 
 * 动能发电机特点：
 * - 燃烧燃料或使用其他能源产生动能（KU）
 * - 动能会有摩擦损失
 * - 可以向相邻方块传输动能
 * - 需要转速差才能有效传输
 * - 需要机械传动来传输
 */
@SuppressWarnings("null")
public abstract class mio_icif_KineticU_Generator extends mio_icif_KineticU_Block implements WorldlyContainer, IBurnControl, IKineticGeneratorBlock {

    // 物品栏处理器
    protected MachineItemHandler itemHandler;

    // 当前燃烧时间
    public int burnTime = 0;

    // 当前物品的总燃烧时间
    public int burnDuration = 0;

    @Override
    public int getDefaultBurnTime() {
        return burnDuration;
    }

    @Override
    public void setBurnTime(int ticks) {
        this.burnTime = ticks;
        this.setChanged();
    }

    // 动能生成速率 (KU/tick)
    protected final int kineticGenerationRate;

    // 燃料槽索引
    protected static final int FUEL_SLOT = 0;

    /**
     * 构造函数
     * @param type 方块实体类型
     * @param pos 位置
     * @param state 方块状态
     * @param layout 槽位布局
     * @param kineticGenerationRate 动能生成速率 (KU/tick)
     */
    public mio_icif_KineticU_Generator(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                       SlotLayout layout, int kineticGenerationRate) {
 // 发电机没有动能存，容量设为0，最大输出设为生成速率
        super(type, pos, state, 0, 0, kineticGenerationRate, 10000, 0.0f);
        this.itemHandler = createItemHandler(layout);
        this.kineticGenerationRate = kineticGenerationRate;
    }

    /**
     * 构造函数（支持自定义动能参数）
     * @param type 方块实体类型
     * @param pos 位置
     * @param state 方块状态
     * @param layout 槽位布局
     * @param kineticGenerationRate 动能生成速率 (KU/tick)
     * @param maxExtract 最大输出速率
     * @param maxRPM 最大转速
     */
    public mio_icif_KineticU_Generator(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                       SlotLayout layout, int kineticGenerationRate,
                                       int maxExtract, int maxRPM) {
 // 产生动能的方块没有动能存，容量设为0
        super(type, pos, state, 0, 0, maxExtract, maxRPM, 0.0f);
        this.itemHandler = createItemHandler(layout);
        this.kineticGenerationRate = kineticGenerationRate;
    }

    protected MachineItemHandler createItemHandler(SlotLayout layout) {
        return new MachineItemHandler(layout) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }
    
    /**
     * 检查前方是否有需要动能的机器
     */
    protected boolean hasKineticConsumerInFront() {
        if (level == null) return false;

        Direction facing = getBlockState().getValue(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
        BlockPos frontPos = worldPosition.relative(facing);

        IMioIcifCapabilities.IKineticStorage frontKinetic = level.getCapability(
            IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, frontPos, facing.getOpposite());
        if (frontKinetic == null) {
            frontKinetic = MioIcifAPI.instance().getCapabilities().adaptKineticStorage(
                level.getBlockEntity(frontPos));
        }

        if (frontKinetic != null && frontKinetic.canReceiveKinetic()) {
            return frontKinetic.getKineticStored() < frontKinetic.getMaxKineticStored();
        }

        return false;
    }
    
    /**
     * 每 tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_KineticU_Generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 记录之前的燃烧状态
        boolean wasBurning = blockEntity.isBurning();

        // 检查前方是否有需要动能的机器
        boolean hasConsumer = blockEntity.hasKineticConsumerInFront();

        // 如果正在燃烧，生成并输出动能
        if (blockEntity.isBurning()) {
            // 如果前方没有需要动能的机器，停止燃烧
            if (!hasConsumer) {
                blockEntity.burnTime = 0;
            } else {
                // 直接输出动能到相邻方向
                blockEntity.outputKineticDirectly();
                blockEntity.burnTime--;

                // 如果燃烧完毕，尝试消耗新的燃料
                if (blockEntity.burnTime <= 0) {
                    blockEntity.consumeFuel();
                }
            }
        } else {
            // 如果没有在燃烧且前方有需要动能的机器，尝试消耗新的燃料
            if (hasConsumer) {
                blockEntity.consumeFuel();
            }
        }

        // 检查燃烧状态是否改变
        boolean isBurning = blockEntity.isBurning();
        if (wasBurning != isBurning) {
            // 更新方块状态（子类可以实现 ACTIVATE 属性）
            blockEntity.updateBlockState(isBurning);
        }

        // 标记方块实体已更新
        blockEntity.setChanged();
    }

    /**
     * 直接向相邻方块输出动能（发电机没有存储，直接输出）
     */
    protected void outputKineticDirectly() {
        if (level == null) return;

        long remainingKinetic = kineticGenerationRate;
        int myRPM = kineticStorage.getRPM();

        Direction facing = getBlockState().getValue(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
        BlockPos frontPos = worldPosition.relative(facing);
        IMioIcifCapabilities.IKineticStorage frontKinetic = level.getCapability(
            IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, frontPos, facing.getOpposite());
        if (frontKinetic == null) {
            frontKinetic = MioIcifAPI.instance().getCapabilities().adaptKineticStorage(
                level.getBlockEntity(frontPos));
        }

        if (frontKinetic != null && frontKinetic.canReceiveKinetic()) {
            int adjacentRPM = frontKinetic.getRPM();
            if (myRPM > adjacentRPM) {
                long maxReceive = frontKinetic.getMaxKineticStored() - frontKinetic.getKineticStored();
                long kineticToTransfer = Math.min(remainingKinetic, maxReceive);
                if (kineticToTransfer > 0) {
                    long received = frontKinetic.receiveKinetic(kineticToTransfer, false);
                    remainingKinetic -= received;
                }
            }
        }

        if (remainingKinetic > 0) {
            for (Direction direction : Direction.values()) {
                if (direction == facing) continue;

                BlockPos adjacentPos = worldPosition.relative(direction);
                IMioIcifCapabilities.IKineticStorage adjacentKinetic = level.getCapability(
                    IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, adjacentPos, direction.getOpposite());
                if (adjacentKinetic == null) {
                    adjacentKinetic = MioIcifAPI.instance().getCapabilities().adaptKineticStorage(
                        level.getBlockEntity(adjacentPos));
                }

                if (adjacentKinetic != null && adjacentKinetic.canReceiveKinetic()) {
                    int adjacentRPM = adjacentKinetic.getRPM();
                    if (myRPM > adjacentRPM) {
                        long maxReceive = adjacentKinetic.getMaxKineticStored() - adjacentKinetic.getKineticStored();
                        long kineticToTransfer = Math.min(remainingKinetic, maxReceive);
                        if (kineticToTransfer > 0) {
                            long received = adjacentKinetic.receiveKinetic(kineticToTransfer, false);
                            remainingKinetic -= received;
                            if (remainingKinetic <= 0) break;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 检查是否正在燃烧
     */
    public boolean isBurning() {
        return burnTime > 0;
    }
    
    /**
     * 消耗燃料
     */
    protected void consumeFuel() {
 // 发电机没有存储，只要有消费者就可以消耗燃料
        ItemStack fuelStack = itemHandler.getStackInSlot(FUEL_SLOT);
        if (!fuelStack.isEmpty()) {
            int burnTime = getFuelBurnTime(fuelStack);
            if (burnTime > 0) {
                this.burnDuration = burnTime;
                this.burnTime = burnTime;

                // 消耗燃料
                fuelStack.shrink(1);

                // 如果燃料堆为空，且燃料有容器物品，则放入容器物品
                if (fuelStack.isEmpty()) {
                    itemHandler.setStackInSlot(FUEL_SLOT, fuelStack.getCraftingRemainingItem());
                }
            }
        }
    }
    
    /**
     * 获取燃料的燃烧时间，子类可以重写此方法来定义可用的燃料
     * @param fuel 燃料物品
     * @return 燃烧时间 (tick)
     */
    public abstract int getFuelBurnTime(ItemStack fuel);
    
    /**
     * 获取燃烧进度，用于GUI显示
     * @return 燃烧进度 (0-1)
     */
    public float getBurnProgress() {
        if (burnDuration == 0) {
            return 0;
        }
        return (float) burnTime / burnDuration;
    }
    
    /**
     * 更新方块状态（燃烧/未燃烧）
     * 子类可以重写此方法以更新方块外观
     */
    protected void updateBlockState(boolean isBurning) {
        // 默认实现：子类可以重写以设置 ACTIVATE 属性
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (itemHandler != null) {
            tag.put("Items", itemHandler.serializeNBT(registries));
        }
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (itemHandler != null && tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
        burnTime = tag.getInt("BurnTime");
        burnDuration = tag.getInt("BurnDuration");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (itemHandler != null) {
            tag.put("Items", itemHandler.serializeNBT(registries));
        }
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (itemHandler != null && tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
        burnTime = tag.getInt("BurnTime");
        burnDuration = tag.getInt("BurnDuration");
    }
    
    // ==================== Container 接口实现 ====================

    public MachineItemHandler getItemHandler() {
        return itemHandler;
    }

    public int getContainerSize() {
        return itemHandler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
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
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int toRemove = Math.min(amount, stack.getCount());
        ItemStack result = stack.copyWithCount(toRemove);
        if (toRemove >= stack.getCount()) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        } else {
            itemHandler.setStackInSlot(slot, stack.copyWithCount(stack.getCount() - toRemove));
        }
        return result;
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
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
    }
    
    @Override
    public boolean stillValid(Player player) {
        return level.getBlockEntity(worldPosition) == this && 
               player.distanceToSqr(worldPosition.getX() + 0.5, 
                                   worldPosition.getY() + 0.5, 
                                   worldPosition.getZ() + 0.5) <= 64.0;
    }
    
    @Override
    public void clearContent() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
    
    @Override
    public int getMaxStackSize() {
        return 64;
    }
    
    // ==================== WorldlyContainer 接口实现 ====================
    
    @Override
    public int[] getSlotsForFace(Direction side) {
        // 所有方向都可以访问燃料槽
        return new int[]{FUEL_SLOT};
    }
    
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        // 燃料槽：只接受燃料
        if (slot == FUEL_SLOT) {
            return getFuelBurnTime(stack) > 0;
        }
        return false;
    }
    
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // 燃料槽不能提取（防止漏斗吸走燃料）
        return false;
    }
    
    /**
     * 创建物品处理器（用于自动化模组）
     */
    public IItemHandler createItemHandler() {
        return new IItemHandlerModifiable() {
            @Override
            public int getSlots() {
                return itemHandler.getSlots();
            }

            @Override
            public @NotNull ItemStack getStackInSlot(int slot) {
                return itemHandler.getStackInSlot(slot);
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                if (slot != FUEL_SLOT || getFuelBurnTime(stack) <= 0) {
                    return stack;
                }

                ItemStack existing = itemHandler.getStackInSlot(slot);
                if (existing.isEmpty()) {
                    int limit = Math.min(stack.getCount(), getMaxStackSize());
                    if (!simulate) {
                        itemHandler.setStackInSlot(slot, stack.copyWithCount(limit));
                        setChanged();
                    }
                    return stack.getCount() > limit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                    int limit = Math.min(existing.getCount() + stack.getCount(), getMaxStackSize());
                    int added = limit - existing.getCount();
                    if (!simulate && added > 0) {
                        existing.grow(added);
                        setChanged();
                    }
                    return stack.getCount() > added ? stack.copyWithCount(stack.getCount() - added) : ItemStack.EMPTY;
                }
                return stack;
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                // 不允许提取燃料
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return getMaxStackSize();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return slot == FUEL_SLOT && getFuelBurnTime(stack) > 0;
            }

            @Override
            public void setStackInSlot(int slot, @NotNull ItemStack stack) {
                itemHandler.setStackInSlot(slot, stack);
            }
        };
    }

    // ==================== IKineticGeneratorBlock 接口实现 ====================

    @Override
    public int getKineticOutput() {
        return isBurning() ? kineticGenerationRate : 0;
    }

    @Override
    public boolean isGenerating() {
        return isBurning();
    }

    @Override
    public int getBurnTime() {
        return burnTime;
    }

    @Override
    public int getBurnDuration() {
        return burnDuration;
    }

    @Override
    public int getKineticGenerationRate() {
        return kineticGenerationRate;
    }

    @Override
    public int getRotorRPM() {
        return kineticStorage != null ? kineticStorage.getRPM() : 0;
    }
}