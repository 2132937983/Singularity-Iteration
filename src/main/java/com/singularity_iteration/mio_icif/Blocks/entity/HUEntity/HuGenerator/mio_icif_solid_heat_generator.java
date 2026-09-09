package com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import com.singularity_iteration.mio_icif.api.machine.IHeatGeneratorBlock;
import com.singularity_iteration.mio_icif.api.machine.IBurnControl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 固体加热机方块实体
 * 通过燃烧可燃烧物（除了岩浆）来产生热能
 *
 * 特点：
 * - 燃烧燃料产生 HU 热能
 * - 每次燃烧产生一个灰烬(item_ash)
 * - 输出 20 HU/t
 * - 不存储热能，只有前面有方块需要热能时才会燃烧
 * - 等效于火力发电加电力加热器
 */
@SuppressWarnings("null")
public class mio_icif_solid_heat_generator extends com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.mio_icif_HeatU_Block implements WorldlyContainer, IHeatGeneratorBlock, IBurnControl {

    // 槽位定义
    public static final int FUEL_SLOT = 0;
    public static final int ASH_SLOT = 1;
    public static final int TOTAL_SLOTS = 2;

    // 热能产生速率 (HU/tick)
    private static final int HEAT_GENERATION_RATE = 20;

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .fuel()
        .output(1)
        .build();

    // 燃烧时间
    private int burnTime;
    // 最大燃烧时间
    private int maxBurnTime;

    // 机器是否正在工作
    protected boolean isWorking;

    // 数据同步访问器
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> isWorking ? 1 : 0;
                case 1 -> burnTime;
                case 2 -> maxBurnTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> isWorking = value == 1;
                case 1 -> burnTime = value;
                case 2 -> maxBurnTime = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    /**
     * 构造函数
     */
    public mio_icif_solid_heat_generator(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.SOLID_HEAT_GENERATOR.get(), pos, state, LAYOUT, 1, 0, 0, 20, 1000, 0);

        this.burnTime = 0;
        this.maxBurnTime = 0;
        this.isWorking = false;
    }

    /**
     * 每 tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_solid_heat_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 处理燃烧逻辑 - 只在前面有方块需要热能时才燃烧
        blockEntity.handleBurning();

        // 更新方块状态（运行/停止）
        boolean isActive = state.getValue(com.singularity_iteration.mio_icif.Blocks.HUGenerator.mio_icif_block_solid_heat_generator.ACTIVE);
        if (blockEntity.isWorking != isActive) {
            level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.HUGenerator.mio_icif_block_solid_heat_generator.ACTIVE, blockEntity.isWorking), 3);
        }
    }

    /**
     * 检查正面是否有方块需要热能
     */
    private boolean hasHeatConsumer() {
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos adjacentPos = worldPosition.relative(facing);

        IMioIcifCapabilities.IHeatStorage adjacentHeat = level.getCapability(
                IMioIcifCapabilities.HEAT_STORAGE_BLOCK, adjacentPos, facing.getOpposite());
        if (adjacentHeat == null) {
            adjacentHeat = MioIcifAPI.instance().getCapabilities().adaptHeatStorage(
                level.getBlockEntity(adjacentPos));
        }

        if (adjacentHeat != null && adjacentHeat.canReceiveHeat()) {
            return adjacentHeat.getHeatStored() < adjacentHeat.getMaxHeatStored();
        }

        return false;
    }

    /**
     * 向正面输出热能
     */
    private void outputHeat() {
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos adjacentPos = worldPosition.relative(facing);

        IMioIcifCapabilities.IHeatStorage adjacentHeat = level.getCapability(
                IMioIcifCapabilities.HEAT_STORAGE_BLOCK, adjacentPos, facing.getOpposite());
        if (adjacentHeat == null) {
            adjacentHeat = MioIcifAPI.instance().getCapabilities().adaptHeatStorage(
                level.getBlockEntity(adjacentPos));
        }

        if (adjacentHeat != null && adjacentHeat.canReceiveHeat()) {
            long heatToOutput = Math.min((long)HEAT_GENERATION_RATE,
                    adjacentHeat.getMaxHeatStored() - adjacentHeat.getHeatStored());
            if (heatToOutput > 0) {
                adjacentHeat.receiveHeat(heatToOutput, false);
            }
        }
    }

    /**
     * 处理燃烧逻辑
     */
    private void handleBurning() {
        // 如果正在燃烧
        if (burnTime > 0) {
            // 检查前面是否还有方块需要热能
            if (hasHeatConsumer()) {
                burnTime--;
                // 直接输出热能到前面
                outputHeat();
                isWorking = true;

                // 燃烧结束，产生灰烬
                if (burnTime <= 0) {
                    produceAsh();
                    // 尝试继续燃烧下一个燃料
                    tryStartBurning();
                }
            } else {
                // 前面没有需要热能的方块，暂停燃烧但不消耗燃料
                isWorking = false;
            }
        } else {
            // 没有在燃烧，尝试开始燃烧
            tryStartBurning();
        }

        setChanged();
    }

    /**
     * 尝试开始燃烧燃料
     */
    private void tryStartBurning() {
        // If already burning, return directly
        if (burnTime > 0) {
            return;
        }

        // 检查前面是否有方块需要热能
        if (!hasHeatConsumer()) {
            isWorking = false;
            return;
        }

        ItemStack fuelStack = itemHandler.getStackInSlot(FUEL_SLOT);
        if (fuelStack.isEmpty()) {
            isWorking = false;
            return;
        }

        // 获取燃料燃烧时间
        int fuelBurnTime = getBurnTime(fuelStack);
        if (fuelBurnTime > 0) {
            // 检查灰烬槽是否有空间
            ItemStack ashStack = itemHandler.getStackInSlot(ASH_SLOT);
            if (!ashStack.isEmpty() && ashStack.getCount() >= ashStack.getMaxStackSize()) {
                // 灰烬槽满了，无法开始燃烧
                isWorking = false;
                return;
            }

            // 开始燃烧
            maxBurnTime = fuelBurnTime;
            burnTime = fuelBurnTime;

            // 消耗燃料
            fuelStack.shrink(1);
            if (fuelStack.isEmpty()) {
                itemHandler.setStackInSlot(FUEL_SLOT, ItemStack.EMPTY);
            }

            isWorking = true;
            setChanged();
        } else {
            isWorking = false;
        }
    }

    /**
     * 产生灰烬
     */
    private void produceAsh() {
        ItemStack ashStack = itemHandler.getStackInSlot(ASH_SLOT);
        ItemStack ashItem = new ItemStack(mio_icif_resources.ASH.get());

        if (ashStack.isEmpty()) {
            itemHandler.setStackInSlot(ASH_SLOT, ashItem);
        } else if (ItemStack.isSameItem(ashStack, ashItem) && ashStack.getCount() < ashItem.getMaxStackSize()) {
            ashStack.grow(1);
        }
        // 如果灰烬槽满了，不产出灰烬（燃料被浪费）
    }

    /**
     * 获取物品的燃烧时间
 * 像原版熔一样，根据物品是否可燃的标签对其进行燃烧发热
     * 排除岩浆桶
     */
    private int getBurnTime(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        // 排除岩浆桶
        if (stack.is(Items.LAVA_BUCKET)) {
            return 0;
        }

        int burnTime = stack.getBurnTime(null);
        return burnTime > 0 ? burnTime : 0;
    }

    /**
     * 检查物品是否是燃料（可燃烧且不是岩浆桶）
     */
    private boolean isFuel(ItemStack stack) {
        return getBurnTime(stack) > 0;
    }

    /**
     * 检查物品是否适合放入指定槽位
     */
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == FUEL_SLOT) {
            return isFuel(stack);
        }
        return false; // 灰烬槽不能手动放入物品
    }

    // ==================== NBT 序列化 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", itemHandler.serializeNBT(registries));
        tag.putInt("burnTime", burnTime);
        tag.putInt("maxBurnTime", maxBurnTime);
        tag.putBoolean("isWorking", isWorking);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
        burnTime = tag.getInt("burnTime");
        maxBurnTime = tag.getInt("maxBurnTime");
        isWorking = tag.getBoolean("isWorking");
    }

    // ==================== Getter 方法 ====================

    @Nullable
    @Override
    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        return null;
    }

    /**
     * 获取当前燃烧时间
     */
    public int getBurnTime() {
        return burnTime;
    }

    /**
     * 获取最大燃烧时间
     */
    public int getMaxBurnTime() {
        return maxBurnTime;
    }

    /**
     * 获取燃烧进度（0-100）
     */
    public int getBurnProgress() {
        if (maxBurnTime <= 0) {
            return 0;
        }
        return ((maxBurnTime - burnTime) * 100) / maxBurnTime;
    }

    /**
     * 检查机器是否正在工作
     */
    public boolean isWorking() {
        return isWorking;
    }

    @Nullable
    @Override
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    /**
     * 获取指定方向的物品处理器（带方向限制）
     */
    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return new SidedItemHandler(side);
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
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(
            this.worldPosition.getX() + 0.5,
            this.worldPosition.getY() + 0.5,
            this.worldPosition.getZ() + 0.5
        ) <= 64.0;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    // ==================== WorldlyContainer 接口实现 ====================

    @Override
    public int[] getSlotsForFace(Direction side) {
        // 燃料槽可以从任何方向访问，灰烬槽也可以从任何方向提取
        return new int[]{FUEL_SLOT, ASH_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        // 只能向燃料槽放入燃料
        return slot == FUEL_SLOT && isFuel(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // 只能提取灰烬槽的物品
        return slot == ASH_SLOT;
    }

    // ==================== SidedItemHandler 内部类 ====================

    /**
     * 带方向限制的 IItemHandler 包装器
     */
    @SuppressWarnings("unused")
    protected class SidedItemHandler implements IItemHandler {
        private final Direction side;

        public SidedItemHandler(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!canInsertItem(slot, stack)) {
                return stack;
            }
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!canExtractItem(slot)) {
                return ItemStack.EMPTY;
            }
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return itemHandler.isItemValid(slot, stack);
        }

        private boolean canInsertItem(int slot, ItemStack stack) {
            // 只能向燃料槽放入燃料
            return slot == FUEL_SLOT && isFuel(stack);
        }

        private boolean canExtractItem(int slot) {
            // 只能提取灰烬槽的物品
            return slot == ASH_SLOT;
        }
    }

    // ==================== MenuProvider 接口实现 ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.solid_heat_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.HUEntity.SolidHeatGeneratorMenu(containerId, playerInventory, this);
    }

    /**
     * 获取数据同步访问器
     */
    public ContainerData getContainerData() {
        return dataAccess;
    }

    // ==================== IHeatGeneratorBlock 接口实现 ====================

    @Override
    public int getHeatOutput() {
        return isWorking ? HEAT_GENERATION_RATE : 0;
    }

    @Override
    public boolean isGenerating() {
        return isWorking;
    }

    @Override
    public int getBurnDuration() {
        return maxBurnTime;
    }

    @Override
    public int getHeatGenerationRate() {
        return HEAT_GENERATION_RATE;
    }

    // ==================== IBurnControl 接口实现 ====================

    @Override
    public int getDefaultBurnTime() {
        return maxBurnTime;
    }

    @Override
    public void setBurnTime(int ticks) {
        this.burnTime = ticks;
        setChanged();
    }
}