package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.runtime.heat.HeatStorage;
import com.miophas.singularity_iteration.core.api.heat.IHeatStorage;
import com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 感应炉方块实体类
 * 使用感应生产热量来同时处理两个输入（可以同时冶炼两个物品）
 *
 * 特点：
 * - 持续耗电产生热量（不输出能量，热量仅用于内部加工）
 * - 温度范围 0-100%，影响加工速度
 * - 两个输入槽 + 两个输出槽 + 一个电池槽 + 两个升级槽
 * - 停电就停止加热，温度自然冷却
 * - 两个输入槽共用一个温度值（同时加工）
 *
 * 参数：
 * - 起始1%需要约1000EU，达到100%需要约95EU每刻维持
 * - 最低加热时间16EU/t，每次加工约0.609375s（2.1875ticks）
 * - 最低维持耗电约1EU/t
 * - 最大持续耗电约128EU/t（MV）
 */
@SuppressWarnings("null")
public class mio_icif_induction_elc extends AbstractProcessingMachineBlockEntity {
    // Per-call-site recipe memo: canWork() runs every tick, getRecipeFor is a linear scan.
    // electric_furnace recipes (recipe API, datapacks) first, then vanilla smelting
    private final com.miophas.singularity_iteration.common.recipe.generic.ElectricSmelting recipeCache1 = new com.miophas.singularity_iteration.common.recipe.generic.ElectricSmelting();
    private final com.miophas.singularity_iteration.common.recipe.generic.ElectricSmelting recipeCache2 = new com.miophas.singularity_iteration.common.recipe.generic.ElectricSmelting();

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(2)
        .battery()
        .output(2)
        .upgrade(2)
        .build();

    // 槽位定义
    public static final int INPUT_SLOT_1 = 0;      // 第一个输入槽
    public static final int INPUT_SLOT_2 = 1;      // 第二个输入槽
    public static final int BATTERY_SLOT = 2;      // 电池槽
    public static final int OUTPUT_SLOT_1 = 3;     // 第一个输出槽
    public static final int OUTPUT_SLOT_2 = 4;     // 第二个输出槽
    public static final int UPGRADE_SLOT_1 = 5;    // 升级槽
    public static final int UPGRADE_SLOT_2 = 6;    // 升级槽
    public static final int TOTAL_SLOTS = 7;       // 总槽位数

    public static final long DEFAULT_CAPACITY = 10000L;
    public static final long DEFAULT_MAX_RECEIVE = 128L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final long DEFAULT_ENERGY_PER_TICK = 15L;

    public static final int MAX_HEAT = 10000;
    public static final int PROGRESS_TARGET = 4000;
    public static final int WORK_ENERGY_COST = 15;

    public static final int HEAT_CAPACITY = 10000;
    public static final int HEAT_MAX_RECEIVE = 0;
    public static final int HEAT_MAX_EXTRACT = 0;
    public static final int HEAT_BASE_TEMP = 20;
    public static final int HEAT_MAX_TEMP = 1000;
    public static final float HEAT_LOSS_FACTOR = 0.005f;
    public static final int HEAT_UP_RATE = 1;

    // Heat storage of the machine.
    protected final HeatStorage heatStorage;

    /**
     * 0.1.9.1 heat hold (GUI switch): the furnace keeps heating while it has nothing to smelt and
     * holds full heat, so the next item smelts at full speed at once. 0.1.11.113: holding full temperature costs what the
     * furnace loses, 8 EU/t (2 EU/t with a Thermal Retention Unit; it was a flat 1 EU/t, nothing when insulated).
     */
    private boolean heatHold;



    /**
     * Constructor for BlockEntityType.Builder.
     */
    public mio_icif_induction_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.INDUCTION_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_induction_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            PROGRESS_TARGET,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.MV);

        this.heatStorage = new HeatStorage(
            HEAT_CAPACITY,
            HEAT_MAX_RECEIVE,
            HEAT_MAX_EXTRACT,
            HEAT_BASE_TEMP,
            HEAT_MAX_TEMP,
            HEAT_LOSS_FACTOR
        );
    }



    /**
     * Returns the heat storage.
     */
    public HeatStorage getHeatStorage() {
        return heatStorage;
    }

    /**
     * Returns the heat storage for the capability system. All sides share the same storage.
     */
    @Nullable
    public IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        return heatStorage;
    }

    /**
     * 判断物品是否可以放入指定槽位
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT_1, INPUT_SLOT_2 -> isSmeltable(stack);
            case BATTERY_SLOT -> isBattery(stack);
            case OUTPUT_SLOT_1, OUTPUT_SLOT_2 -> false;
            case UPGRADE_SLOT_1, UPGRADE_SLOT_2 -> isUpgrade(stack);
            default -> false;
        };
    }

    /**
     * 判断物品是否是升级件
     */
    private boolean isUpgrade(ItemStack stack) {
        return stack.getItem() instanceof com.miophas.singularity_iteration.common.item.upgrade.mio_icif_upgrade;
    }

    /**
     * 判断物品是否可以冶炼
     */
    private boolean isSmeltable(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        return recipeCache1.find(level, stack).isPresent();
    }

    /**
     * Finds the smelting recipe for the item in the input slot.
     */
    private Optional<RecipeHolder<SmeltingRecipe>> findRecipe(int inputSlot) {
        if (level == null) {
            return Optional.empty();
        }
        ItemStack input = itemHandler.getStackInSlot(inputSlot);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        return recipeCache2.find(level, input);
    }

    // 0.1.10.38: constant slot lists (they were new arrays on every call, several per tick); never modify them.
    private static final int[] SIDE_SLOTS = {INPUT_SLOT_1, INPUT_SLOT_2, BATTERY_SLOT, OUTPUT_SLOT_1, OUTPUT_SLOT_2};

    /**
     * 指定方向可访问的槽位
     */
    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return SIDE_SLOTS;
    }

    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    /**
     * 判断指定槽位是否可以从指定方向提取物品
     */
    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        return slot == OUTPUT_SLOT_1 || slot == OUTPUT_SLOT_2;
    }

    /**
     * 判断当前输入是否有有效的冶炼配方
     * 用于判断当前物品是否更该设置进度
     */
    @Override
    protected boolean hasValidRecipe() {
        return canProcessSlot(INPUT_SLOT_1, OUTPUT_SLOT_1) || canProcessSlot(INPUT_SLOT_2, OUTPUT_SLOT_2);
    }

    /**
     * Checks if the machine can work. At least one input slot must have a valid recipe.
     */
    @Override
    protected boolean canWork() {
        return canProcessSlot(INPUT_SLOT_1, OUTPUT_SLOT_1) || canProcessSlot(INPUT_SLOT_2, OUTPUT_SLOT_2);
    }

    /**
     * 判断指定槽位是否可以加工
     */
    private boolean canProcessSlot(int inputSlot, int outputSlot) {
        return prepareLane(inputSlot, outputSlot).isPresent();
    }

    private Optional<RecipeSlots.Prepared> prepareLane(int inputSlot, int outputSlot) {
        var recipe = findRecipe(inputSlot);
        if (recipe.isEmpty()) return Optional.empty();
        // 0.1.10.13: a smelting recipe of another mod may give a null result (GitHub issue #18)
        ItemStack result = com.miophas.singularity_iteration.common.util.RecipeResults.of(recipe.get().value(), level.registryAccess());
        if (result.isEmpty()) return Optional.empty();
        return RecipeSlots.prepare(itemHandler, inputSlot, 1, new int[]{outputSlot}, java.util.List.of(result));
    }

    private Optional<RecipeSlots.Prepared> prepareWorkingLanes() {
        var lanes = new java.util.ArrayList<RecipeSlots.Prepared>();
        prepareLane(INPUT_SLOT_1, OUTPUT_SLOT_1).ifPresent(lanes::add);
        prepareLane(INPUT_SLOT_2, OUTPUT_SLOT_2).ifPresent(lanes::add);
        return RecipeSlots.combine(lanes);
    }

    /**
     * Overclocker factor: the base machines turn the upgrade's process-time multiplier into whole
     * progress steps ({@link #getProgressPerTick()}); the induction furnace's heat model scales
     * continuously instead - heat builds up and converts into progress this many times faster.
     */
    private double overclockSpeed() {
        return 1.0 / Math.max(1.0e-3, upgradeStats.getProcessTimeMultiplier());
    }

    /** Energy cost scaled by the overclockers' energy multiplier (rounded up, at least the base). */
    private long scaledCost(long base) {
        return Math.max(base, (long) Math.ceil(base * upgradeStats.getEnergyUsageMultiplier()));
    }

    // ---- 0.1.11.113 (processing overhaul M6): the melt temperature ----

    /**
     * An induction furnace heats the charge itself (eddy currents in the crucible). Temperature, not a heat counter:
     * the 64 EU/t heater brings the crucible from 20 °C to 1,600 °C in about 30 seconds (heat capacity 24.3 EU/K); the
     * furnace loses heat in proportion to how hot it is (8 EU/t at 1,600 °C; a quarter with a Thermal Retention Unit),
     * so at temperature the thermostat only replaces that loss, and a furnace left alone cools with a time constant of
     * about 4 minutes. Smelting starts at 600 °C and runs at full speed at 1,600 °C (an item every 12 ticks per lane, as
     * the full heat did), with 15 EU/t for the melt on top.
     */
    public static final double AMBIENT = 20, MAX_TEMPERATURE = 1600, SMELT_START = 600;
    public static final long HEATER_EU = 64;
    public static final double HEAT_CAPACITY_EU_PER_K = 24.3, LOSS_AT_MAX_EU = 8, RETENTION_LOSS_SHARE = 0.25;
    /** Progress per tick at full temperature: an operation in 12 ticks. */
    public static final double FULL_SPEED_PROGRESS = PROGRESS_TARGET / 12.0;

    private double temperature = AMBIENT;
    /** EU the heater drew in the last tick (for the screen). */
    private long heaterDraw;

    /** Heat loss now (EU/t): proportional to the temperature above ambient; a quarter with thermal retention. */
    public double heatLoss() {
        double k = LOSS_AT_MAX_EU / (MAX_TEMPERATURE - AMBIENT) * (thermalRetention() ? RETENTION_LOSS_SHARE : 1);
        return k * Math.max(0, temperature - AMBIENT);
    }

    public double temperature() { return temperature; }
    public long heaterDraw() { return heaterDraw; }

    /** Share of full smelting speed at the current temperature: 0 below 600 °C, 1 at 1,600 °C. */
    public double smeltRate() {
        return Math.max(0, Math.min(1, (temperature - SMELT_START) / (MAX_TEMPERATURE - SMELT_START)));
    }

    @Override
    protected void onTick() {
        boolean remote = isRemotelyDisabled();   // 0.1.11.113: a remote stop stops the heater too
        boolean working = !remote && canWork();
        boolean heating = !remote && (working || heatHold || level != null && redstoneSignal());
        double loss = heatLoss();
        double before = temperature;
        heaterDraw = 0;
        if (heating) {
            // the thermostat: up to the heater's power, no more than reaches the top temperature
            double needed = (MAX_TEMPERATURE - temperature) * HEAT_CAPACITY_EU_PER_K + loss;
            long want = Math.min(HEATER_EU, (long) Math.ceil(Math.max(0, needed)));
            long paid = want <= 0 ? 0 : apiUseEnergy(Math.min(want, apiGetStoredEnergy()), false);
            heaterDraw = paid;
            temperature += (paid - loss) / HEAT_CAPACITY_EU_PER_K;
        } else {
            temperature -= loss / HEAT_CAPACITY_EU_PER_K;
        }
        temperature = Math.max(AMBIENT, Math.min(MAX_TEMPERATURE, temperature));
        heatStorage.setHeat(Math.round((temperature - AMBIENT) / (MAX_TEMPERATURE - AMBIENT) * HEAT_CAPACITY));   // the old heat gauge, for other readers
        if (Math.abs(temperature - before) > 0.5) setChanged();
    }

    /** 0.1.11.113: why the furnace runs, heats or stands (a {@link com.miophas.singularity_iteration.common.processing.ProcessStatus} code). */
    public int processStatus() {
        if (level == null) return com.miophas.singularity_iteration.common.processing.ProcessStatus.NO_INPUT;
        if (!canWorkRedstone()) return com.miophas.singularity_iteration.common.processing.ProcessStatus.REDSTONE_OFF;
        if (itemHandler.getStackInSlot(INPUT_SLOT_1).isEmpty() && itemHandler.getStackInSlot(INPUT_SLOT_2).isEmpty())
            return com.miophas.singularity_iteration.common.processing.ProcessStatus.NO_INPUT;
        if (findRecipe(INPUT_SLOT_1).isEmpty() && findRecipe(INPUT_SLOT_2).isEmpty())
            return com.miophas.singularity_iteration.common.processing.ProcessStatus.NO_RECIPE;
        if (prepareWorkingLanes().isEmpty()) return com.miophas.singularity_iteration.common.processing.ProcessStatus.OUTPUT_FULL;
        if (isWorking()) return smeltRate() <= 0
            ? com.miophas.singularity_iteration.common.processing.ProcessStatus.HEATING : com.miophas.singularity_iteration.common.processing.ProcessStatus.RUNNING;
        return com.miophas.singularity_iteration.common.processing.ProcessStatus.NO_POWER;
    }

    /** True with a Thermal Retention Unit in an upgrade slot (0.1.9.1). */
    public boolean thermalRetention() {
        for (int slot = UPGRADE_SLOT_1; slot <= UPGRADE_SLOT_2; slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.getItem() instanceof com.miophas.singularity_iteration.common.item.upgrade.mio_icif_upgrade u
                    && u.getLocalUpgradeType() == com.miophas.singularity_iteration.common.item.upgrade.mio_icif_upgrade.UpgradeType.THERMAL_RETENTION) return true;
        }
        return false;
    }

    public boolean heatHold() { return heatHold; }

    public void toggleHeatHold() {
        heatHold = !heatHold;
        setChanged();
    }

    /** Ticks until full temperature at the heater's power (0 when there). */
    public int ticksToFullHeat() {
        if (temperature >= MAX_TEMPERATURE - 0.5) return 0;
        double net = Math.max(1, HEATER_EU - heatLoss());
        return (int) Math.ceil((MAX_TEMPERATURE - temperature) * HEAT_CAPACITY_EU_PER_K / net);
    }

    /**
     * 执行生产工作 - 两个槽位共用进度值（同时加工）
     */
    @Override
    protected void doWork() {
        if (prepareWorkingLanes().isEmpty()) { stopWork(); return; }
        if (progress < PROGRESS_TARGET) {
            long workCost = scaledCost(WORK_ENERGY_COST);
            if (apiGetStoredEnergy() < workCost || apiUseEnergy(workCost, false) != workCost) {
                stopWork(); return;
            }
            long gain = Math.round(FULL_SPEED_PROGRESS * smeltRate() * overclockSpeed());   // 0.1.11.113: by temperature
            progress = (int) Math.min(PROGRESS_TARGET, (long) progress + gain);
        }
        isWorking = true;
        if (progress >= PROGRESS_TARGET) finishSmeltingBoth();
    }

    /**
     * 完成冶炼 - 同一时间处理两个槽位
     */
    private void finishSmeltingBoth() {
        var operation = prepareWorkingLanes();
        if (operation.isEmpty()) { stopWork(); return; }
        float xp = 0;
        if (prepareLane(INPUT_SLOT_1, OUTPUT_SLOT_1).isPresent()) xp += findRecipe(INPUT_SLOT_1).map(r -> r.value().getExperience()).orElse(0F);
        if (prepareLane(INPUT_SLOT_2, OUTPUT_SLOT_2).isPresent()) xp += findRecipe(INPUT_SLOT_2).map(r -> r.value().getExperience()).orElse(0F);
        int completed = progress;
        progress = 0; isWorking = false;
        if (!operation.get().commit()) progress = completed;
        else {
            storedXp.add(xp);
            super.finishWork();   // 0.1.11.113: counted for the work cycle upgrades, with the completion cue
        }
        setChanged();
    }

    /** Banked smelting XP, collected straight into the player's experience bar. */
    private final com.miophas.singularity_iteration.common.processing.StoredExperience storedXp = new com.miophas.singularity_iteration.common.processing.StoredExperience();

    public float getStoredExperience() { return storedXp.get(); }

    public int collectExperience(net.minecraft.server.level.ServerPlayer player) {
        int given = storedXp.collect(player);
        if (given > 0) setChanged();
        return given;
    }

    /**
     * 完成单个槽位的冶炼
     */


    public int getHeatPercent() {
        return (int) Math.round((temperature - AMBIENT) / (MAX_TEMPERATURE - AMBIENT) * 100);
    }

    public int gaugeProgressScaled(int i) {
        return (int) com.miophas.singularity_iteration.core.api.util.BoundedUnits.multiplyDivide(Math.max(0, Math.min(PROGRESS_TARGET, progress)), Math.max(0, i), PROGRESS_TARGET);
    }

    /**
     * Runs the machine logic on each tick.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_induction_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        AbstractProcessingMachineBlockEntity.tick(level, pos, state, blockEntity);

        blockEntity.syncLit(level, pos, state, com.miophas.singularity_iteration.common.block.producer.mio_icif_block_induction_elc.LIT);   // 0.1.10.38: off after LIT_OFF_DELAY idle ticks
    }

    @Override
    protected boolean canWorkRedstone() {
        if (level == null) {
            return true;
        }
        if (isRemotelyDisabled()) return false;   // 0.1.11.113: the remote stop was ignored here
        boolean powered = redstoneSignal();
        if (upgradeStats.isRedstoneInverted()) {
            return powered;
        }
        return !powered;
    }

    @Override
    protected void stopWork() {
        isWorking = false;
    }

    @Override
    protected void finishWork() {
    }

    @Override
    protected void updateProgress() {
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("heat", heatStorage.getHeatStored());
        tag.putDouble("induction_temperature", temperature);   // 0.1.11.113
        tag.putInt("progress", progress);
        tag.putBoolean("heat_hold", heatHold);
        storedXp.save(tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("heat", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) heatStorage.setHeat(tag.getLong("heat"));
        else if (tag.contains("heat_percent", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC))
            heatStorage.setHeat(com.miophas.singularity_iteration.core.api.util.BoundedUnits.multiplyDivide(Math.max(0, Math.min(100, tag.getLong("heat_percent"))), HEAT_CAPACITY, 100));
        // 0.1.11.113: the temperature; an old save carries its heat over as the same share of the range
        if (tag.contains("induction_temperature")) temperature = Math.max(AMBIENT, Math.min(MAX_TEMPERATURE, tag.getDouble("induction_temperature")));
        else temperature = AMBIENT + (MAX_TEMPERATURE - AMBIENT) * Math.max(0, Math.min(1, heatStorage.getHeatStored() / (double) HEAT_CAPACITY));
        if (!tag.contains("progress") && tag.contains("progress1")) progress = tag.getInt("progress1");
        heatHold = tag.getBoolean("heat_hold");
        progress = Math.max(0, Math.min(PROGRESS_TARGET, progress));
        storedXp.load(tag);
    }

    // ==================== Getters ====================

    public int getProgress1() {
        return progress;
    }

    public int getProgress2() {
        return progress;
    }

    public int getMaxProgress1() {
        return PROGRESS_TARGET;
    }

    public int getMaxProgress2() {
        return PROGRESS_TARGET;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return PROGRESS_TARGET;
    }

    /** Energy per tick now: the melt (15 EU/t at the overclock's multiplier while working) plus the heater. */
    public long getCurrentEnergyCost() {
        return (isWorking ? scaledCost(WORK_ENERGY_COST) : 0) + heaterDraw;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.induction_elc");
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.producer.InductionElcMenu(
            containerId, playerInventory, this);
    }
}