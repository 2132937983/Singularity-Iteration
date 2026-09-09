package com.singularity_iteration.mio_icif.api.internal.machine;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotType;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.machine.ISlotLayout;
import com.singularity_iteration.mio_icif.api.machine.IWorkCompleteCallback;
import com.singularity_iteration.mio_icif.api.machine.builder.IMachineBuilderAPI;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * 通用机器方块实体（内部实现）- 供 API 构建器使用
 *
 * <p>这是一个通用的生产者机器实现，附属模组可以通过回调函数自定义行为。
 * 
 * <p><strong>这是内部实现类，不应被 Addon 直接扩展。</strong>
 * Addon 开发者应该实现自己的 BlockEntity 并使用 IMachineBuilderAPI 提供的构建器接口。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 在模组初始化阶段创建 BlockEntityType
 * public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericMachineBlockEntity>> MY_MACHINE =
 *     BLOCK_ENTITIES.register("my_machine", () ->
 *         BlockEntityType.Builder.of(GenericMachineBlockEntity::new, ModBlocks.MY_MACHINE.get()).build(null));
 *
 * // 使用 API 构建器创建机器定义
 * IMachineDefinition definition = MioIcifAPI.instance().getMachineBuilderAPI()
 *     .createElectricMachineBuilder()
 *     .setName("my_machine")
 *     .setEnergyCapacity(10000)
 *     .setEnergyPerTick(32)
 *     .setProcessTime(200)
 *     .useStandardLayout(1, 1, true, 4)
 *     .setCableTier(ICableTier tier)
 *     .withEntityType(MY_MACHINE.get())
 *     .build();
 *
 * // 设置工作处理器
 * GenericMachineBlockEntity entity = ...;
 * entity.setWorkHandler((machine, inputs) -> {
 *     // 查找配方
 *     var recipe = findRecipe(inputs[0]);
 *     if (recipe != null) {
 *         // 消耗输入物品
 *         machine.consumeInput(0, 1);
 *         // 插入输出物品
 *         machine.insertOutput(recipe.getResult());
 *     }
 * });
 * }</pre>
 */
public class GenericMachineBlockEntity extends mio_icif_producer
    implements com.singularity_iteration.mio_icif.api.machine.IMachineConfigurable {

    // 回调函数
    private BiConsumer<GenericMachineBlockEntity, ItemStack[]> workHandler;
    private Runnable tickCallback;
    private Runnable workStartCallback;
    private IWorkCompleteCallback workCompleteCallback;
    
    // 机器配置（用于API查询）
    @Nullable
    private IMachineBuilderAPI.MachineConfiguration configuration;
    
    // 跟踪上一个tick的工作状态，用于检测工作开始
    private boolean wasWorkingLastTick = false;

    /**
     * 简化构造函数 - 供 NeoForge BlockEntityType.Builder.of() 使用
     * 
     * <p>使用示例：
     * <pre>{@code
     * public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericMachineBlockEntity>> MY_MACHINE =
     *     BLOCK_ENTITIES.register("my_machine", () ->
     *         BlockEntityType.Builder.of(GenericMachineBlockEntity::new, ModBlocks.MY_MACHINE.get()).build(null));
     * }</pre>
     */
    public GenericMachineBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type);
    }

    public GenericMachineBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                     long capacity, long maxReceive, long maxExtract,
                                     int maxProgress, SlotLayout slotLayout, long energyPerTick, CableTier cableTier) {
        super(pos, state, type, capacity, maxReceive, maxExtract, maxProgress, slotLayout, energyPerTick, cableTier);
    }

    public GenericMachineBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                     long capacity, long maxReceive, long maxExtract,
                                     int maxProgress, ISlotLayout slotLayout, long energyPerTick, ICableTier cableTier) {
        super(pos, state, type, capacity, maxReceive, maxExtract, maxProgress, slotLayout, energyPerTick, cableTier);
    }

    /**
     * 设置机器配置（供构建器注入）
     */
    public void setConfiguration(@Nullable IMachineBuilderAPI.MachineConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * 获取机器配置
     */
    @Nullable
    public IMachineBuilderAPI.MachineConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * 设置工作处理器
     * @param handler 接收 (entity, inputs) 并处理配方的回调。
     *                inputs 是输入槽物品的副本，仅用于配方匹配。
     *                实际物品操作请调用 {@link #consumeInput(int, int)} 和 {@link #insertOutput(ItemStack)}。
     */
    public void setWorkHandler(BiConsumer<GenericMachineBlockEntity, ItemStack[]> handler) {
        this.workHandler = handler;
    }

    /**
     * 设置每 tick 回调（供构建器注入）
     */
    public void setTickCallback(Runnable tickCallback) {
        this.tickCallback = tickCallback;
    }

    /**
     * 设置工作开始回调（供构建器注入）
     */
    public void setWorkStartCallback(Runnable workStartCallback) {
        this.workStartCallback = workStartCallback;
    }

    /**
     * 设置工作完成回调（供构建器注入）
     */
    public void setWorkCompleteCallback(IWorkCompleteCallback workCompleteCallback) {
        this.workCompleteCallback = workCompleteCallback;
    }

    /**
     * 从指定输入槽消耗指定数量的物品
     * @param inputIndex 输入槽索引（0-based，相对于输入槽起始位置）
     * @param amount 消耗数量
     * @return 实际消耗的数量
     */
    public int consumeInput(int inputIndex, int amount) {
        int inputStart = slotLayout.getStart(SlotType.INPUT);
        int inputCount = slotLayout.getCount(SlotType.INPUT);
        if (inputIndex < 0 || inputIndex >= inputCount) return 0;
        int slot = inputStart + inputIndex;
        ItemStack stack = itemHandler.getStackInSlot(slot);
        int canExtract = Math.min(amount, stack.getCount());
        if (canExtract > 0) {
            itemHandler.extractItem(slot, canExtract, false);
        }
        return canExtract;
    }

    /**
     * 向输出槽插入物品
     * @param stack 要插入的物品
     * @return 剩余未插入的物品（空表示全部插入成功）
     */
    public ItemStack insertOutput(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int outputStart = slotLayout.getStart(SlotType.OUTPUT);
        int outputCount = slotLayout.getCount(SlotType.OUTPUT);
        ItemStack remainder = stack.copy();
        for (int i = 0; i < outputCount && !remainder.isEmpty(); i++) {
            int slot = outputStart + i;
            remainder = itemHandler.insertItem(slot, remainder, false);
        }
        return remainder;
    }

    /**
     * 获取物品处理器（可用于更复杂的物品操作）
     */
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // 默认实现：根据槽位类型返回合适的槽位
        int inputCount = slotLayout.getCount(SlotType.INPUT);
        int outputCount = slotLayout.getCount(SlotType.OUTPUT);
        int batteryCount = slotLayout.getCount(SlotType.BATTERY);
        int upgradeCount = slotLayout.getCount(SlotType.UPGRADE);

        int inputStart = slotLayout.getStart(SlotType.INPUT);
        int outputStart = slotLayout.getStart(SlotType.OUTPUT);
        int batteryStart = slotLayout.getStart(SlotType.BATTERY);

        return switch (side) {
            case DOWN -> {
                // 下方：输出槽
                int[] slots = new int[outputCount];
                for (int i = 0; i < outputCount; i++) {
                    slots[i] = outputStart + i;
                }
                yield slots;
            }
            case UP -> {
                // 上方：输入槽 + 电池槽
                int[] slots = new int[inputCount + batteryCount];
                int idx = 0;
                for (int i = 0; i < inputCount; i++) {
                    slots[idx++] = inputStart + i;
                }
                for (int i = 0; i < batteryCount; i++) {
                    slots[idx++] = batteryStart + i;
                }
                yield slots;
            }
            default -> {
                // 其他方向：所有非升级槽
                int totalSlots = slotLayout.getTotalCount();
                int[] slots = new int[totalSlots - upgradeCount];
                int idx = 0;
                for (int i = 0; i < totalSlots; i++) {
                    if (slotLayout.getType(i) != SlotType.UPGRADE) {
                        slots[idx++] = i;
                    }
                }
                yield slots;
            }
        };
    }

    @Override
    protected boolean canWork() {
        // 默认：检查输入槽是否有物品
        int inputStart = slotLayout.getStart(SlotType.INPUT);
        int inputCount = slotLayout.getCount(SlotType.INPUT);
        for (int i = 0; i < inputCount; i++) {
            if (!itemHandler.getStackInSlot(inputStart + i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doWork() {
        // 先验证配方是否有效，避免无效工作时消耗能量
        if (!canProcessWithHandler()) {
            stopWork();
            return;
        }

        // 消耗能量 (使用 extract 方法，因为 energyStorage 是 ILongEnergyStorage)
        long energyCost = getEffectiveEnergyPerTick();
        energyStorage.extract(energyCost, false);

        // 增加进度
        int progressPerTick = getProgressPerTick();
        progress += progressPerTick;

        // 检查是否完成
        if (progress >= maxProgress) {
            progress = 0;
            isWorking = true;
            onWorkComplete();
        } else {
            isWorking = true;
        }
    }

    /**
     * 检查 workHandler 是否可以处理当前输入
     * 使用模拟模式验证配方，不实际消耗物品
     */
    private boolean canProcessWithHandler() {
        if (workHandler == null) return false;
        if (getLevel() == null || getLevel().isClientSide) return false;

        int inputStart = slotLayout.getStart(SlotType.INPUT);
        int inputCount = slotLayout.getCount(SlotType.INPUT);
        
        // 检查是否有输入物品
        boolean hasInput = false;
        for (int i = 0; i < inputCount; i++) {
            if (!itemHandler.getStackInSlot(inputStart + i).isEmpty()) {
                hasInput = true;
                break;
            }
        }
        if (!hasInput) return false;

        // 检查输出槽是否有空间（粗略检查）
        int outputStart = slotLayout.getStart(SlotType.OUTPUT);
        int outputCount = slotLayout.getCount(SlotType.OUTPUT);
        for (int i = 0; i < outputCount; i++) {
            ItemStack outputStack = itemHandler.getStackInSlot(outputStart + i);
            if (outputStack.isEmpty() || outputStack.getCount() < outputStack.getMaxStackSize()) {
                return true; // 有输出空间
            }
        }
        return false;
    }

    /**
     * 工作完成时调用
     */
    protected void onWorkComplete() {
        if (getLevel() != null && !getLevel().isClientSide) {
            if (workCompleteCallback != null) {
                workCompleteCallback.onWorkComplete(getLevel(), getBlockPos(), ItemStack.EMPTY);
            }
        }
        if (workHandler == null) return;
        // 确保仅在服务端执行回调，防止客户端/服务端不同步
        if (getLevel() != null && getLevel().isClientSide) return;

        int inputStart = slotLayout.getStart(SlotType.INPUT);
        int inputCount = slotLayout.getCount(SlotType.INPUT);
        ItemStack[] inputs = new ItemStack[inputCount];
        for (int i = 0; i < inputCount; i++) {
            inputs[i] = itemHandler.getStackInSlot(inputStart + i).copy();
        }
        workHandler.accept(this, inputs);
    }

    /**
     * 获取有效的每 tick 能量消耗
     */
    public long getEffectiveEnergyPerTick() {
        return (long) (energyPerTick * upgradeStats.getEnergyUsageMultiplier());
    }

    /**
     * 获取每 tick 处理的进度
     */
    public int getProgressPerTick() {
        return Math.max(1, (int) Math.ceil(1.0 / upgradeStats.getProcessTimeMultiplier()));
    }

    // ==================== IProducerBlock 接口实现 ====================

    @Override
    public java.util.List<ItemStack> getUpgrades() {
        java.util.List<ItemStack> upgrades = new java.util.ArrayList<>();
        int start = getUpgradeSlotStart();
        int count = getUpgradeSlotCount();
        for (int i = 0; i < count; i++) {
            ItemStack stack = itemHandler.getStackInSlot(start + i);
            if (!stack.isEmpty()) {
                upgrades.add(stack);
            }
        }
        return upgrades;
    }

    @Override
    public IWorkCompleteCallback getWorkCompleteCallback() {
        return workCompleteCallback;
    }

    @Override
    protected void onTick() {
        if (getLevel() != null && !getLevel().isClientSide) {
            // 检测工作开始：从非工作状态变为工作状态
            if (isWorking && !wasWorkingLastTick && workStartCallback != null) {
                workStartCallback.run();
            }
            wasWorkingLastTick = isWorking;
            
            // 调用tick回调
            if (tickCallback != null) {
                tickCallback.run();
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        
        // 如果是通过简化构造函数创建的实例，尝试从回调注册表获取回调
        // 仅当回调尚未设置时才从注册表获取，避免覆盖已设置的回调
        if (getLevel() != null && !getLevel().isClientSide) {
            BlockEntityType<?> type = getType();
            if (type != null) {
                var callbacks = com.singularity_iteration.mio_icif.api.machine.builder.MachineBuilderAPIImpl.getCallbacks(type);
                if (callbacks != null) {
                    // 将回调转换为 Runnable 形式并注入（仅当尚未设置时）
                    if (callbacks.onWorkStart != null && this.workStartCallback == null) {
                        BlockPos pos = getBlockPos();
                        this.workStartCallback = () -> callbacks.onWorkStart.onWorkStart(getLevel(), pos);
                    }
                    if (callbacks.onWorkComplete != null && this.workCompleteCallback == null) {
                        BlockPos pos = getBlockPos();
                        this.workCompleteCallback = (level, blockPos, output) -> callbacks.onWorkComplete.onWorkComplete(level, pos, ItemStack.EMPTY);
                    }
                    if (callbacks.onTick != null && this.tickCallback == null) {
                        BlockPos pos = getBlockPos();
                        this.tickCallback = () -> callbacks.onTick.onTick(getLevel(), pos, progress, maxProgress);
                    }
                }
            }
        }
    }
}