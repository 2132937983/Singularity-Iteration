package com.singularity_iteration.mio_icif.Blocks.entity.KUEntity;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import com.singularity_iteration.mio_icif.api.item.ILatheItem;
import com.singularity_iteration.mio_icif.energy.kinetic.IKineticSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import com.singularity_iteration.mio_icif.api.machine.ISlotLayout;

/**
 * 车床方块实体
 * 对齐 IC2 原版机制
 * - 3 个槽位：车刀(0)、加工件(1)、输
2)
 * - KU 鍔ㄨ兘缂撳啿锛屾渶
10000
 * - 
tick KU 衰减 1%
 * - 从相邻动能源获取 KU
 * - 5 个位置独立加工，每个位置有厚度
 * - 车刀需要硬度大于加工件
 * - 每次加工消
1000 KU
 * - 每次加工消耗车刀 1 点耐久
 * - 有概率产
 */
@SuppressWarnings("null")
public class mio_icif_lathe extends BlockEntity implements MenuProvider, com.singularity_iteration.mio_icif.api.machine.IProducerBlock {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .tool()
        .input(1)
        .output(1)
        .build();

    // 槽位定义（按 builder 调用顺序：TOOL=0, INPUT=1, OUTPUT=2
public static final int TOTAL_SLOTS = 3;
    public static final int TOOL_SLOT = 0;
    public static final int LATHE_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;

    // KU 参数
    public static final int MAX_KU_BUFFER = 10000;
    public static final int KU_COST_PER_OPERATION = 1000;
    public static final int KU_DECAY_DIVISOR = 100; // 

    // 位置数量
    public static final int POSITION_COUNT = 5;

    // 物品存储
    protected MachineItemHandler itemHandler;
    
    // KU 缂撳啿
    public int kUBuffer = 0;

    // 是否激活工作状
protected boolean active = false;

    public mio_icif_lathe(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.LATHE_ENTITY_TYPE.get());
    }

    public mio_icif_lathe(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type, pos, state);
        this.itemHandler = new MachineItemHandler(LAYOUT) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_lathe.this.isItemValidForSlot(slot, stack));
    }

    /**
     * 
tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_lathe blockEntity) {
        if (level.isClientSide()) return;

        // 更新激活状态（KU > 500 时激活）
        boolean shouldBeActive = blockEntity.kUBuffer > 500;
        if (blockEntity.active != shouldBeActive) {
            blockEntity.active = shouldBeActive;
            blockEntity.updateBlockState();
        }

        // KU 
        blockEntity.kUBuffer = (int) (blockEntity.kUBuffer - (blockEntity.kUBuffer / 100.0f) + 0.5f);
        if (blockEntity.kUBuffer < 0) blockEntity.kUBuffer = 0;

        // 从相邻动能源获取 KU
        blockEntity.getKU();

        blockEntity.setChanged();
    }

    /**
     * 从相邻动能源获取 KU
     * 使用 KUCapabilities 系统
IKineticSource 接口
     */
    private void getKU() {
        if (level == null) return;
        
        for (Direction dir : Direction.values()) {
            BlockPos adjacentPos = worldPosition.relative(dir);
            IMioIcifCapabilities.IKineticStorage adjacentStorage = level.getCapability(
                IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, adjacentPos, dir.getOpposite());
            if (adjacentStorage == null) {
                net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(adjacentPos);
                adjacentStorage = MioIcifAPI.instance().getCapabilities().adaptKineticStorage(be);
            }
            
            if (adjacentStorage != null && adjacentStorage.canExtractKinetic()) {
                long available = Math.min(adjacentStorage.getKineticStored(), adjacentStorage.getMaxExtract());
                if (available > 0) {
                    long space = MAX_KU_BUFFER - kUBuffer;
                    if (space > 0) {
                        long toExtract = Math.min(available, space);
                        long extracted = adjacentStorage.extractKinetic(toExtract, false);
                        if (extracted > 0) {
                            kUBuffer += extracted;
                        }
                        break;
                    }
                }
            }
            
            // 同时尝试 IKineticSource 接口（直接产出的动能源，如动能发电机
    
            BlockEntity te = level.getBlockEntity(adjacentPos);
            if (te instanceof IKineticSource source) {
                if (source.isProducingKinetic(dir.getOpposite()) && source.getKineticOutput(dir.getOpposite()) > 0) {
                    int bandwidth = source.getKineticOutput(dir.getOpposite());
                    if (bandwidth > 0) {
                        int diff = Math.min(bandwidth, MAX_KU_BUFFER - kUBuffer);
                        if (diff > 0) {
                            kUBuffer += diff;
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * 处理指定位置的加
 * 
GUI 按钮触发，对
IC2 原版 process 方法
     * @param position 段位
(0-4)
     * @return 是否成功加工
     */
    public boolean process(int position) {
        if (!canWork(false)) return false;

        ItemStack latheStack = itemHandler.getStackInSlot(LATHE_SLOT);
        ItemStack toolStack = itemHandler.getStackInSlot(TOOL_SLOT);

        if (latheStack.isEmpty() || toolStack.isEmpty()) return false;
        if (!(latheStack.getItem() instanceof ILatheItem)) return false;
        if (!(toolStack.getItem() instanceof ILatheItem.ILatheTool)) return false;

        ILatheItem l = (ILatheItem) latheStack.getItem();
        ILatheItem.ILatheTool t = (ILatheItem.ILatheTool) toolStack.getItem();

        // 检查输出槽是否能容
ItemStack outputItem = l.getOutputItem(latheStack, position);
        if (!outputItem.isEmpty()) {
            ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
            if (!currentOutput.isEmpty()) {
                if (!currentOutput.is(outputItem.getItem())) return false;
                if (currentOutput.getCount() + 1 > currentOutput.getMaxStackSize()) return false;
            }
        }

        // 获取当前状态，检查该位置是否还可以加
int[] currentState = l.getCurrentState(latheStack);
        if (currentState[position] <= 1) return false;

        // 减少该位置的厚度
        l.setState(latheStack, position, currentState[position] - 1);

        // 随机产出
        if (level != null && level.random.nextFloat() < l.getOutputChance(latheStack, position)) {
            ItemStack produced = l.getOutputItem(latheStack, position);
            if (!produced.isEmpty()) {
                ItemStack out = itemHandler.getStackInSlot(OUTPUT_SLOT);
                if (out.isEmpty()) {
                    itemHandler.setStackInSlot(OUTPUT_SLOT, produced.copy());
                } else {
                    out.grow(1);
                }
            }
        }

        // 消耗车刀耐久
        t.applyCustomDamage(toolStack, 1, null);
        if (t.getCustomDamage(toolStack) >= t.getMaxCustomDamage(toolStack)) {
            itemHandler.setStackInSlot(TOOL_SLOT, ItemStack.EMPTY);
        }

        // 消
        kUBuffer -= KU_COST_PER_OPERATION;
        if (kUBuffer < 0) kUBuffer = 0;

        setChanged();
        return true;
    }

    /**
     * 检查是否能工作
     * @param power 是否忽略 KU 检
 */
    public boolean canWork(boolean power) {
        ItemStack toolStack = itemHandler.getStackInSlot(TOOL_SLOT);
        ItemStack latheStack = itemHandler.getStackInSlot(LATHE_SLOT);

        if (toolStack.isEmpty() || !(toolStack.getItem() instanceof ILatheItem.ILatheTool)) return false;
        if (latheStack.isEmpty() || !(latheStack.getItem() instanceof ILatheItem)) return false;
        if (kUBuffer < KU_COST_PER_OPERATION && !power) return false;

        ILatheItem l = (ILatheItem) latheStack.getItem();
        ILatheItem.ILatheTool t = (ILatheItem.ILatheTool) toolStack.getItem();

        // 车刀硬度必须大于加工件硬
return t.getHardness(toolStack) > l.getHardness(latheStack);
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case TOOL_SLOT -> stack.getItem() instanceof ILatheItem.ILatheTool;
            case LATHE_SLOT -> stack.getItem() instanceof ILatheItem;
            case OUTPUT_SLOT -> false;
            default -> false;
        };
    }

    protected void updateBlockState() {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (state.hasProperty(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_lathe.LIT)) {
            level.setBlock(worldPosition, state.setValue(
                com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_lathe.LIT, active), 3);
        }
    }

    // ========== 对外接口 ==========

    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return itemHandler;
    }

    public MachineItemHandler getItemHandler() {
        return itemHandler;
    }

    public boolean isActive() { return active; }
    
    /**
     * 鑾峰彇 KU 缂撳啿锛堜緵 GUI 鏄剧ず
 */
    public int getKUBuffer() { return kUBuffer; }
    
    public int getMaxKUBuffer() { return MAX_KU_BUFFER; }

    /**
     * 获取加工件当前状态（
GUI 显示
 */
    public int[] getLatheState() {
        ItemStack stack = itemHandler.getStackInSlot(LATHE_SLOT);
        if (stack.isEmpty() || !(stack.getItem() instanceof ILatheItem)) {
            return new int[0];
        }
        return ((ILatheItem) stack.getItem()).getCurrentState(stack);
    }

    /**
     * 获取加工件宽度（
GUI 显示
 */
    public int getLatheWidth() {
        ItemStack stack = itemHandler.getStackInSlot(LATHE_SLOT);
        if (stack.isEmpty() || !(stack.getItem() instanceof ILatheItem)) {
            return 0;
        }
        return ((ILatheItem) stack.getItem()).getWidth(stack);
    }

    /**
     * 获取加工件纹理（
GUI 渲染
 */
    @Nullable
    public net.minecraft.resources.ResourceLocation getLatheTexture() {
        ItemStack stack = itemHandler.getStackInSlot(LATHE_SLOT);
        if (stack.isEmpty() || !(stack.getItem() instanceof ILatheItem)) {
            return null;
        }
        return ((ILatheItem) stack.getItem()).getTexture(stack);
    }

    // ==================== IProducerBlock API ====================

    @Override
    public void forceStartWork() {
        this.active = true;
    }

    @Override
    public void forceStopWork() {
        this.active = false;
    }

    @Override
    public com.singularity_iteration.mio_icif.api.machine.IMachineUpgradeStats getUpgradeStats() {
        return com.singularity_iteration.mio_icif.Items.Upgrade.MachineUpgradeStats.empty();
    }

    @Override
    public int getUpgradeSlotStart() {
        return -1;
    }

    @Override
    public int getUpgradeSlotCount() {
        return 0;
    }

    @Override
    public java.util.List<ItemStack> getUpgrades() {
        return java.util.Collections.emptyList();
    }

    @Override
    public int getProgress() {
        return 0;
    }

    @Override
    public int getMaxProgress() {
        return 1;
    }

    @Override
    public int getBaseMaxProgress() {
        return 1;
    }

    @Override
    public long getEnergyPerTick() {
        return 0;
    }

    @Override
    public ISlotLayout getSlotLayout() {
        return LAYOUT;
    }

    @Override
    public int getContainerSize() {
        return itemHandler.getSlots();
    }

    @Override
    public com.singularity_iteration.mio_icif.api.energy.IEnergyStorageAccess getEnergyStorage() {
        return null;
    }

    @Override
    public com.singularity_iteration.mio_icif.api.machine.IWorkCompleteCallback getWorkCompleteCallback() {
        return null;
    }

    @Override
    public boolean isWorking() {
        return active;
    }

    @Override
    public long getEffectiveEnergyPerTick() {
        return 0;
    }

    @Override
    public long getTotalProcessed() {
        return 0;
    }

    // ========== NBT 持久

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", itemHandler.serializeNBT(registries));
        tag.putInt("kUBuffer", kUBuffer);
        tag.putBoolean("active", active);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("items"));
        }
        kUBuffer = tag.getInt("kUBuffer");
        active = tag.getBoolean("active");
    }

    // ========== 菜单/显示名称 ==========

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.lathe");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.LatheMenu(id, playerInventory, this);
    }
}