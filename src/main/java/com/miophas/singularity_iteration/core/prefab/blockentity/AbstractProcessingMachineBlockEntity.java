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
 * <p><b>EN:</b> Base class of the electric production machines (EU, no fuel). It gives the work
 * progress, the slot layout and the Container / WorldlyContainer access for hoppers and pipes. It
 * also plays the completion cue ({@link #playCompletionCue()}, at most once per
 * {@link #COMPLETE_SOUND_COOLDOWN} ticks). Recipe lookup and energy operations go through the API layer. Subclasses override the
 * protected methods to change the behavior, so addons can extend a machine safely.
 * <p><b>ZH:</b> 电力生产机器（使用 EU，不用燃料）的基类。提供工作进度、槽位布局、供漏斗与管道使用的 Container / WorldlyContainer 访问，
 * 以及完成提示音（{@link #playCompletionCue()}，每 {@link #COMPLETE_SOUND_COOLDOWN} tick 最多一次）。配方查询与能量操作经由 API 层。
 * 子类覆盖受保护方法来改变行为，因此附属可以安全地扩展机器。
 * <p><b>JA:</b> 電気式の生産機械（EU を使い、燃料は使わない）の基底クラス。作業進捗、スロット配置、ホッパーとパイプ用の
 * Container / WorldlyContainer アクセス、完了音（{@link #playCompletionCue()}、{@link #COMPLETE_SOUND_COOLDOWN} tick に最大 1 回）を提供する。
 * レシピ検索と電力操作は API 層を通す。サブクラスは protected メソッドを上書きして動作を変えるので、アドオンは安全に機械を拡張できる。
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
                @Override public void markUnsaved() {
                    fluidMoves++;   // 0.1.10.8: fluid upgrade pacing (a move or a buffer change)
                    ContainerToTank.markUnsaved(AbstractProcessingMachineBlockEntity.this);
                }
                @Override public void markNeighborUnsaved(BlockPos pos) {
                    com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent
                        .markNeighborUnsaved(level, pos);
                }
            });
    private final com.miophas.singularity_iteration.core.runtime.processing.ItemTransferBuffer scexItemOutput = new com.miophas.singularity_iteration.core.runtime.processing.ItemTransferBuffer(() -> ContainerToTank.markUnsaved(this));
    private final com.miophas.singularity_iteration.core.runtime.processing.ItemTransferBuffer scexItemInput = new com.miophas.singularity_iteration.core.runtime.processing.ItemTransferBuffer(() -> ContainerToTank.markUnsaved(this));
    private boolean automationRunning;
    private long automationRetryAfter;
    /**
     * 0.1.10.7 item automation pacing: item moves of the ejector / pulling upgrades so far, runs
     * in a row that moved nothing, and the game time of the next run. After runs that moved
     * nothing the item upgrades wait 4, 8, 16, then 20 ticks; a change of this machine's
     * inventory runs them at the next tick again. Fluid upgrades: see {@code fluidMoves}.
     */
    private long automationMoves;
    private int automationIdleRuns;
    private long automationNextRun = Long.MIN_VALUE;
    /**
     * 0.1.10.8 fluid upgrade pacing (machines with the built-in fluid transfer): fluid moves and
     * buffer changes so far, runs in a row that moved nothing, the game time of the next run, and
     * the signature of the machine's own tanks after the last run (a change runs them at once).
     */
    private long fluidMoves;
    private int fluidIdleRuns;
    private long fluidNextRun = Long.MIN_VALUE;
    private long fluidSignatureSeen;
    /**
     * Runs of the built-in fluid transfer, counted only when the built-in eject and pull ran for
     * every installed kind (an override of handleFluidUpgrades, ejectFluids or pullFluids is not
     * paced), and whether the last run was one.
     */
    private long fluidBuiltInRuns, fluidBuiltInEjects, fluidBuiltInPulls;
    private boolean fluidPaced = true;
    /**
     * 0.1.10.8 idle sleep (see {@code MachineSleep}): quiet idle ticks in a row, the change stamp
     * (every {@code setChanged}, so every inventory change too), the stored energy at the end of the
     * last full tick, and while asleep: the stamp at sleep and the game time of the safety recheck.
     */
    private int sleepQuietTicks;
    private long sleepStamp, sleepStampSeen, sleepEnergy, sleepUntil;
    private boolean asleep;


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

    // Change detection for input slot items (used to reset progress)
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

    // ==================== API method layer ====================
    // All recipe and energy operations go through these protected methods. Subclasses can override them to change the behavior.

    /**
     * Gets the recipe API instance.
     */
    protected IRecipeAPI getRecipeAPI() {
        return MioIcifAPI.instance().getRecipeAPI();
    }

    /**
     * Gets the item API instance.
     */
    protected IItemAPI getItemAPI() {
        return MioIcifAPI.instance().getItemAPI();
    }

    /**
     * Gets the energy API access interface (through the parent class).
     */
    @Override
    protected IEnergyTileAccess getEnergyAPI() {
        return super.getEnergyAPI();
    }

    /**
     * Finds a recipe through the API.
     * Subclasses can override this method to supply custom recipe lookup logic.
     */
    @SuppressWarnings("deprecation")
    protected <T extends net.minecraft.world.item.crafting.Recipe<?>> java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<T>> apiFindRecipe(
            net.minecraft.world.item.crafting.RecipeType<T> type, ItemStack input, Level level) {
        return getRecipeAPI().findRecipe(type, input, level);
    }

    /**
     * Gets the recipe output through the API.
     */
    protected ItemStack apiGetRecipeOutput(net.minecraft.world.item.crafting.RecipeHolder<?> recipe) {
        return getRecipeAPI().getRecipeOutput(recipe);
    }

    /**
     * Gets the recipe processing time through the API.
     */
    protected int apiGetRecipeProcessTime(net.minecraft.world.item.crafting.RecipeHolder<?> recipe) {
        return getRecipeAPI().getRecipeProcessTime(recipe);
    }

    /**
     * Gets the recipe energy cost through the API.
     */
    protected long apiGetRecipeEnergyCost(net.minecraft.world.item.crafting.RecipeHolder<?> recipe) {
        return getRecipeAPI().getRecipeEnergyCost(recipe);
    }

    /**
     * Checks through the API if the item is a battery.
     * Subclasses can override this method to support custom battery types.
     */
    protected boolean apiIsBattery(ItemStack stack) {
        return getItemAPI().isBattery(stack);
    }

    /**
     * Charges a battery through the API.
     */
    protected long apiChargeBattery(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().chargeBattery(stack, amount, simulate);
    }

    /**
     * Discharges a battery through the API.
     */
    protected long apiDischargeBattery(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().dischargeBattery(stack, amount, simulate);
    }

    /**
     * Gets the battery capacity through the API.
     */
    protected long apiGetBatteryCapacity(ItemStack stack) {
        return getItemAPI().getBatteryCapacity(stack);
    }

    /**
     * Gets the current battery energy through the API.
     */
    protected long apiGetBatteryStored(ItemStack stack) {
        return getItemAPI().getBatteryStored(stack);
    }

    /**
     * Gets the battery charge rate through the API.
     */
    protected long apiGetChargeRate(ItemStack stack) {
        return getItemAPI().getChargeRate(stack);
    }

    /**
     * Charges electric armor through the API.
     */
    protected long apiChargeElectricArmor(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().chargeElectricArmor(stack, amount, simulate);
    }

    /**
     * Discharges electric armor through the API.
     */
    protected long apiDischargeElectricArmor(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().dischargeElectricArmor(stack, amount, simulate);
    }

    /**
     * Checks through the API if the item is electric armor.
     */
    protected boolean apiIsElectricArmor(ItemStack stack) {
        return getItemAPI().isElectricArmor(stack);
    }

    /**
     * Checks through the API if the item is an electric tool.
     */
    protected boolean apiIsElectricTool(ItemStack stack) {
        return getItemAPI().isElectricTool(stack);
    }

    /**
     * Uses energy through the API (internal work).
     * It uses apiUseEnergy, which ignores the maxExtract limit.
     */
    protected boolean apiConsumeEnergy(long amount) {
        long consumed = apiUseEnergy(amount, false);
        return consumed >= amount;
    }

    /**
     * Gets the current stored energy through the API.
     * It accesses energyStorage directly, because a call through getEnergyAPI() causes infinite recursion.
     */
    @Override
    protected long apiGetStoredEnergy() {
        return energyStorage.getAmount();
    }

    /**
     * Receives energy through the API.
     * It accesses energyStorage directly, because a call through getEnergyAPI() causes infinite recursion.
     */
    @Override
    protected long apiReceiveEnergy(long amount, boolean simulate) {
        return energyStorage.receive(amount, simulate);
    }

    /**
     * Simplified constructor, for NeoForge BlockEntityType.Builder.of().
     *
     * <p>This constructor uses the default configuration. Use it for machines such as GenericMachineBlockEntity that
     * are created through a BlockEntityType. The real configuration comes from MachineRegistry in onLoad().
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
        automationNextRun = Long.MIN_VALUE;   // 0.1.10.7: wake the item upgrades
        fluidNextRun = Long.MIN_VALUE;        // 0.1.10.8: and the fluid upgrades
        setChanged();
    }

    /**
     * 0.1.10.7: tells the item automation pacing that an item upgrade moved something (subclasses
     * with their own item transfers call it; the built-in transfers do).
     */
    protected final void noteAutomationMove() {
        automationMoves++;
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
        // Scanner slots, memory slots, tool slots and similar slots hold only 1 item
        if (type == SlotType.SCANNER || type == SlotType.MEMORY || type == SlotType.TOOL) {
            return 1;
        }
        return 64;
    }

    /**
     * Gets the slots that are accessible from the given side.
     * Subclasses must implement this method to define the accessible slots for each side.
     * @param side the side
     * @return the array of accessible slots
     */
    protected abstract int[] getSlotsForDirection(Direction side);

    /**
     * Checks if an item can be inserted into the given slot from the given side.
     * It finds the slot type automatically from the SlotLayout.
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
            case UPGRADE -> false; // Automation cannot insert into upgrade slots (same as original IC2)
            case OUTPUT -> false;
            case FILTER -> isItemValidForSlot(slot, stack);
            default -> isItemValidForSlot(slot, stack);
        };
    }

    protected boolean isUpgradeSlot(int slot) {
        return slotLayout.isType(slot, SlotType.UPGRADE);
    }

    /** This old constant stays for compatibility. For the real policy, see {@link EmergencyChargePolicy}. */
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
     * Update logic for each tick.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, AbstractProcessingMachineBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 0.1.10.8: an idle machine sleeps (see MachineSleep). Its automation upgrades keep running;
        // anything they move changes the inventory and wakes the machine at the next tick.
        if (blockEntity.asleep) {
            if (blockEntity.stillAsleep(level)) {
                com.miophas.singularity_iteration.core.runtime.processing.MachineSleep.noteSkipped();
                blockEntity.handleAutomationUpgrades();
                return;
            }
            blockEntity.asleep = false;
            blockEntity.sleepQuietTicks = 0;
        }
        long stampBefore = blockEntity.sleepStamp;
        boolean production = false;

        // Refresh the upgrade statistics and apply their effects.
        blockEntity.recalculateUpgradeStats();

        // Run the energy transfer logic of the parent class
        AbstractEnergyBlockEntity.tick(level, pos, state, blockEntity);

        // Detect changes of the input slot items. If they changed, reset the progress
        blockEntity.checkInputChanged();

        // Per-tick hook for subclasses (for example the tick callback of GenericMachineBlockEntity)
        blockEntity.onTick();

        // Redstone inversion upgrade: it controls whether the redstone signal stops the machine
        if (!blockEntity.canWorkRedstone()) {
            blockEntity.stopWork();
        } else {
            production = true;
            blockEntity.tickProduction();
        }

        // Discharge the battery slot (only take energy from the battery, do not charge the battery)
        blockEntity.handleBatterySlot();

        // Run automation upgrades (eject, import and similar)
        // Note: automation upgrades must continue to run also when the redstone signal stops the machine
        blockEntity.handleAutomationUpgrades();

        // Work sound: play it periodically while the machine works
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
        blockEntity.noteQuiet(level, production, stampBefore);
    }

    /**
     * 0.1.10.8: true when this machine may sleep while idle (see {@code MachineSleep}). Return true
     * only when {@code canWork()} depends on nothing but the inventory, the saved state (every change
     * of it calls {@code setChanged()}) and the stored energy: those wake the machine. Otherwise a
     * change can be noticed up to {@code MachineSleep.RECHECK_TICKS} ticks late. Default: false.
     */
    protected boolean idleSleepAllowed() {
        return false;
    }

    /** 0.1.10.8: true while the machine sleeps (for tests and diagnostics). */
    public final boolean isIdleAsleep() {
        return asleep;
    }

    /** 0.1.10.8: wakes the machine for its next tick (for code that changes it without {@code setChanged()}). */
    public final void wakeFromIdleSleep() {
        asleep = false;
        sleepQuietTicks = 0;
    }

    /** The sleep holds while nothing changed: stamp, work, progress, stored energy, and the recheck time. */
    private boolean stillAsleep(Level level) {
        return com.miophas.singularity_iteration.core.runtime.processing.MachineSleep.enabled()
            && sleepStamp == sleepStampSeen && !isWorking && progress == 0
            && level.getGameTime() < sleepUntil && apiGetStoredEnergy() == sleepEnergy && !isRemoved();
    }

    /** After a full tick: count a quiet idle tick, and sleep after {@code MachineSleep.QUIET_TICKS} of them. */
    private void noteQuiet(Level level, boolean production, long stampBefore) {
        // most machines do not sleep: nothing more to do for them
        if (!idleSleepAllowed() || !com.miophas.singularity_iteration.core.runtime.processing.MachineSleep.enabled()) {
            sleepQuietTicks = 0;
            return;
        }
        long energy = apiGetStoredEnergy();
        int battery = getBatterySlot();
        boolean quiet = production && !isWorking && progress == 0 && sleepStamp == stampBefore && energy == sleepEnergy
            && (battery < 0 || battery >= itemHandler.getSlots() || itemHandler.getStackInSlot(battery).isEmpty());
        sleepEnergy = energy;
        if (!quiet) { sleepQuietTicks = 0; return; }
        if (++sleepQuietTicks < com.miophas.singularity_iteration.core.runtime.processing.MachineSleep.QUIET_TICKS) return;
        asleep = true;
        sleepStampSeen = sleepStamp;
        sleepUntil = level.getGameTime() + com.miophas.singularity_iteration.core.runtime.processing.MachineSleep.RECHECK_TICKS;
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
     * Updates the upgrade statistics.
     * Overclocker upgrade: more progress per tick and more EU use. The voltage tier does not change.
     */
    protected void recalculateUpgradeStats() {
        if (!upgradeStatsDirty) return;
        int upgradeStart = slotLayout.getStart(SlotType.UPGRADE);
        int upgradeCount = slotLayout.getCount(SlotType.UPGRADE);
        this.upgradeStats = throttled(MachineUpgradeStats.fromInventory(itemHandler, upgradeStart, upgradeCount));
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

    // ------------------------------------------------------------------ 0.1.10.6: overclock throttle
    /** Share of the installed overclockers that work, in percent (the overclock slider of the GUI; saved). */
    private int overclockThrottle = 100;
    /** Overclockers in the upgrade slots, before the throttle. */
    private int installedOverclockers;
    /** Game time of the last finished operation (for the progress display of fast machines). */
    private long lastFinishTick = Long.MIN_VALUE / 2;

    /** Active overclockers for {@code installed} ones at {@code percent} throttle: round(installed x percent / 100). */
    public static int activeOverclockers(int installed, int percent) {
        return (int) Math.round(Math.max(0, installed) * Math.max(0, Math.min(100, percent)) / 100.0);
    }

    /** The statistics with the throttle applied; records the installed overclocker count. */
    protected MachineUpgradeStats throttled(MachineUpgradeStats raw) {
        installedOverclockers = raw.getOverclockerCount();
        return raw.withActiveOverclockers(activeOverclockers(installedOverclockers, overclockThrottle));
    }

    public int getInstalledOverclockers() {
        recalculateUpgradeStats();
        return installedOverclockers;
    }

    public int getOverclockThrottle() { return overclockThrottle; }

    /** Overclockers that work now (installed x throttle). */
    public int getActiveOverclockers() { return activeOverclockers(getInstalledOverclockers(), overclockThrottle); }

    /** Sets the throttle (0..100 %); the speed and the energy use follow from the next tick. */
    public void setOverclockThrottle(int percent) {
        int value = Math.max(0, Math.min(100, percent));
        if (value == overclockThrottle) return;
        overclockThrottle = value;
        upgradeStatsDirty = true;
        recalculateUpgradeStats();
        setChanged();
    }

    /** Progress per tick for the GUI (with the throttle). */
    public int getDisplayProgressPerTick() {
        recalculateUpgradeStats();
        return getProgressPerTick();
    }

    /** Ticks of one operation at the current speed. */
    public int getTicksPerOperation() {
        int perTick = Math.max(1, getDisplayProgressPerTick());
        return Math.max(1, (maxProgress + perTick - 1) / perTick);
    }

    /**
     * Progress for the GUI. An overclocked machine can finish an operation in the same tick in
     * which it starts the next one, so the progress the client receives is always 0 and the bar
     * looked still (0.1.10.5 report: the recycler). Right after an operation the bar shows full.
     */
    public int getDisplayProgress() {
        if (progress == 0 && level != null && level.getGameTime() - lastFinishTick <= 1) return maxProgress;
        return progress;
    }

    protected void updateProcessingParameters() {
        // The overclocker upgrade now adds more progress per tick. It does not decrease the maximum progress.
        // Thus this code only resets a maxProgress that an old save possibly changed back to the base value.
        if (this.maxProgress != this.baseMaxProgress) {
            this.maxProgress = this.baseMaxProgress;
            if (this.progress > this.maxProgress) {
                this.progress = this.maxProgress;
            }
        }
    }

    /**
     * Gets the work progress added per tick (the overclocker upgrade changes it).
     * The overclocker upgrade makes the machine do more progress in 1 tick and use more EU. The voltage tier does not change.
     */
    protected int getProgressPerTick() {
        return Math.max(1, (int) Math.ceil(1.0 / upgradeStats.getProcessTimeMultiplier()));
    }

    /**
     * Gets the current upgrade statistics.
     */
    @Override
    public IMachineUpgradeStats getUpgradeStats() {
        return upgradeStats;
    }

    /**
     * Gets the start index of the upgrade slots.
     */
    public int getUpgradeSlotStart() {
        return slotLayout.getStart(SlotType.UPGRADE);
    }

    /**
     * Gets the number of upgrade slots.
     */
    public int getUpgradeSlotCount() {
        return slotLayout.getCount(SlotType.UPGRADE);
    }

    /**
     * Checks if the machine can work under redstone control.
     * Default: the machine works when there is no redstone signal, and stops when there is a signal.
     * Redstone inversion upgrade: the machine works when there is a signal, and stops when there is no signal.
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
     * Gets the array of input slot indices. Subclasses can override it.
     */
    protected int[] getInputSlots() {
        return inputSlotIndices;
    }

    /**
     * Gets the array of output slot indices. Subclasses can override it.
     */
    protected int[] getOutputSlots() {
        return outputSlotIndices;
    }

    /**
     * Runs the automation upgrades.
     * Subclasses can override it to add more specific fluid or item interaction.
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
        // 0.1.10.7: item upgrades are paced (see automationNextRun); 0.1.10.8: fluid upgrades too (runFluidUpgrades)
        long now = level.getGameTime();
        boolean itemsDue = !com.miophas.singularity_iteration.core.runtime.processing.AutomationPacing.enabled()
            || now >= automationNextRun;
        long movesBefore = automationMoves;
        boolean ranItems = false;
        // tick() refreshes the immutable snapshot after an upgrade-slot mutation.
        if (hasMeasuredItemAutomation()) {
            if (itemsDue) { transferMeasuredItems(); ranItems = true; }
            runFluidUpgrades(now);
            if (ranItems) paceItemAutomation(now, movesBefore);
            return;
        }
        if (itemsDue && upgradeStats.getEjectorCount() > 0) {
            ejectItems(upgradeStats.getEjectorCount());
            ranItems = true;
        }
        if (itemsDue && upgradeStats.getPullingCount() > 0) {
            pullItems(upgradeStats.getPullingCount());
            ranItems = true;
        }
        if (ranItems) paceItemAutomation(now, movesBefore);
        runFluidUpgrades(now);
        } catch (RuntimeException externalFailure) {
            // Buffers retain durable uncertainty; an untrusted handler must not crash the tile tick.
            automationRetryAfter = level.getGameTime() + 20;
        } finally { automationRunning = false; }
    }

    /**
     * 0.1.10.8: the fluid upgrades with the waits of the item upgrades, for machines that use the
     * built-in fluid transfer: a run that moved fluid runs again at the next tick; after a run that
     * moved nothing they wait 4, 8, 16, then 20 ticks, unless the machine's own tanks or inventory
     * change (then at the next tick). Machines that override the fluid transfer, and every machine
     * when the pacing is off, run it every tick as before.
     */
    private void runFluidUpgrades(long now) {
        if (!com.miophas.singularity_iteration.core.runtime.processing.AutomationPacing.enabled()) {
            handleFluidUpgrades();
            return;
        }
        if (upgradeStats.getFluidEjectorCount() == 0 && upgradeStats.getFluidPullingCount() == 0) {
            handleFluidUpgrades();
            return;
        }
        long signature = fluidSignature();
        if (fluidPaced && now < fluidNextRun && signature == fluidSignatureSeen) return;
        long movesBefore = fluidMoves, builtInBefore = fluidBuiltInRuns, stampBefore = sleepStamp;
        handleFluidUpgrades();
        // Only a run of the built-in transfer is paced: an override runs every tick, as before.
        fluidPaced = fluidBuiltInRuns != builtInBefore;
        long after = fluidSignature();
        if (!fluidPaced || fluidMoves != movesBefore || after != signature || sleepStamp != stampBefore
                || fluidAutomation.outputBuffer().hasPending() || fluidAutomation.inputBuffer().hasPending()) {
            fluidIdleRuns = 0;
            fluidNextRun = Long.MIN_VALUE;
        } else {
            fluidIdleRuns = Math.min(fluidIdleRuns + 1, 8);
            fluidNextRun = now + com.miophas.singularity_iteration.core.runtime.processing.AutomationPacing.idleDelay(fluidIdleRuns);
        }
        fluidSignatureSeen = after;
    }

    /** Fluid and amount of every own tank (both ports of a machine with separate ports), as one number. */
    private long fluidSignature() {
        if (this instanceof com.miophas.singularity_iteration.core.api.fluid.IFluidPorts ports) {
            return tankSignature(ports.fluidPort(true, null)) * 31 + tankSignature(ports.fluidPort(false, null));
        }
        return tankSignature(getFluidHandlerCapability(null));
    }

    private static long tankSignature(@Nullable IFluidHandler handler) {
        if (handler == null) return 0;
        long hash = 1;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            var fluid = handler.getFluidInTank(tank);
            hash = hash * 31 + fluid.getAmount();
            hash = hash * 31 + (fluid.isEmpty() ? 0 : System.identityHashCode(fluid.getFluid()));
        }
        return hash;
    }

    /** 0.1.10.7: after an item upgrade run: busy again at once, or wait longer after a run that moved nothing. */
    private void paceItemAutomation(long now, long movesBefore) {
        if (automationMoves != movesBefore || scexItemOutput.hasPending() || scexItemInput.hasPending()) {
            automationIdleRuns = 0;
            automationNextRun = Long.MIN_VALUE;
        } else {
            automationIdleRuns = Math.min(automationIdleRuns + 1, 8);
            automationNextRun = now + com.miophas.singularity_iteration.core.runtime.processing.AutomationPacing.idleDelay(automationIdleRuns);
        }
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
        // 0.1.10.6: storage neighbours first, processing machines last (observed order within a kind)
        Direction[] faces = byTargetPriority(java.util.Arrays.asList(OBSERVED_TRANSFER_FACES));
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
            for (Direction face : faces) {
                if (selected != null && selected != face) continue;
                if (pingPong(face, eject)) continue;
                IItemHandler adjacent = getAdjacentItemHandler(worldPosition.relative(face), face.getOpposite());
                if (adjacent == null) continue;
                if (eject) {
                    for (int output : getOutputSlots()) {
                        if (scexItemOutput.move(itemHandler, output, adjacent, -1, budget) > 0) {
                            markItemTransferSaved(worldPosition.relative(face));
                            noteTransfer(face, true);
                        }
                    }
                } else {
                    // a pass that found nothing to pull waits a second before scanning the neighbour again
                    if (level != null && level.getGameTime() < pullIdleUntil) continue;
                    int remaining = budget;
                    for (int sourceSlot = 0; sourceSlot < adjacent.getSlots() && remaining > 0; sourceSlot++) {
                        if (adjacent.getStackInSlot(sourceSlot).isEmpty()) continue;
                        for (int input : getInputSlots()) {
                            int moved = scexItemInput.move(adjacent, sourceSlot, itemHandler, input, remaining);
                            if (moved <= 0) continue;
                            remaining -= moved;
                            markItemTransferSaved(worldPosition.relative(face));
                            noteTransfer(face, false);
                            break;
                        }
                    }
                    if (remaining == budget && level != null) pullIdleUntil = level.getGameTime() + PULL_IDLE_TICKS;
                }
            }
        }
    }

    // ------------------------------------------------------------------ 0.1.10.6: item routing
    /*
     * Ping-pong: a furnace that pulls dust from a macerator and ejects its ingots back into it fed
     * the macerator, which made dust again, forever (the energy of both machines went nowhere).
     * Now (1) a directionless ejector tries storage neighbours first (chests, barrels, crates, the
     * block tag mio_icif:automation/storage), other blocks next and processing machines last; and
     * (2) items never go back and forth with a processing machine (an SI machine, a vanilla furnace
     * or brewing stand, the tag mio_icif:automation/processor): no eject into a machine that this
     * one pulled from in the last PING_PONG_TICKS, no pull from a machine that this one ejected
     * into, and no eject into a machine whose own ejector reaches this one. Storage neighbours are
     * never restricted, so "pull ore from a chest, put ingots into the same chest" keeps working.
     */

    /** Ticks after a transfer with a processing machine during which items do not go the other way. */
    public static final int PING_PONG_TICKS = 200;
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> AUTOMATION_STORAGE =
        net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "automation/storage"));
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> AUTOMATION_PROCESSOR =
        net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "automation/processor"));
    private static final long NEVER = Long.MIN_VALUE / 2;
    private final long[] lastPullFrom = {NEVER, NEVER, NEVER, NEVER, NEVER, NEVER};
    private final long[] lastEjectTo = {NEVER, NEVER, NEVER, NEVER, NEVER, NEVER};

    /** Kind of the neighbour on {@code face}: 0 storage, 1 other, 2 processing machine. */
    /** 0.1.10.7: neighbour kinds per face (-1: unknown), when they were worked out, and the caches that invalidate them. */
    private final int[] cachedKinds = {-1, -1, -1, -1, -1, -1};
    private final long[] kindsCheckedAt = new long[6];
    @SuppressWarnings({"unchecked", "rawtypes"})
    private final net.neoforged.neoforge.capabilities.BlockCapabilityCache<IItemHandler, Direction>[] kindWatches =
        new net.neoforged.neoforge.capabilities.BlockCapabilityCache[6];
    /** Ticks a neighbour kind is kept at most (block tags can change on a reload). */
    private static final int KIND_RECHECK = 200;

    /**
     * Kind of the neighbour on {@code face} for the routing order: 0 storage, 1 other, 2 processing
     * machine. 0.1.10.7: kept per face until the block there changes (capability invalidation of
     * that position), at most {@link #KIND_RECHECK} ticks.
     */
    protected int neighbourKind(Direction face) {
        int i = face.ordinal();
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) return computeNeighbourKind(face);
        long now = server.getGameTime();
        if (cachedKinds[i] >= 0 && now - kindsCheckedAt[i] < KIND_RECHECK) return cachedKinds[i];
        int kind = computeNeighbourKind(face);
        var watch = kindWatches[i];
        if (watch == null || watch.level() != server) {
            watch = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, server,
                worldPosition.relative(face), face.getOpposite(), () -> !isRemoved(), () -> cachedKinds[i] = -1);
            kindWatches[i] = watch;
        }
        watch.getCapability();   // arms the invalidation listener
        cachedKinds[i] = kind;
        kindsCheckedAt[i] = now;
        return kind;
    }

    private int computeNeighbourKind(Direction face) {
        if (level == null) return 1;
        BlockPos pos = worldPosition.relative(face);
        if (!level.isLoaded(pos)) return 1;
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return 1;
        BlockState state = be.getBlockState();
        if (state.is(AUTOMATION_PROCESSOR) || be instanceof AbstractProcessingMachineBlockEntity
                || be instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
                || be instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity) return 2;
        if (state.is(AUTOMATION_STORAGE) || be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity
                || be instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity
                || be instanceof net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity) return 0;
        return 1;
    }

    /** {@code faces} sorted by target priority: storage, other, processing machine (stable). */
    protected Direction[] byTargetPriority(Iterable<Direction> faces) {
        java.util.List<Direction> list = new java.util.ArrayList<>(6);
        for (Direction face : faces) list.add(face);
        int[] kind = new int[6];
        for (Direction face : list) kind[face.ordinal()] = neighbourKind(face);
        list.sort(java.util.Comparator.comparingInt(face -> kind[face.ordinal()]));
        return list.toArray(new Direction[0]);
    }

    /** True when an eject ({@code eject}) or a pull on {@code face} would send items back and forth with a machine. */
    protected boolean pingPong(Direction face, boolean eject) {
        if (level == null || neighbourKind(face) != 2) return false;
        long now = level.getGameTime();
        if (eject) {
            if (now - lastPullFrom[face.ordinal()] < PING_PONG_TICKS) return true;
            // the neighbour's own ejector reaches this machine: what goes in comes straight back
            return level.getBlockEntity(worldPosition.relative(face)) instanceof AbstractProcessingMachineBlockEntity other
                && other.ejectsToward(face.getOpposite());
        }
        return now - lastEjectTo[face.ordinal()] < PING_PONG_TICKS;
    }

    /** True when an item ejector of this machine can put items out through {@code face}. */
    public boolean ejectsToward(Direction face) {
        if (upgradeStats.getEjectorCount() <= 0) return false;
        List<Direction> dirs = upgradeStats.getEjectorDirections();
        return dirs.isEmpty() || dirs.contains(face);
    }

    private void noteTransfer(Direction face, boolean eject) {
        automationMoves++;   // 0.1.10.7: item automation pacing
        if (level == null) return;
        (eject ? lastEjectTo : lastPullFrom)[face.ordinal()] = level.getGameTime();
    }

    private static final int PULL_IDLE_TICKS = 20;
    /** Game time before which a pulling upgrade does not rescan a neighbour that had nothing to give. */
    private long pullIdleUntil;

    private void markItemTransferSaved(BlockPos neighborPos) {
        // Also mark the neighbor: third-party inventory handlers may not notify their owner.
        // Keep both sides of the completed transfer eligible for saving.
        setChanged();
        BlockEntity neighbor = level.getBlockEntity(neighborPos);
        if (neighbor != null) neighbor.setChanged();
    }

    /**
     * Ejector upgrade: pushes output slot items into adjacent containers.
     * @param upgradeCount the number of ejector upgrades
     */
    protected void ejectItems(int upgradeCount) {
        int maxPerTick = Math.max(1, upgradeCount);
        List<Direction> dirs = upgradeStats.getEjectorDirections();
        // 0.1.10.6: storage first, processing machines last; no ping-pong with a machine
        // 0.1.10.7: the order is worked out only when an output slot has something to send
        Direction[] targetDirs = null;
        for (int outputSlot : getOutputSlots()) {
            ItemStack stack = itemHandler.getStackInSlot(outputSlot);
            if (stack.isEmpty() && !scexItemOutput.hasPending()) {
                continue;
            }
            if (targetDirs == null) targetDirs = byTargetPriority(!dirs.isEmpty() ? dirs : ALL_DIRECTIONS);
            for (Direction dir : targetDirs) {
                if (pingPong(dir, true)) continue;
                IItemHandler target = getAdjacentItemHandler(worldPosition.relative(dir), dir.getOpposite());
                if (target == null) {
                    continue;
                }
                int moved = scexItemOutput.move(itemHandler, outputSlot, target, -1, maxPerTick);
                if (moved > 0) {
                    markItemTransferSaved(worldPosition.relative(dir));
                    noteTransfer(dir, true);
                    stack = itemHandler.getStackInSlot(outputSlot);
                    if (stack.isEmpty() && !scexItemOutput.hasPending()) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Import upgrade: pulls items from adjacent containers into the input slots.
     * @param upgradeCount the number of import upgrades
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
                if (pingPong(dir, false)) continue;   // 0.1.10.6
                IItemHandler source = getAdjacentItemHandler(worldPosition.relative(dir), dir.getOpposite());
                if (source == null) {
                    continue;
                }
                boolean pulled = false;
                for (int i = 0; i < source.getSlots(); i++) {
                    if (scexItemInput.move(source, i, itemHandler, inputSlot, maxPerTick) > 0) {
                        markItemTransferSaved(worldPosition.relative(dir));
                        noteTransfer(dir, false);
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
        dropMachineSpecificPending(drop);
    }

    /** Machines buffering items outside their inventory (mined loot, ...) spill them here on removal. */
    protected void dropMachineSpecificPending(java.util.function.Consumer<ItemStack> drop) {
    }

    /**
     * Gets the item handler at the adjacent position.
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
     * Gets the fluid handler at the adjacent position.
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
     * Runs the fluid automation upgrades.
     * Subclasses with more than one fluid tank or special output sides can override it.
     */
    protected void handleFluidUpgrades() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (this instanceof com.miophas.singularity_iteration.core.api.fluid.IFluidPorts ports) {
            transferMeasuredCannerFluids(ports);
            fluidBuiltInRuns++;
            return;
        }
        IFluidHandler own = getFluidHandlerCapability(null);
        if (own == null || own.getTanks() == 0) {
            fluidBuiltInRuns++;
            return;
        }

        long ejectsBefore = fluidBuiltInEjects, pullsBefore = fluidBuiltInPulls;
        int ejectorCount = upgradeStats.getFluidEjectorCount();
        if (ejectorCount > 0) {
            ejectFluids(own, ejectorCount);
        }
        int pullingCount = upgradeStats.getFluidPullingCount();
        if (pullingCount > 0) {
            pullFluids(own, pullingCount);
        }
        if ((ejectorCount <= 0 || fluidBuiltInEjects != ejectsBefore) && (pullingCount <= 0 || fluidBuiltInPulls != pullsBefore))
            fluidBuiltInRuns++;   // 0.1.10.8: the built-in transfer ran (see runFluidUpgrades)
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
                    // 0.1.10.11: ejecting goes into the neighbour's input port (the left tank of a
                    // canner), pulling takes from its output port. Before, the ports were swapped: a
                    // canner ejecting into the next canner filled that canner's right (output) tank.
                    adjacent = other.fluidPort(eject, face.getOpposite());
                }
                IFluidHandler from = eject ? canner.fluidPort(false, null) : adjacent;
                IFluidHandler to = eject ? adjacent : canner.fluidPort(true, null);
                int moved = (eject ? fluidAutomation.outputBuffer() : fluidAutomation.inputBuffer()).move(from, to, budget);
                if (moved > 0) {
                    fluidMoves++;   // 0.1.10.8: fluid upgrade pacing
                    setChanged();
                    if (neighbor != null) neighbor.setChanged();
                }
            }
        }
    }

    /**
     * Fluid ejector upgrade: pushes fluid from the output tank into adjacent tanks.
     * By default, the last tank is the output tank.
     * @param own the fluid handler of this machine
     * @param upgradeCount the number of fluid ejector upgrades
     */
    protected void ejectFluids(IFluidHandler own, int upgradeCount) {
        fluidBuiltInEjects++;
        fluidAutomation.ejectFluids(own, upgradeCount);
    }

    /**
     * Fluid import upgrade: pulls fluid from adjacent tanks into the input tank.
     * By default, the first tank is the input tank.
     * @param own the fluid handler of this machine
     * @param upgradeCount the number of fluid import upgrades
     */
    protected void pullFluids(IFluidHandler own, int upgradeCount) {
        fluidBuiltInPulls++;
        fluidAutomation.pullFluids(own, upgradeCount);
    }

    /**
     * Subclasses return the fluid handler of this machine, or null if the machine does not support fluids.
     */
    @Nullable
    protected IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return null;
    }

    /** Gives the fluid handler of this machine to other code, so that addons can register a fluid capability. It is null if the machine has no fluid support. */
    @Nullable
    public final IFluidHandler fluidHandler(@Nullable Direction side) {
        return getFluidHandlerCapability(side);
    }

    /**
     * Updates the work progress.
     * The overclocker upgrade increases the progress added per tick.
     */
    protected void updateProgress() {
        if (isWorking) {
            progress = Math.min(maxProgress, progress + getProgressPerTick());
        }
        // Progress does not go back. It stays at the current value until the work completes
    }

    /**
     * Checks if the machine can work.
     * @return true if the machine can work
     */
    protected abstract boolean canWork();

    /**
     * Checks if there is a valid recipe (used to decide if the progress must be reset).
     * Subclasses must override this method to check if the input items match a recipe.
     * By default it returns true, which means that the progress is not reset (conservative policy).
     * @return true if there is a valid recipe
     */
    protected boolean hasValidRecipe() {
        return true;
    }

    /**
     * Does the production work.
     */
    protected abstract void doWork();

    /**
     * Stops the work.
     */
    protected void stopWork() {
        isWorking = false;
    }

    /**
     * Detects if the input slot items changed.
     * It resets the work progress only when the item type or NBT changes. A change of count does not reset it.
     * Thus, when automation adds more of the same item, the work progress continues.
     */
    protected void checkInputChanged() {
        int[] inputSlots = getInputSlots();
        if (inputSlots.length == 0) return;

        // Initialize lastInputStacks
        if (lastInputStacks == null || lastInputStacks.length != inputSlots.length) {
            lastInputStacks = new ItemStack[inputSlots.length];
            for (int i = 0; i < inputSlots.length; i++) {
                lastInputStacks[i] = itemHandler.getStackInSlot(inputSlots[i]).copy();
            }
            return;
        }

        // Check each input slot for a change
        boolean changed = false;
        for (int i = 0; i < inputSlots.length; i++) {
            ItemStack current = itemHandler.getStackInSlot(inputSlots[i]);
            ItemStack last = lastInputStacks[i];

            // Detect only item type or NBT changes, not count changes
            // Thus, when automation adds more of the same item, the progress is not reset
            if (!ItemStack.isSameItemSameComponents(current, last)) {
                changed = true;
                break;
            }
        }

        if (changed) {
            // The input items changed. Reset the progress
            progress = 0;
            isWorking = false;
            // Update the cache
            for (int i = 0; i < inputSlots.length; i++) {
                lastInputStacks[i] = itemHandler.getStackInSlot(inputSlots[i]).copy();
            }
        }
    }

    /**
     * Checks if the progress must be reset (when the input slot items change).
     * Default implementation: reset only when the input slot is empty or the recipe does not match. Keep the progress when power is lost.
     * Subclasses can override this method to use custom reset logic.
     * @return true if the progress must be reset
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
     * Completes the production.
     */
    protected void finishWork() {
        progress = 0;
        isWorking = false;
        if (level != null) lastFinishTick = level.getGameTime();
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
     * Per-tick hook for subclasses (does nothing by default).
     */
    protected void onTick() {
        // hook for subclasses
    }

    /**
     * Starts the work by force (for API calls).
     */
    public void forceStartWork() {
        this.isWorking = true;
        this.progress = Math.min(this.progress + 1, this.maxProgress);
    }

    /**
     * Stops the work by force (for API calls).
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
     * Gets the total number of processed items.
     * Subclasses must override this method to return the real total.
     */
    public long getTotalProcessed() {
        return 0;
    }

    /**
     * Discharges the battery slot (only takes energy from the battery into the machine, does not charge the battery).
     * A production machine can only use battery energy. It cannot charge the battery.
     *
     * <p>Battery operations go through IItemAPI, so addons can register custom battery types.
     */
    protected void handleBatterySlot() {
        int slot = getBatterySlot();
        if (slot >= 0 && !itemHandler.getStackInSlot(slot).isEmpty()) scexFeBridge().discharge(itemHandler, slot);
    }

    /**
     * Gets the item handler.
     *
     * @return the item handler
     */
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    /**
     * Gets the item handler for the given side (with side restrictions).
     * The capability system uses it to limit extraction.
     * @param side the side
     * @return the IItemHandler with side restrictions
     */
    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        if (side == null) {
            if (unsidedItemHandler == null) unsidedItemHandler = new SidedItemHandler(null);
            return unsidedItemHandler;
        }
        return sidedItemHandlers.computeIfAbsent(side, SidedItemHandler::new);
    }

    /**
     * Gets the current work progress.
     * @return the work progress
     */
    public int getProgress() {
        return progress;
    }

    /**
     * Gets the maximum work progress.
     * @return the maximum work progress
     */
    public int getMaxProgress() {
        return maxProgress;
    }

    /**
     * Gets the base maximum progress (upgrades do not change it).
     * @return the base maximum progress
     */
    public int getBaseMaxProgress() {
        return baseMaxProgress;
    }

    /**
     * Checks if the machine is working.
     * @return true if the machine is working
     */
    public boolean isWorking() {
        return isWorking;
    }

    /**
     * Gets the energy that each work operation uses.
     * @return the energy cost
     */
    public long getEnergyPerTick() {
        return energyPerTick;
    }

    /**
     * Gets the work progress as a percentage (for GUI display).
     * @return a value from 0 to 100
     */
    public int getWorkProgress() {
        if (maxProgress <= 0) {
            return 0;
        }
        return (progress * 100) / maxProgress;
    }

    /**
     * Gets the real energy use per tick (with the overclocker upgrade effect), in EU/tick.
     */
    public long getEffectiveEnergyPerTick() {
        // Use the overridable getEnergyPerTick(), not the field, so that machines whose energy use changes with their state (for example the condenser, which charges per heat vent) are included in the overclocker statistics.
        return upgradeStats.getEnergyPerTick(getEnergyPerTick());
    }

    /**
     * Checks if there is sufficient energy to work.
     * @return true if there is sufficient energy
     */
    protected boolean hasEnoughEnergy() {
        return apiGetStoredEnergy() >= getEffectiveEnergyPerTick();
    }

    /**
     * Uses energy.
     * @return true if the energy was used
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
        // The transformer upgrade increases the maximum receive rate to the voltage rating of the effective tier
        return Math.max(baseMaxReceive, getEffectiveCableTier().getPowerRating());
    }

    /**
     * Gets the energy that this machine demands in this tick.
     * Same as original IC2 behavior: it returns all the free space of the internal buffer (capacity - stored).
     * In original IC2, the machine capacity = energyPerTick x operationLength. Thus the demand stays in a sensible range.
     * For example: a capacity of 100 EU, or a macerator capacity of 600 EU, does not cause a too large demand.
     */
    @Override
    public double getDemandedEnergy() {
        if (energyStorage.scexNetworkControlled()) return 0.0D;
        if (isPowerSource) return 0.0D;
        long spaceAvailable = getEffectiveCapacity() - apiGetStoredEnergy();
        if (spaceAvailable <= 0) return 0.0D;
        return spaceAvailable;
    }

    // ==================== Container interface implementation ====================

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
        sleepStamp++;   // 0.1.10.8: wakes a sleeping machine (see MachineSleep)
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

    // ==================== WorldlyContainer interface implementation ====================

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

    // ==================== SidedItemHandler inner class ====================

    /**
     * IItemHandler wrapper with side restrictions.
     * The capability system uses it to limit item insertion and extraction.
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
            // Check if items can be inserted from the given side
            if (!canInsertItem(slot, stack, side)) {
                return stack;
            }
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            // Check if items can be extracted from the given side
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
        if (overclockThrottle != 100) tag.putInt("scex_oc_throttle", overclockThrottle);   // 0.1.10.6
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        overclockThrottle = tag.contains("scex_oc_throttle") ? Math.max(0, Math.min(100, tag.getInt("scex_oc_throttle"))) : 100;
        upgradeStatsDirty = true;
        scexItemOutput.load(registries, tag.getCompound("scex_item_output_pending"));
        scexItemInput.load(registries, tag.getCompound("scex_item_input_pending"));
        fluidAutomation.load(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
            // Move the slot count of old saves automatically (for example the condenser grew from 3/7 slots to 8 slots)
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
        // Reset the input slot cache. The next tick initializes it again
        lastInputStacks = null;

        recalculateUpgradeStats();
        // The energy base class loads before the upgrade inventory. Restore against
        // the final capacity so a reload does not truncate an upgraded machine's EU.
        if (tag.contains("energy", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) apiSetEnergy(tag.getLong("energy"));
        energyStorage.scexLoadFraction(tag.getLong("scex_energy_fraction"));
    }

    /**
     * 0.1.10.17 (E-2): whether {@link #getUpdateTag} includes the inventory. Default false: no SI client
     * code reads the items of a machine outside its menu, and the menu gets them from the vanilla slot
     * sync. Return true when a client renderer (or other client code) reads the items of this block
     * entity. The loaders apply an inventory only when the tag has one, so a tag without it never clears
     * the client's items.
     */
    protected boolean syncInventoryToClient() {
        return false;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        // 0.1.10.17 (E-2): the inventory only when a subclass asks for it. A screen gets the items from
        // its menu (vanilla sends all slots when the menu opens); the update tag goes to every player
        // that tracks the chunk, with every chunk load.
        if (syncInventoryToClient()) tag.put("inventory", itemHandler.serializeNBT(registries));
        // Synchronize the work progress
        tag.putInt("progress", progress);
        // Synchronize the work state
        tag.putBoolean("is_working", isWorking);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        // Synchronize the item stacks from the server
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        // Synchronize the work progress
        if (tag.contains("progress")) {
            progress = tag.getInt("progress");
        }
        // Synchronize the work state
        if (tag.contains("is_working")) {
            isWorking = tag.getBoolean("is_working");
        }
        // Reset the input slot cache. The next tick initializes it again
        lastInputStacks = null;
        upgradeStatsDirty = true;
    }

    // ==================== IProducerBlock interface implementation ====================

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
        return null; // The base class has no callback. Subclasses can override it
    }
}
