package com.singularity_iteration.mio_icif.Blocks.entity.HUEntity;

import com.singularity_iteration.mio_icif.api.machine.IBurnControl;
import com.singularity_iteration.mio_icif.api.machine.IHeatGeneratorBlock;

import com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
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
 * HU 热能发电机方块实体基类 * 模仿 mio_icif_Energy_Generator，但使用 HU 热能系统
 * 
 * 热能发电机特点：
 * - 燃烧燃料产生热能（HU）
 * - 热能会自然散失
 * - 可以向相邻方块传导热能
 * - 需要温度差才能有效传输
 */
@SuppressWarnings("null")
public abstract class mio_icif_HeatU_Generator extends mio_icif_HeatU_Block implements WorldlyContainer, IBurnControl, IHeatGeneratorBlock {

    // 物品处理器，使用统一的槽位系�
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

    // 热能生成速率 (HU/tick)
    protected final int heatGenerationRate;

    // 燃料槽索引
protected static final int FUEL_SLOT = 0;

    /**
     * 构造函数
 * @param type 方块实体类型
     * @param pos 位置
     * @param state 方块状态
 * @param layout 槽位布局
     * @param heatGenerationRate 热能生成速率 (HU/tick)
     */
    public mio_icif_HeatU_Generator(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                       SlotLayout layout, int heatGenerationRate) {
        super(type, pos, state, 20000, 0, 500, 20, 1500, 0.02f);
        this.itemHandler = createItemHandler(layout);
        this.heatGenerationRate = heatGenerationRate;
    }

    /**
     * 构造函数（支持自定义热能参数）
     * @param type 方块实体类型
     * @param pos 位置
     * @param state 方块状态
 * @param layout 槽位布局
     * @param heatGenerationRate 热能生成速率 (HU/tick)
     * @param capacity 热能容量
     * @param maxExtract 最大输出速率
     * @param maxTemp 最高温�
 */
    public mio_icif_HeatU_Generator(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                       SlotLayout layout, int heatGenerationRate,
                               int capacity, int maxExtract, int maxTemp) {
        super(type, pos, state, capacity, 0, maxExtract, 20, maxTemp, 0.02f);
        this.itemHandler = createItemHandler(layout);
        this.heatGenerationRate = heatGenerationRate;
    }

    protected MachineItemHandler createItemHandler(SlotLayout layout) {
        return new MachineItemHandler(layout);
    }
    
    /**
     * 检查前方是否有需要热量的机器
     */
    protected boolean hasHeatConsumerInFront() {
        if (level == null) return false;

        // 获取方块朝向
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos frontPos = worldPosition.relative(facing);

        // 获取前方方块的热能存储能量
    IMioIcifCapabilities.IHeatStorage frontHeat = level.getCapability(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK, frontPos, facing.getOpposite());

        // 检查前方是否有可以接收热能的机器
    if (frontHeat != null && frontHeat.canReceiveHeat()) {
            // 检查前方机器是否还需要热能（未充满
        return frontHeat.getHeatStored() < frontHeat.getMaxHeatStored();
        }

        return false;
    }

    /**
     * tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_HeatU_Generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        
        // 记录之前的燃烧状态
    boolean wasBurning = blockEntity.isBurning();
        
        // 检查热能存储是否已满
    boolean isHeatFull = blockEntity.getHeatStorage().getHeatStored() >= 
                            blockEntity.getHeatStorage().getMaxHeatStored();
        
        // 检查前方是否有需要热量的机器
        boolean hasConsumer = blockEntity.hasHeatConsumerInFront();
        
        // 如果正在燃烧，生成热�
    if (blockEntity.isBurning()) {
            // 如果热能已满或前方没有需要热量的机器，停止燃烧
        if (isHeatFull || !hasConsumer) {
                blockEntity.burnTime = 0;
            } else {
                blockEntity.generateHeat();
                blockEntity.burnTime--;
                
                // 如果燃烧完毕，尝试消耗新的燃料
            if (blockEntity.burnTime <= 0) {
                    blockEntity.consumeFuel();
                }
            }
        } else {
            // 如果没有在燃烧、热能未满且前方有需要热量的机器，尝试消耗新的燃料
        if (!isHeatFull && hasConsumer) {
                blockEntity.consumeFuel();
            }
        }
        
        // 应用热损坏
    blockEntity.applyHeatLoss();
        
        // 向相邻方块传导热�
    blockEntity.distributeHeat();
        
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
     * 生成热能
     */
    protected void generateHeat() {
        long heatGenerated = Math.min((long)heatGenerationRate, 
            getHeatStorage().getMaxHeatStored() - getHeatStorage().getHeatStored());
        getHeatStorage().generateHeatInternal(heatGenerated, false);
    }
    
    /**
     * 向相邻方块传导热能（重写以优化传输）
     */
    @Override
    protected void distributeHeat() {
        if (heatStorage.getHeatStored() <= 0) {
            return;
        }
        
        int myTemp = heatStorage.getTemperature();
        
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = worldPosition.relative(direction);
            
            // 获取相邻方块的热能存储能量
        IMioIcifCapabilities.IHeatStorage adjacentHeat = level.getCapability(
                IMioIcifCapabilities.HEAT_STORAGE_BLOCK, adjacentPos, direction.getOpposite());
            
            if (adjacentHeat != null && adjacentHeat.canReceiveHeat()) {
                int adjacentTemp = adjacentHeat.getTemperature();
                
                // 需要温度差才能传导
                if (myTemp > adjacentTemp) {
                    int tempDiff = myTemp - adjacentTemp;
                    
                    // 发电机优先输出热�
                long maxTransfer = Math.min(heatStorage.getMaxExtract(), 
                                               adjacentHeat.getMaxHeatStored() - adjacentHeat.getHeatStored());
                    long heatToTransfer = Math.min(maxTransfer, tempDiff / 5);
                
                    if (heatToTransfer > 0) {
                        long extracted = heatStorage.extractHeat(heatToTransfer, false);
                        if (extracted > 0) {
                            long received = adjacentHeat.receiveHeat(extracted, false);
                            if (received < extracted) {
                                heatStorage.receiveHeat(extracted - received, false);
                            }
                            setChanged();
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
        // 检查热能存储是否已满
    boolean isHeatFull = getHeatStorage().getHeatStored() >= 
                            getHeatStorage().getMaxHeatStored();
        
        // 如果热能已满，不消耗燃料
    if (isHeatFull) {
            return;
        }
        
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

    @Override
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
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int toRemove = Math.min(amount, stack.getCount());
        ItemStack result = stack.copyWithCount(toRemove);
        stack.shrink(toRemove);
        if (stack.isEmpty()) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }
        setChanged();
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
        // 所有方向都可以访问燃料�
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
     * 获取物品处理器（用于 capability 系统）
     * @param side 方向
     * @return 物品处理器
     */
    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return new GeneratorItemHandler(side);
    }

    // ==================== IHeatGeneratorBlock 接口实现 ====================

    @Override
    public int getHeatOutput() {
        return isBurning() ? heatGenerationRate : 0;
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
    public int getHeatGenerationRate() {
        return heatGenerationRate;
    }
    
    // ==================== GeneratorItemHandler 内部分?====================
    
    /**
     * 热能发电机的物品处理器
 * 用于 capability 系统，限制物品的插入和提取行为
 */
    protected class GeneratorItemHandler implements IItemHandlerModifiable {
        private final Direction side;

        public GeneratorItemHandler(@Nullable Direction side) {
            this.side = side;
        }

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
            if (!canPlaceItemThroughFace(slot, stack, side)) {
                return stack;
            }

            ItemStack currentStack = itemHandler.getStackInSlot(slot);
            if (currentStack.isEmpty()) {
                // 槽位为空，可以放弃
            int count = Math.min(stack.getCount(), getMaxStackSize());
                if (!simulate) {
                    itemHandler.setStackInSlot(slot, stack.copyWithCount(count));
                    setChanged();
                }
                if (count < stack.getCount()) {
                    return stack.copyWithCount(stack.getCount() - count);
                }
                return ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(currentStack, stack)) {
                // 槽位有相同物品，可以合并
                int space = getMaxStackSize() - currentStack.getCount();
                int toAdd = Math.min(stack.getCount(), space);
                if (toAdd > 0) {
                    if (!simulate) {
                        currentStack.grow(toAdd);
                        setChanged();
                    }
                    if (toAdd < stack.getCount()) {
                        return stack.copyWithCount(stack.getCount() - toAdd);
                    }
                    return ItemStack.EMPTY;
                }
            }
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!canTakeItemThroughFace(slot, itemHandler.getStackInSlot(slot), side)) {
                return ItemStack.EMPTY;
            }

            ItemStack currentStack = itemHandler.getStackInSlot(slot);
            if (currentStack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            int toExtract = Math.min(amount, currentStack.getCount());
            ItemStack extracted = currentStack.copyWithCount(toExtract);

            if (!simulate) {
                currentStack.shrink(toExtract);
                if (currentStack.isEmpty()) {
                    itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
                }
                setChanged();
            }

            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return getMaxStackSize();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return canPlaceItemThroughFace(slot, stack, side);
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            itemHandler.setStackInSlot(slot, stack);
            setChanged();
        }
    }
}