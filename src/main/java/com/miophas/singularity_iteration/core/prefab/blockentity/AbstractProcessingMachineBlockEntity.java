package com.miophas.singularity_iteration.core.prefab.blockentity;

import com.miophas.singularity_iteration.core.prefab.inventory.ISlotValidator;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotType;
import com.miophas.singularity_iteration.core.prefab.item.EmergencyChargePolicy;
import com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.energy.IEnergyTileAccess;
import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import com.miophas.singularity_iteration.core.api.machine.IMachineUpgradeStats;
import com.miophas.singularity_iteration.core.api.machine.ISlotLayout;
import com.miophas.singularity_iteration.core.api.machine.IWorkCompleteCallback;
import com.miophas.singularity_iteration.core.api.recipe.IRecipeAPI;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;

import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;

/**
 * 生产者机器基类
 * 所有生产用机器的基类，提供工作进度等基础功能
 * 电器类型，使用电力而非燃料
 * 实现 Container 和 WorldlyContainer 接口以支持漏斗交互
 * 使用 EU (Energy Unit) 能量系统
 *
 * <p>配方查找和能量操作通过 API 层进行，子类可以覆盖受保护的方法
 * 来改变行为，Addon 可以安全地扩展机器行为。
 */
@SuppressWarnings("null")
public abstract class AbstractProcessingMachineBlockEntity extends AbstractEnergyBlockEntity implements WorldlyContainer, ISlotValidator, com.miophas.singularity_iteration.core.api.machine.IProducerBlock {
    private static final List<Direction> ALL_DIRECTIONS = List.of(Direction.values());
    private final com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent fluidAutomation =
        new com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent(
            new com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent.Host() {
                @Override public Level level() { return level; }
                @Override public BlockPos worldPosition() { return worldPosition; }
                @Override public IFluidHandler ownFluidHandler() { return getFluidHandlerCapability(null); }
                @Override public IFluidHandler adjacentFluidHandler(BlockPos pos, Direction side) {
                    return getAdjacentFluidHandler(pos, side);
                }
                @Override public com.miophas.singularity_iteration.core.api.machine.IMachineUpgradeStats upgradeStats() {
                    recalculateUpgradeStats();
                    return upgradeStats;
                }
                @Override public void markUnsaved() { ContainerToTank.markUnsaved(AbstractProcessingMachineBlockEntity.this); }
                @Override public void markNeighborUnsaved(BlockPos pos) {
                    com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent
                        .markNeighborUnsaved(level, pos);
                }
            });
    private final com.miophas.singularity_iteration.core.runtime.processing.ItemTransferBuffer scexItemOutput = new com.miophas.singularity_iteration.core.runtime.processing.ItemTransferBuffer(() -> ContainerToTank.markUnsaved(this));
    private final com.miophas.singularity_iteration.core.runtime.processing.ItemTransferBuffer scexItemInput = new com.miophas.singularity_iteration.core.runtime.processing.ItemTransferBuffer(() -> ContainerToTank.markUnsaved(this));
    private boolean automationRunning;
    private long automationRetryAfter;


    protected final SlotLayout slotLayout;
    protected MachineItemHandler itemHandler;
    // Capability wrappers are immutable views; reuse one per side instead of allocating on every lookup.
    private final EnumMap<Direction, SidedItemHandler> sidedItemHandlers = new EnumMap<>(Direction.class);
    private SidedItemHandler unsidedItemHandler;

    protected int progress;
    protected int maxProgress;
    protected final int baseMaxProgress;
    protected final long baseMaxReceive;

    protected final long baseCapacity;

    protected boolean isWorking;

    // 输入槽物品变化检测（用于重置进度）
    protected ItemStack[] lastInputStacks;

    public static final int WORK_SOUND_INTERVAL = 40;
    protected int workSoundTick;

    protected final long energyPerTick;
    protected final int slotCount;
    // SlotLayout is immutable. Keep the hot-path index arrays once per machine instead
    // of allocating a new int[] for every recipe and automation check.
    private final int[] inputSlotIndices;
    private final int[] outputSlotIndices;

    protected IMachineUpgradeStats upgradeStats = MachineUpgradeStats.empty();
    // Upgrade slots change infrequently; rebuild the immutable snapshot only after a slot mutation.
    private boolean upgradeStatsDirty = true;

    // ==================== API 方法层 ====================
    // 所有配方和能量操作通过这些受保护的方法进行，子类可以覆盖以改变行为

    /**
     * 获取配方 API 实例
     */
    protected IRecipeAPI getRecipeAPI() {
        return MioIcifAPI.instance().getRecipeAPI();
    }

    /**
     * 获取物品 API 实例
     */
    protected IItemAPI getItemAPI() {
        return MioIcifAPI.instance().getItemAPI();
    }

    /**
     * 获取能量 API 访问接口（通过父类）
     */
    @Override
    protected IEnergyTileAccess getEnergyAPI() {
        return super.getEnergyAPI();
    }

    /**
     * 通过 API 查找配方
     * 子类可以覆盖此方法以提供自定义配方查找逻辑
     */
    @SuppressWarnings("deprecation")
    protected <T extends net.minecraft.world.item.crafting.Recipe<?>> java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<T>> apiFindRecipe(
            net.minecraft.world.item.crafting.RecipeType<T> type, ItemStack input, Level level) {
        return getRecipeAPI().findRecipe(type, input, level);
    }

    /**
     * 通过 API 获取配方输出
     */
    protected ItemStack apiGetRecipeOutput(net.minecraft.world.item.crafting.RecipeHolder<?> recipe) {
        return getRecipeAPI().getRecipeOutput(recipe);
    }

    /**
     * 通过 API 获取配方处理时间
     */
    protected int apiGetRecipeProcessTime(net.minecraft.world.item.crafting.RecipeHolder<?> recipe) {
        return getRecipeAPI().getRecipeProcessTime(recipe);
    }

    /**
     * 通过 API 获取配方能耗
     */
    protected long apiGetRecipeEnergyCost(net.minecraft.world.item.crafting.RecipeHolder<?> recipe) {
        return getRecipeAPI().getRecipeEnergyCost(recipe);
    }

    /**
     * 通过 API 检查物品是否为电池
     * 子类可以覆盖此方法以支持自定义电池类型
     */
    protected boolean apiIsBattery(ItemStack stack) {
        return getItemAPI().isBattery(stack);
    }

    /**
     * 通过 API 为电池充电
     */
    protected long apiChargeBattery(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().chargeBattery(stack, amount, simulate);
    }

    /**
     * 通过 API 从电池放电
     */
    protected long apiDischargeBattery(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().dischargeBattery(stack, amount, simulate);
    }

    /**
     * 通过 API 获取电池容量
     */
    protected long apiGetBatteryCapacity(ItemStack stack) {
        return getItemAPI().getBatteryCapacity(stack);
    }

    /**
     * 通过 API 获取电池当前能量
     */
    protected long apiGetBatteryStored(ItemStack stack) {
        return getItemAPI().getBatteryStored(stack);
    }

    /**
     * 通过 API 获取电池充电速率
     */
    protected long apiGetChargeRate(ItemStack stack) {
        return getItemAPI().getChargeRate(stack);
    }

    /**
     * 通过 API 为电力护甲充电
     */
    protected long apiChargeElectricArmor(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().chargeElectricArmor(stack, amount, simulate);
    }

    /**
     * 通过 API 从电力护甲放电
     */
    protected long apiDischargeElectricArmor(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().dischargeElectricArmor(stack, amount, simulate);
    }

    /**
     * 通过 API 检查是否为电力护甲
     */
    protected boolean apiIsElectricArmor(ItemStack stack) {
        return getItemAPI().isElectricArmor(stack);
    }

    /**
     * 通过 API 检查是否为电动工具
     */
    protected boolean apiIsElectricTool(ItemStack stack) {
        return getItemAPI().isElectricTool(stack);
    }

    /**
     * 通过 API 消耗能量（内部做功）
     * 使用 apiUseEnergy 绕过 maxExtract 限制
     */
    protected boolean apiConsumeEnergy(long amount) {
        long consumed = apiUseEnergy(amount, false);
        return consumed >= amount;
    }

    /**
     * 通过 API 获取当前存储能量
     * 直接访问 energyStorage，避免通过 getEnergyAPI() 造成无限递归
     */
    @Override
    protected long apiGetStoredEnergy() {
        return energyStorage.getAmount();
    }

    /**
     * 通过 API 接收能量
     * 直接访问 energyStorage，避免通过 getEnergyAPI() 造成无限递归
     */
    @Override
    protected long apiReceiveEnergy(long amount, boolean simulate) {
        return energyStorage.receive(amount, simulate);
    }

    /**
     * 简化构造函数 - 供 NeoForge BlockEntityType.Builder.of() 使用
     *
     * <p>此构造函数使用默认配置，适用于 GenericMachineBlockEntity 等需要通过
     * BlockEntityType 创建的机器。实际配置应在 onLoad() 时从 MachineRegistry 获取。
     */
    public AbstractProcessingMachineBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        this(pos, state, type, 10000, 100, 100, 200,
             SlotLayout.builder().input(1).output(1).build(), 10, CableTier.LV);
    }

    public AbstractProcessingMachineBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type,
                             long capacity, long maxReceive, long maxExtract,
                              int maxProgress, SlotLayout slotLayout, long energyPerTick, CableTier cableTier) {
        super(pos, state, type, capacity, maxReceive, maxExtract, cableTier);
        this.baseMaxProgress = maxProgress;
        this.baseMaxReceive = maxReceive;
        this.maxProgress = maxProgress;
        this.baseCapacity = capacity;
        this.energyPerTick = energyPerTick;
        this.slotLayout = slotLayout;
        this.slotCount = slotLayout.getTotalSlots();
        this.inputSlotIndices = slotLayout.getSlotsOfType(SlotType.INPUT);
        this.outputSlotIndices = slotLayout.getSlotsOfType(SlotType.OUTPUT);
        this.progress = 0;
        this.isWorking = false;
        this.workSoundTick = 0;
        this.lastInputStacks = null;

        this.setAsConsumer();

        this.itemHandler = createItemHandler(slotLayout);
    }

    public AbstractProcessingMachineBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type,
                             long capacity, long maxReceive, long maxExtract,
                             int maxProgress, ISlotLayout slotLayout, long energyPerTick, ICableTier cableTier) {
        this(pos, state, type, capacity, maxReceive, maxExtract, maxProgress,
             SlotLayout.fromISlotLayout(slotLayout), energyPerTick, CableTier.fromICableTier(cableTier));
    }



    protected MachineItemHandler createItemHandler(SlotLayout layout) {
        MachineItemHandler handler = new MachineItemHandler(layout) {
            @Override protected void onContentsChanged(int slot) {
                AbstractProcessingMachineBlockEntity.this.onInventoryChanged(slot);
            }
        };
        handler.setValidator(this);
        return handler;
    }

    /** Custom inventories must propagate this hook, including deserialization and atomic commits. */
    protected final void onInventoryChanged(int slot) {
        if (slotLayout.isType(slot, SlotType.UPGRADE)) upgradeStatsDirty = true;
        setChanged();
    }

    @Deprecated
    protected void initUpgradeSlots() {
    }

    @Deprecated
    protected void setUpgradeSlots(int start, int count) {
    }

    @Override
    public ISlotLayout getSlotLayout() {
        return slotLayout;
    }

    @Override
    public boolean isValidForSlot(int slot, ItemStack stack, SlotType type) {
        return isItemValidForSlot(slot, stack);
    }

    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    protected int getSlotLimit(int slot) {
        SlotType type = slotLayout.getInternalSlotType(slot);
        // 扫描槽、记忆槽、工具槽等限制只能放1个物品
        if (type == SlotType.SCANNER || type == SlotType.MEMORY || type == SlotType.TOOL) {
            return 1;
        }
        return 64;
    }

    /**
     * 获取指定方向可访问的槽位
     * 子类必须实现此方法以定义各方向可访问的槽位
     * @param side 方向
     * @return 可访问的槽位数组
     */
    protected abstract int[] getSlotsForDirection(Direction side);

    /**
     * 检查指定槽位是否可以从指定方向插入物品
     * 基于 SlotLayout 自动判断槽位类型
     */
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        SlotType type = slotLayout.getInternalSlotType(slot);
        if (type == null) return false;

        if (!itemHandler.isItemValid(slot, stack)) {
            return false;
        }

        ItemStack currentStack = itemHandler.getStackInSlot(slot);
        if (!currentStack.isEmpty()) {
            if (!ItemStack.isSameItem(currentStack, stack)) {
                return false;
            }
            if (currentStack.getCount() >= currentStack.getMaxStackSize()) {
                return false;
            }
        }

        return switch (type) {
            case INPUT -> !isBattery(stack);
            case BATTERY -> isBattery(stack);
            case UPGRADE -> false; // 升级槽不允许自动化插入（与IC2原版一致）
            case OUTPUT -> false;
            case FILTER -> isItemValidForSlot(slot, stack);
            default -> isItemValidForSlot(slot, stack);
        };
    }

    protected boolean isUpgradeSlot(int slot) {
        return slotLayout.isType(slot, SlotType.UPGRADE);
    }

    /** 保留原常量供兼容；实际策略见 {@link EmergencyChargePolicy}。 */
    public static final long REDSTONE_ENERGY_VALUE = EmergencyChargePolicy.DEFAULT_ENERGY_VALUE;

    protected boolean isBattery(ItemStack stack) {
        return getItemAPI().isBattery(stack)
            || EmergencyChargePolicy.applies(stack)
            || com.miophas.singularity_iteration.core.runtime.energy.FeMachineBridge.dischargeable(stack);
    }

    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        SlotType type = slotLayout.getInternalSlotType(slot);
        return type == SlotType.OUTPUT;
    }

    protected int getBatterySlot() {
        return slotLayout.getStart(SlotType.BATTERY);
    }

    /**
     * 每tick更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, AbstractProcessingMachineBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 刷新升级插件统计信息并应用相关效果?
        blockEntity.recalculateUpgradeStats();

        // 调用父类的能量传输逻辑
        AbstractEnergyBlockEntity.tick(level, pos, state, blockEntity);

        // 检测输入槽物品变化，如果变化则重置进度
        blockEntity.checkInputChanged();

        // 子类每 tick 钩子（例如 GenericMachineBlockEntity 的 tick 回调）
        blockEntity.onTick();

        // 红石信号反转升级：控制机器是否被红石信号阻止
        if (!blockEntity.canWorkRedstone()) {
            blockEntity.stopWork();
        } else {
            blockEntity.tickProduction();
        }

        // 处理电池槽放电（仅从电池获取能量，不给电池充电）
        blockEntity.handleBatterySlot();

        // 处理自动化升级（弹出/抽取等）
        // 注意：即使机器因红石信号停止工作，自动化升级仍应继续运行
        blockEntity.handleAutomationUpgrades();

        // 工作音效：机器工作时周期性播放
        if (blockEntity.isWorking) {
            blockEntity.workSoundTick++;
            if (blockEntity.workSoundTick >= WORK_SOUND_INTERVAL) {
                blockEntity.workSoundTick = 0;
                var workSound = blockEntity.getWorkSound();
                if (workSound != null) level.playSound(null, pos, workSound,
                    SoundSource.BLOCKS, 0.5F, 1.0F);
            }
        } else {
            blockEntity.workSoundTick = 0;
        }
    }

    /** Override for an addon-specific sound, or return null for a silent machine. */
    protected @org.jetbrains.annotations.Nullable net.minecraft.sounds.SoundEvent getWorkSound() {
        return MachinePresentation.workSound();
    }

    // SCEX: extension point for machines with a measured, discrete operation cycle.
    // Keep the existing scheduler for machines with their own heat/fluid/continuous logic.
    protected void tickProduction() {
        if (canWork()) {
            doWork();
        } else {
            if (shouldResetProgress()) progress = 0;
            stopWork();
        }
        updateProgress();
    }

    /**
     * 刷新升级插件统计信息
     * 超频升级：每 tick 处理更多进度，消耗更多EU，电压等级不变
     */
    protected void recalculateUpgradeStats() {
        if (!upgradeStatsDirty) return;
        int upgradeStart = slotLayout.getStart(SlotType.UPGRADE);
        int upgradeCount = slotLayout.getCount(SlotType.UPGRADE);
        this.upgradeStats = MachineUpgradeStats.fromInventory(itemHandler, upgradeStart, upgradeCount);
        updateProcessingParameters();
        long newCapacity = getProcessingCapacity();
        IEnergyTileAccess api = getEnergyAPI();
        if (newCapacity != api.getMaxEnergy()) {
            api.setCapacity(newCapacity);
        }

        long effectiveMaxReceive = getEffectiveMaxReceive();
        if (effectiveMaxReceive != api.getMaxReceive()) {
            energyStorage.setMaxReceive(effectiveMaxReceive);
        }
        upgradeStatsDirty = false;
    }

    protected long getProcessingCapacity() {
        return baseCapacity + upgradeStats.getEnergyCapacityBonus();
    }

    protected void updateProcessingParameters() {
        // 超频升级改为1tick 增加更多进度，而不是减少最大进度
        // 因此这里只需要把旧存档可能被改过的maxProgress 恢复成基础值
        if (this.maxProgress != this.baseMaxProgress) {
            this.maxProgress = this.baseMaxProgress;
            if (this.progress > this.maxProgress) {
                this.progress = this.maxProgress;
            }
        }
    }

    /**
     * 获取每tick 增加的工作进度（受超频升级影响）
     * 超频升级使机器在 1 tick 内处理多个进度，同时消耗更多EU，但电压等级不变
     */
    protected int getProgressPerTick() {
        return Math.max(1, (int) Math.ceil(1.0 / upgradeStats.getProcessTimeMultiplier()));
    }

    /**
     * 获取当前升级统计信息
     */
    @Override
    public IMachineUpgradeStats getUpgradeStats() {
        return upgradeStats;
    }

    /**
     * 获取升级插件槽位起始索引
     */
    public int getUpgradeSlotStart() {
        return slotLayout.getStart(SlotType.UPGRADE);
    }

    /**
     * 获取升级插件槽位数量
     */
    public int getUpgradeSlotCount() {
        return slotLayout.getCount(SlotType.UPGRADE);
    }

    /**
     * 检查机器在红石控制下是否可以工作
     * 默认：无红石信号时工作，有红石信号时停止
     * 红石信号反转升级：有红石信号时工作，无信号时停止
     */
    protected boolean canWorkRedstone() {
        if (level == null) {
            return true;
        }
        if (isRemotelyDisabled()) {
            return false;
        }
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (upgradeStats.isRedstoneInverted()) {
            return powered;
        }
        return !powered;
    }

    /**
     * 获取输入槽位索引数组，子类可覆盖
     */
    protected int[] getInputSlots() {
        return inputSlotIndices;
    }

    /**
     * 获取输出槽位索引数组，子类可覆盖
     */
    protected int[] getOutputSlots() {
        return outputSlotIndices;
    }

    /**
     * 处理自动化升级插件
     * 子类可以覆盖以添加更具体的流体/物品交互
     */
    protected void handleAutomationUpgrades() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (automationRunning || level.getGameTime() < automationRetryAfter) return;
        automationRunning = true;
        try {
        // These items/fluids already belong to us; delivery must not depend on
        // the continued existence (or slot count) of the external source.
        if (scexItemInput.hasPending()) {
            for (int input : getInputSlots()) {
                scexItemInput.deliverPending(itemHandler, input, Integer.MAX_VALUE);
                if (!scexItemInput.hasPending()) break;
            }
        }
        if (fluidAutomation.inputBuffer().hasPending()) {
            IFluidHandler target = this instanceof com.miophas.singularity_iteration.core.api.fluid.ISeparateFluidPorts canner
                ? canner.getInputFluidTank() : getFluidHandlerCapability(null);
            if (target != null) fluidAutomation.inputBuffer().deliverPending(target, Integer.MAX_VALUE, () -> !isRemoved());
        }
        if (isRemoved()) return;
        // Fast path: most machines carry no automation upgrade. Skip the registry lookup,
        // slot scan and neighbour capability queries below entirely.
        if (!hasAutomationUpgrade()) return;
        // tick() refreshes the immutable snapshot after an upgrade-slot mutation.
        if (hasMeasuredItemAutomation()) {
            transferMeasuredItems();
            handleFluidUpgrades();
            return;
        }
        if (upgradeStats.getEjectorCount() > 0) {
            ejectItems(upgradeStats.getEjectorCount());
        }
        if (upgradeStats.getPullingCount() > 0) {
            pullItems(upgradeStats.getPullingCount());
        }
        handleFluidUpgrades();
        } catch (RuntimeException externalFailure) {
            // Buffers retain durable uncertainty; an untrusted handler must not crash the tile tick.
            automationRetryAfter = level.getGameTime() + 20;
        } finally { automationRunning = false; }
    }

    // R27: normal-game observations, four basic processors only. Each upgrade
    // slot acts separately; the sampled budgets are 1, 4, 16 and a full stack.
    // Keep other SI machines and extended upgrade stacks on their existing path.
    private boolean hasAutomationUpgrade() {
        return upgradeStats.getEjectorCount() > 0 || upgradeStats.getPullingCount() > 0
            || upgradeStats.getFluidEjectorCount() > 0 || upgradeStats.getFluidPullingCount() > 0;
    }

    /** The block of a tile never changes, so the registry-profile answer is memoised (null = unknown). */
    private @Nullable Boolean standardProcessor;

    private boolean hasMeasuredItemAutomation() {
        if (standardProcessor == null) {
            var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock());
            standardProcessor = com.miophas.singularity_iteration.core.api.energy.EnergyNodeRegistry.standardProcessor(id);
        }
        if (!standardProcessor) return false;
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            if (itemHandler.getStackInSlot(slot).getCount() > 64) return false;
        }
        return true;
    }

    private static final Direction[] OBSERVED_TRANSFER_FACES = {
        Direction.EAST, Direction.WEST, Direction.SOUTH,
        Direction.NORTH, Direction.UP, Direction.DOWN
    };

    private void transferMeasuredItems() {
        if (upgradeStats.getEjectorCount() == 0 && upgradeStats.getPullingCount() == 0) return;
        IItemAPI api = getItemAPI();
        int start = getUpgradeSlotStart();
        int end = Math.min(start + getUpgradeSlotCount(), itemHandler.getSlots());
        for (int slot = start; slot < end; slot++) {
            ItemStack upgrade = itemHandler.getStackInSlot(slot);
            if (upgrade.isEmpty()) continue;
            String type = api.getUpgradeType(upgrade);
            boolean eject = "ejector".equals(type);
            if (!eject && !"pulling".equals(type)) continue;
            int budget = switch (upgrade.getCount()) {
                case 1 -> 1;
                case 2 -> 4;
                case 3 -> 16;
                default -> 64;
            };
            Direction selected = api.getUpgradeDirection(upgrade);
            for (Direction face : OBSERVED_TRANSFER_FACES) {
                if (selected != null && selected != face) continue;
                IItemHandler adjacent = getAdjacentItemHandler(worldPosition.relative(face), face.getOpposite());
                if (adjacent == null) continue;
                if (eject) {
                    for (int output : getOutputSlots()) {
                        if (scexItemOutput.move(itemHandler, output, adjacent, -1, budget) > 0)
                            markItemTransferSaved(worldPosition.relative(face));
                    }
                } else {
                    int remaining = budget;
                    for (int sourceSlot = 0; sourceSlot < adjacent.getSlots() && remaining > 0; sourceSlot++) {
                        for (int input : getInputSlots()) {
                            int moved = scexItemInput.move(adjacent, sourceSlot, itemHandler, input, remaining);
                            if (moved <= 0) continue;
                            remaining -= moved;
                            markItemTransferSaved(worldPosition.relative(face));
                            break;
                        }
                    }
                }
            }
        }
    }

    private void markItemTransferSaved(BlockPos neighborPos) {
        // Also mark the neighbor: third-party inventory handlers may not notify their owner.
        // Keep both sides of the completed transfer eligible for saving.
        setChanged();
        BlockEntity neighbor = level.getBlockEntity(neighborPos);
        if (neighbor != null) neighbor.setChanged();
    }

    /**
     * 弹出升级：将输出槽物品推送到相邻容器
     * @param upgradeCount 弹出升级数量
     */
    protected void ejectItems(int upgradeCount) {
        int maxPerTick = Math.max(1, upgradeCount);
        List<Direction> dirs = upgradeStats.getEjectorDirections();
        Iterable<Direction> targetDirs = !dirs.isEmpty() ? dirs : ALL_DIRECTIONS;
        for (int outputSlot : getOutputSlots()) {
            ItemStack stack = itemHandler.getStackInSlot(outputSlot);
            if (stack.isEmpty() && !scexItemOutput.hasPending()) {
                continue;
            }
            for (Direction dir : targetDirs) {
                IItemHandler target = getAdjacentItemHandler(worldPosition.relative(dir), dir.getOpposite());
                if (target == null) {
                    continue;
                }
                int moved = scexItemOutput.move(itemHandler, outputSlot, target, -1, maxPerTick);
                if (moved > 0) {
                    markItemTransferSaved(worldPosition.relative(dir));
                    stack = itemHandler.getStackInSlot(outputSlot);
                    if (stack.isEmpty() && !scexItemOutput.hasPending()) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * 抽取升级：从相邻容器拉取物品到输入槽
     * @param upgradeCount 抽取升级数量
     */
    protected void pullItems(int upgradeCount) {
        int maxPerTick = Math.max(1, upgradeCount);
        List<Direction> dirs = upgradeStats.getPullingDirections();
        Iterable<Direction> targetDirs = !dirs.isEmpty() ? dirs : ALL_DIRECTIONS;
        for (int inputSlot : getInputSlots()) {
            ItemStack current = itemHandler.getStackInSlot(inputSlot);
            if (current.getCount() >= itemHandler.getSlotLimit(inputSlot)) {
                continue;
            }
            for (Direction dir : targetDirs) {
                IItemHandler source = getAdjacentItemHandler(worldPosition.relative(dir), dir.getOpposite());
                if (source == null) {
                    continue;
                }
                boolean pulled = false;
                for (int i = 0; i < source.getSlots(); i++) {
                    if (scexItemInput.move(source, i, itemHandler, inputSlot, maxPerTick) > 0) {
                        markItemTransferSaved(worldPosition.relative(dir));
                        pulled = true;
                        break;
                    }
                }
                if (pulled) {
                    break;
                }
            }
        }
    }

    /** Called only for block replacement, never for chunk unload or normal save. */
    public final void dropPendingItems() {
        if (level == null || level.isClientSide) return;
        java.util.function.Consumer<ItemStack> drop = stack -> net.minecraft.world.Containers.dropItemStack(
                level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
        scexItemOutput.closeForRemoval(drop);
        scexItemInput.closeForRemoval(drop);
    }

    /**
     * 获取相邻位置的物品处理器
     */
    @Nullable
    protected IItemHandler getAdjacentItemHandler(BlockPos pos, @Nullable Direction side) {
        if (side != null && level instanceof net.minecraft.server.level.ServerLevel server
                && pos.equals(worldPosition.relative(side.getOpposite()))) {
            var cache = itemNeighbourCaches[side.ordinal()];
            if (cache == null) {
                cache = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(
                    Capabilities.ItemHandler.BLOCK, server, pos.immutable(), side);
                itemNeighbourCaches[side.ordinal()] = cache;
            }
            return withBlockEntity(pos, cache.getCapability());
        }
        if (level == null || !level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return null;
        }
        BlockEntity target = level.getBlockEntity(pos);
        if (target == null) {
            return null;
        }
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
    }

    /*
     * Neighbour capability caches, one per face, created lazily on the server. NeoForge
     * invalidates them itself when the neighbour changes, so the per-tick automation path no
     * longer resolves capability providers on every call. Indexed by the side passed to the
     * getter (the neighbour's face), i.e. Direction.ordinal().
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private final net.neoforged.neoforge.capabilities.BlockCapabilityCache<IItemHandler, Direction>[] itemNeighbourCaches =
        new net.neoforged.neoforge.capabilities.BlockCapabilityCache[6];
    @SuppressWarnings({"unchecked", "rawtypes"})
    private final net.neoforged.neoforge.capabilities.BlockCapabilityCache<IFluidHandler, Direction>[] fluidNeighbourCaches =
        new net.neoforged.neoforge.capabilities.BlockCapabilityCache[6];

    /**
     * Keeps the historical contract that only block entities (not block-only providers)
     * are automation targets. The cache has already confirmed the chunk is loaded.
     */
    @Nullable
    private <T> T withBlockEntity(BlockPos pos, @Nullable T capability) {
        return capability != null && level.getBlockEntity(pos) != null ? capability : null;
    }

    /**
     * 获取相邻位置的流体处理器
     */
    @Nullable
    protected IFluidHandler getAdjacentFluidHandler(BlockPos pos, @Nullable Direction side) {
        if (side != null && level instanceof net.minecraft.server.level.ServerLevel server
                && pos.equals(worldPosition.relative(side.getOpposite()))) {
            var cache = fluidNeighbourCaches[side.ordinal()];
            if (cache == null) {
                cache = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(
                    Capabilities.FluidHandler.BLOCK, server, pos.immutable(), side);
                fluidNeighbourCaches[side.ordinal()] = cache;
            }
            return withBlockEntity(pos, cache.getCapability());
        }
        if (level == null || !level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return null;
        }
        BlockEntity target = level.getBlockEntity(pos);
        if (target == null) {
            return null;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
    }

    /**
     * 处理流体自动化升级
     * 子类如有多个流体槽或特殊输出方向可覆盖
     */
    protected void handleFluidUpgrades() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (this instanceof com.miophas.singularity_iteration.core.api.fluid.IFluidPorts ports) {
            transferMeasuredCannerFluids(ports);
            return;
        }
        IFluidHandler own = getFluidHandlerCapability(null);
        if (own == null || own.getTanks() == 0) {
            return;
        }

        int ejectorCount = upgradeStats.getFluidEjectorCount();
        if (ejectorCount > 0) {
            ejectFluids(own, ejectorCount);
        }
        int pullingCount = upgradeStats.getFluidPullingCount();
        if (pullingCount > 0) {
            pullFluids(own, pullingCount);
        }
    }

    // R27: the observed canner input reservoir is never a source for automation.
    // Work through its separate reservoirs, not the historical combined capability.
    private void transferMeasuredCannerFluids(
            com.miophas.singularity_iteration.core.api.fluid.IFluidPorts canner) {
        if (upgradeStats.getFluidEjectorCount() == 0 && upgradeStats.getFluidPullingCount() == 0) return;
        int start = getUpgradeSlotStart();
        int end = Math.min(start + getUpgradeSlotCount(), itemHandler.getSlots());
        IItemAPI api = getItemAPI();
        for (int slot = start; slot < end; slot++) {
            ItemStack upgrade = itemHandler.getStackInSlot(slot);
            if (upgrade.isEmpty()) continue;
            String type = api.getUpgradeType(upgrade);
            boolean eject = "fluid_ejector".equals(type);
            if (!eject && !"fluid_pulling".equals(type)) continue;
            // 50, 200, 800 mB observed; later quantities require forward validation.
            int budget = 50 << (2 * (Math.min(upgrade.getCount(), 5) - 1));
            Direction selected = api.getUpgradeDirection(upgrade);
            for (Direction face : OBSERVED_TRANSFER_FACES) {
                if (selected != null && selected != face) continue;
                BlockPos neighborPos = worldPosition.relative(face);
                IFluidHandler adjacent = getAdjacentFluidHandler(neighborPos, face.getOpposite());
                if (adjacent == null) continue;
                BlockEntity neighbor = level.getBlockEntity(neighborPos);
                if (neighbor instanceof com.miophas.singularity_iteration.core.api.fluid.IFluidPorts other) {
                    adjacent = other.fluidPort(!eject, face.getOpposite());
                }
                IFluidHandler from = eject ? canner.fluidPort(false, null) : adjacent;
                IFluidHandler to = eject ? adjacent : canner.fluidPort(true, null);
                int moved = (eject ? fluidAutomation.outputBuffer() : fluidAutomation.inputBuffer()).move(from, to, budget);
                if (moved > 0) {
                    setChanged();
                    if (neighbor != null) neighbor.setChanged();
                }
            }
        }
    }

    /**
     * 流体弹出升级：将输出槽流体推送到相邻储罐
     * 默认把最后一个tank 当作输出 tank
     * @param own 本机器的流体处理器
     * @param upgradeCount 流体弹出升级数量
     */
    protected void ejectFluids(IFluidHandler own, int upgradeCount) {
        fluidAutomation.ejectFluids(own, upgradeCount);
    }

    /**
     * 流体抽入升级：从相邻储罐抽取流体到输入tank
     * 默认把第一个tank 当作输入 tank
     * @param own 本机器的流体处理器
     * @param upgradeCount 流体抽入升级数量
     */
    protected void pullFluids(IFluidHandler own, int upgradeCount) {
        fluidAutomation.pullFluids(own, upgradeCount);
    }

    /**
     * 子类返回本机器的流体处理器，若不支持流体则返回null
     */
    @Nullable
    protected IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return null;
    }

    /** 对外暴露本机流体处理器，供附属注册流体能力；无流体支持时为 null。 */
    @Nullable
    public final IFluidHandler fluidHandler(@Nullable Direction side) {
        return getFluidHandlerCapability(side);
    }

    /**
     * 更新工作进度
     * 超频升级会提高每 tick 增加的进度值
     */
    protected void updateProgress() {
        if (isWorking) {
            progress = Math.min(maxProgress, progress + getProgressPerTick());
        }
        // 进度不回退，保持当前进度直到完成
    }

    /**
     * 检查机器是否可以工作
     * @return 是否可以工作
     */
    protected abstract boolean canWork();

    /**
     * 检查是否有有效的配方（用于判断进度是否应该重置）
     * 子类应该重写此方法来判断输入物品是否有匹配的配方
     * 默认返回true，表示不重置进度（保守策略）
     * @return 是否有有效配方
     */
    protected boolean hasValidRecipe() {
        return true;
    }

    /**
     * 执行生产工作
     */
    protected abstract void doWork();

    /**
     * 停止工作
     */
    protected void stopWork() {
        isWorking = false;
    }

    /**
     * 检测输入槽物品是否发生变化
     * 只在物品类型/NBT变化时重置工作进度，数量变化不重置
     * 这样自动化添加相同物品时不会打断工作进度
     */
    protected void checkInputChanged() {
        int[] inputSlots = getInputSlots();
        if (inputSlots.length == 0) return;

        // 初始化 lastInputStacks
        if (lastInputStacks == null || lastInputStacks.length != inputSlots.length) {
            lastInputStacks = new ItemStack[inputSlots.length];
            for (int i = 0; i < inputSlots.length; i++) {
                lastInputStacks[i] = itemHandler.getStackInSlot(inputSlots[i]).copy();
            }
            return;
        }

        // 检查每个输入槽是否发生变化
        boolean changed = false;
        for (int i = 0; i < inputSlots.length; i++) {
            ItemStack current = itemHandler.getStackInSlot(inputSlots[i]);
            ItemStack last = lastInputStacks[i];

            // 只检测物品类型/NBT变化，不检测数量变化
            // 这样自动化添加相同物品时不会重置进度
            if (!ItemStack.isSameItemSameComponents(current, last)) {
                changed = true;
                break;
            }
        }

        if (changed) {
            // 输入物品发生变化，重置进度
            progress = 0;
            isWorking = false;
            // 更新缓存
            for (int i = 0; i < inputSlots.length; i++) {
                lastInputStacks[i] = itemHandler.getStackInSlot(inputSlots[i]).copy();
            }
        }
    }

    /**
     * 检查是否应该重置进度（输入槽物品变化时）
     * 默认实现：只在输入槽为空或配方不匹配时重置，断电时保持进度
     * 子类可以覆盖此方法来自定义重置逻辑
     * @return 是否应该重置进度
     */
    protected boolean shouldResetProgress() {
        int[] inputSlots = getInputSlots();
        if (inputSlots.length == 0) return false;
        ItemStack input = itemHandler.getStackInSlot(inputSlots[0]);
        if (input.isEmpty()) {
            return true;
        }
        return !hasValidRecipe();
    }

    /**
     * 完成生产
     */
    protected void finishWork() {
        progress = 0;
        isWorking = false;
        playCompletionCue();
    }

    /** Minimum ticks between two completion cues of one machine (fast, overclocked machines stay quiet). */
    public static final int COMPLETE_SOUND_COOLDOWN = 60;
    private long lastCompleteSound = Long.MIN_VALUE / 2;

    /** Plays the "operation complete" cue, at most once per {@link #COMPLETE_SOUND_COOLDOWN} ticks. */
    protected void playCompletionCue() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;
        long now = level.getGameTime();
        if (now - lastCompleteSound < COMPLETE_SOUND_COOLDOWN) return;
        lastCompleteSound = now;
        level.playSound(null, getBlockPos(),
            com.miophas.singularity_iteration.core.platform.neoforge.CoreSounds.MACHINE_COMPLETE.get(),
            SoundSource.BLOCKS, 0.35F, 0.96F + level.random.nextFloat() * 0.08F);
    }

    /**
     * 每 tick 子类钩子（默认无操作）
     */
    protected void onTick() {
        // hook for subclasses
    }

    /**
     * 强制开始工作（供 API 调用）
     */
    public void forceStartWork() {
        this.isWorking = true;
        this.progress = Math.min(this.progress + 1, this.maxProgress);
    }

    /**
     * 强制停止工作（供 API 调用）
     */
    public void forceStopWork() {
        this.isWorking = false;
        this.progress = 0;
    }

    @Override
    public void setLit(boolean lit) {
        if (getLevel() != null && !getLevel().isClientSide) {
            BlockState state = getBlockState();
            for (var property : state.getProperties()) {
                if (property instanceof net.minecraft.world.level.block.state.properties.BooleanProperty boolProp
                    && "lit".equals(boolProp.getName())) {
                    if (state.getValue(boolProp) != lit) {
                        getLevel().setBlock(getBlockPos(), state.setValue(boolProp, lit), 3);
                    }
                    return;
                }
            }
        }
    }

    /**
     * Lamp state of a processing machine: green while working; blinking amber when it has input
     * (or needs none) but too little energy; red when stopped by redstone, starved of input, or
     * holding input with energy but unable to run (output full / no valid recipe); dark when it is
     * empty and unpowered.
     */
    @Override
    public com.miophas.singularity_iteration.core.api.machine.MachineStatus machineStatus() {
        if (isWorking || runFlag()) return com.miophas.singularity_iteration.core.api.machine.MachineStatus.RUNNING;
        if (!canWorkRedstone()) return com.miophas.singularity_iteration.core.api.machine.MachineStatus.BLOCKED;
        long stored = apiGetStoredEnergy();
        long need = Math.max(1, getEffectiveEnergyPerTick());
        int[] inputs = getInputSlots();
        boolean hasInput = false;
        for (int slot : inputs) {
            if (slot >= 0 && slot < itemHandler.getSlots() && !itemHandler.getStackInSlot(slot).isEmpty()) { hasInput = true; break; }
        }
        if (inputs.length == 0 || hasInput) {
            return stored < need ? com.miophas.singularity_iteration.core.api.machine.MachineStatus.NO_POWER
                : com.miophas.singularity_iteration.core.api.machine.MachineStatus.BLOCKED;
        }
        return stored > 0 ? com.miophas.singularity_iteration.core.api.machine.MachineStatus.BLOCKED
            : com.miophas.singularity_iteration.core.api.machine.MachineStatus.OFF;
    }

    @Override
    public void serverTick() {
        if (getLevel() == null || getLevel().isClientSide) return;
        tick(getLevel(), getBlockPos(), getBlockState(), this);
    }

    /**
     * 获取总处理物品数量
     * 子类应重写此方法以返回实际的总处理量
     */
    public long getTotalProcessed() {
        return 0;
    }

    /**
     * 处理电池槽的放电逻辑（仅从电池提取能量给机器，不给电池充电）
     * 生产机器只能消耗电池能量，不能给电池充电
     *
     * <p>通过 IItemAPI 进行电池操作，Addon 可以注册自定义电池类型。
     */
    protected void handleBatterySlot() {
        int slot = getBatterySlot();
        if (slot >= 0 && !itemHandler.getStackInSlot(slot).isEmpty()) scexFeBridge().discharge(itemHandler, slot);
    }

    /**
     * 获取物品处理器
     *
     * @return 物品处理器
     */
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    /**
     * 获取指定方向的物品处理器（带方向限制）
     * 用于 capability 系统，限制提取行为
     * @param side 方向
     * @return 带方向限制的 IItemHandler
     */
    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        if (side == null) {
            if (unsidedItemHandler == null) unsidedItemHandler = new SidedItemHandler(null);
            return unsidedItemHandler;
        }
        return sidedItemHandlers.computeIfAbsent(side, SidedItemHandler::new);
    }

    /**
     * 获取当前工作进度
     * @return 工作进度
     */
    public int getProgress() {
        return progress;
    }

    /**
     * 获取最大工作进度
     * @return 最大工作进度
     */
    public int getMaxProgress() {
        return maxProgress;
    }

    /**
     * 获取基础最大进度（不受升级影响）
     * @return 基础最大进度
     */
    public int getBaseMaxProgress() {
        return baseMaxProgress;
    }

    /**
     * 检查机器是否正在工作
     * @return 是否正在工作
     */
    public boolean isWorking() {
        return isWorking;
    }

    /**
     * 获取每次工作消耗的能量
     * @return 能量消耗
     */
    public long getEnergyPerTick() {
        return energyPerTick;
    }

    /**
     * 获取工作进度百分比（用于GUI显示）
     * @return 0-100之间的值
     */
    public int getWorkProgress() {
        if (maxProgress <= 0) {
            return 0;
        }
        return (progress * 100) / maxProgress;
    }

    /**
     * 获取实际每tick 能耗（含超频升级影响）
     */
    public long getEffectiveEnergyPerTick() {
        // 走可覆写的 getEnergyPerTick() 而不是字段，便于"能耗随机器状态变化"的机器（如冷凝机按散热片计费）接入超频统计。
        return upgradeStats.getEnergyPerTick(getEnergyPerTick());
    }

    /**
     * 检查是否有足够能量工作
     * @return 是否有足够能量
     */
    protected boolean hasEnoughEnergy() {
        return apiGetStoredEnergy() >= getEffectiveEnergyPerTick();
    }

    /**
     * 消耗能量
     * @return 是否成功消耗
     */
    protected boolean consumeEnergy() {
        long effectiveEnergy = getEffectiveEnergyPerTick();
        long used = getEnergyAPI().useEnergy(effectiveEnergy, false);
        return used >= effectiveEnergy;
    }

    @Override
    public ICableTier getEffectiveCableTier() {
        ICableTier baseTier = (ICableTier) energyStorage.getCableTier();
        return upgradeStats.getEffectiveCableTier(baseTier);
    }

    @Override
    public long getEffectiveCapacity() {
        return baseCapacity + upgradeStats.getEnergyCapacityBonus();
    }

    @Override
    public long getEffectiveMaxReceive() {
        // 变压器升级提升最大接收速率到有效等级的电压等级
        return Math.max(baseMaxReceive, getEffectiveCableTier().getPowerRating());
    }

    /**
     * 获取本tick需求的能量量
 * 对齐 IC2 原版行为：返回内部冲区的全部剩余空间(capacity - storage)
     * IC2 原版机器的容纳= energyPerTick × operationLength，因此需求量自然受限于合理范围
 * 例如：电容纳=100 EU，打粉机容量600 EU，不会出现需求过大的问题
     */
    @Override
    public double getDemandedEnergy() {
        if (energyStorage.scexNetworkControlled()) return 0.0D;
        if (isPowerSource) return 0.0D;
        long spaceAvailable = getEffectiveCapacity() - apiGetStoredEnergy();
        if (spaceAvailable <= 0) return 0.0D;
        return spaceAvailable;
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
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (player == null || this.level == null || this.isRemoved()
                || this.level.getBlockEntity(this.worldPosition) != this) {
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
        // Internal hot paths may reuse arrays; public automation must not mutate
        // the machine's cached layout (or a subclass's shared static layout).
        return getSlotsForDirection(side).clone();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canInsertItem(slot, stack, side);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canExtractItem(slot, side);
    }

    // ==================== SidedItemHandler 内部类 ====================

    /**
     * 带方向限制的 IItemHandler 包装类
     * 用于 capability 系统，限制物品的插入和提取行为
     */
    protected class SidedItemHandler implements IItemHandler {
        protected final Direction side;

        /**
         * {@code side} is the capability context: the face of this machine being
         * accessed (a hopper above inserts through UP). It is passed through
         * unchanged, as {@link #canPlaceItemThroughFace} does; the former
         * {@code getOpposite()} mirrored every side rule, so e.g. the Cropmatron's
         * and Crop Harvester's top-only slots accepted items only from below.
         */
        public SidedItemHandler(@Nullable Direction side) {
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
            // 检查是否可以从指定方向插入
            if (!canInsertItem(slot, stack, side)) {
                return stack;
            }
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            // 检查是否可以从指定方向提取
            if (!canExtractItem(slot, side)) {
                return ItemStack.EMPTY;
            }
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return AbstractProcessingMachineBlockEntity.this.isItemValidForSlot(slot, stack);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("scex_item_output_pending", scexItemOutput.save(registries));
        tag.put("scex_item_input_pending", scexItemInput.save(registries));
        fluidAutomation.save(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("progress", progress);
        tag.putBoolean("is_working", isWorking);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        scexItemOutput.load(registries, tag.getCompound("scex_item_output_pending"));
        scexItemInput.load(registries, tag.getCompound("scex_item_input_pending"));
        fluidAutomation.load(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
            // 自动迁移旧存档的槽位数量（例如冷凝机从3/7 槽扩容到 8 槽）
            if (itemHandler.getSlots() != slotCount) {
                MachineItemHandler resized = createItemHandler(slotLayout);
                int copyCount = Math.min(itemHandler.getSlots(), slotCount);
                for (int i = 0; i < copyCount; i++) {
                    resized.setStackInSlot(i, itemHandler.getStackInSlot(i));
                }
                itemHandler = resized;
            }
        }
        if (tag.contains("progress")) {
            progress = tag.getInt("progress");
        }
        if (tag.contains("is_working")) {
            isWorking = tag.getBoolean("is_working");
        }
 // 重置输入槽存，下次tick会重新初始化
        lastInputStacks = null;

        recalculateUpgradeStats();
        // The energy base class loads before the upgrade inventory. Restore against
        // the final capacity so a reload does not truncate an upgraded machine's EU.
        if (tag.contains("energy", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) apiSetEnergy(tag.getLong("energy"));
        energyStorage.scexLoadFraction(tag.getLong("scex_energy_fraction"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        // 同步物品栏到客户端
        tag.put("inventory", itemHandler.serializeNBT(registries));
        // 同步工作进度
        tag.putInt("progress", progress);
        // 同步工作状态
        tag.putBoolean("is_working", isWorking);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        // 从服务端同步物品栈
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        // 同步工作进度
        if (tag.contains("progress")) {
            progress = tag.getInt("progress");
        }
        // 同步工作状态
        if (tag.contains("is_working")) {
            isWorking = tag.getBoolean("is_working");
        }
 // 重置输入槽存，下次tick会重新初始化
        lastInputStacks = null;
        upgradeStatsDirty = true;
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
        return null; // 基类没有回调，子类可以覆盖
    }
}
