package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.menu.producer.PowderAdvancedElcMenu;
import com.miophas.singularity_iteration.common.processing.Comminution;
import com.miophas.singularity_iteration.common.recipe.mio_icif_ModRecipes;
import com.miophas.singularity_iteration.common.recipe.mio_icif_PowderRecipe;
import com.miophas.singularity_iteration.common.recipe.mio_icif_SingleItemRecipeInput;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.machine.ISlotLayout;
import com.miophas.singularity_iteration.core.api.machine.ProcessCost;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractStandardMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache;
import com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 进阶旋风打粉机 (advanced cyclone macerator). 0.1.11.109 (processing overhaul M2): a dry air-swept mill with a cyclone
 * classifier, in closed circuit.
 * <ul>
 *   <li><b>Closed circuit.</b> The cyclone takes the fines away and returns the coarse particles to the mill, so nothing
 *       is ground twice: the energy per operation is 80 % of the macerator's (Bond's law at 47.2 EU per unit of work
 *       index, {@link Comminution}). Iron ore 480 EU (was 750).</li>
 *   <li><b>Rotor.</b> At full speed it draws 12 EU/t (MV): iron ore in 40 ticks. The rotor and fan have inertia: they
 *       speed up 10 % a second while the mill has work and power, and coast down 5 % a second otherwise. Below full
 *       speed the mill runs that share of the ticks, so its mean power and its speed fall together and the energy per
 *       operation stays the same.</li>
 *   <li><b>Hold speed.</b> A switch on the machine (menu button {@link PowderAdvancedElcMenu#BUTTON_HOLD_SPEED}) keeps the
 *       rotor turning while the mill is idle for {@link #HOLD_EU_PER_TICK} EU/t, so the next item runs at full speed.</li>
 *   <li><b>Cyclone.</b> At full speed the cut size d50 is about 75 μm with a circulating load of about 250 %
 *       ({@link #cutSizeMicrons()}, {@link #circulatingLoadPercent()}; the screen shows them).</li>
 * </ul>
 * It used to be a {@code GenericMachineBlockEntity} (5 EU/t × 150 ticks for everything, 750 EU, registered through the
 * machine builder from its constructor). The slots keep their order (input, output, battery, four upgrades), so saves
 * load unchanged; the rotor starts at rest.
 */
@SuppressWarnings("null")
public class mio_icif_powder_advanced_elc extends AbstractStandardMachineBlockEntity implements ProcessCost.Source {
    public static final int INPUT_SLOT = 0, OUTPUT_SLOT = 1, BATTERY_SLOT = 2, UPGRADE_SLOT_START = 3, SLOT_COUNT = 7;
    private static final SlotLayout LAYOUT = SlotLayout.builder().input(1).output(1).battery().upgrade(4).build();

    /** Stock buffer (EU), as before; the hardest operation is added on top. */
    public static final long DEFAULT_CAPACITY = 750L;
    public static final long DEFAULT_MAX_RECEIVE = 128L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    /** Power at full rotor speed (EU/t). */
    public static final long DEFAULT_ENERGY_PER_TICK = 12L;
    /** Closed-circuit energy factor: the cyclone returns the coarse part, nothing is overground. */
    public static final double EFFICIENCY = 0.8;
    /** Ticks per operation for iron ore at full speed (the reference; the material decides). */
    public static final int DEFAULT_WORK_TIME = 40;
    /** The hardest common operation (gravel, about 952 EU) sizes the buffer. */
    public static final long MAX_OPERATION_ENERGY = 960L;
    /** Rotor speed change per tick: +10 % a second spinning up, −5 % a second coasting. */
    public static final double SPIN_UP_PER_TICK = 0.10 / 20, COAST_PER_TICK = 0.05 / 20;
    /** Power to keep the rotor turning while idle (hold speed). */
    public static final long HOLD_EU_PER_TICK = 1L;
    /** Cyclone at full speed: cut size d50 (μm) and circulating load (%). */
    public static final double FULL_SPEED_D50 = 75, FULL_SPEED_CIRCULATING_LOAD = 250;

    private final RecipeLookupCache<mio_icif_SingleItemRecipeInput, mio_icif_PowderRecipe> recipeCache =
        new RecipeLookupCache<>(mio_icif_ModRecipes.POWDER_TYPE);
    private final RecipeLookupCache<mio_icif_SingleItemRecipeInput, mio_icif_PowderRecipe> slotCache =
        new RecipeLookupCache<>(mio_icif_ModRecipes.POWDER_TYPE);

    /** Rotor speed, 0..1 (saved). */
    private double speed;
    /** Fraction of a run tick carried to the next tick (saved). */
    private double carry;
    /** Keep the rotor turning while idle (saved). */
    private boolean holdSpeed;
    /** The rotor is held this tick (for the screen). */
    private boolean holding;

    private static ISlotLayout menuLayout;

    public mio_icif_powder_advanced_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.POWDER_ADVANCED_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_powder_advanced_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type, DEFAULT_CAPACITY, DEFAULT_MAX_RECEIVE, DEFAULT_MAX_EXTRACT, DEFAULT_WORK_TIME, LAYOUT,
            DEFAULT_ENERGY_PER_TICK, CableTier.MV);
    }

    /** The slot layout for the menu (input 0, output 1, battery 2, upgrades 3 to 6), the same as the machine's. */
    public static ISlotLayout getOrCreateLayout() {
        if (menuLayout == null) menuLayout = MioIcifAPI.instance().getMachineBuilderAPI().createStandardLayout(1, 1, true, 4);
        return menuLayout;
    }

    // ---- recipe ------------------------------------------------------------------------------------------------------

    private Optional<RecipeHolder<mio_icif_PowderRecipe>> findRecipe() {
        if (level == null) return Optional.empty();
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return Optional.empty();
        return recipeCache.find(level, new mio_icif_SingleItemRecipeInput(input));
    }

    private boolean isCrushable(ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        // the count is ignored here, so a recipe that takes several items accepts fewer while they are put in
        return slotCache.find(level, new mio_icif_SingleItemRecipeInput(stack.copyWithCount(64))).isPresent();
    }

    private Optional<RecipeSlots.Prepared> prepare(RecipeHolder<mio_icif_PowderRecipe> recipe) {
        return RecipeSlots.prepare(itemHandler, INPUT_SLOT, recipe.value().getIngredientCount(),
            new int[]{OUTPUT_SLOT}, java.util.List.of(recipe.value().getResultItem(level.registryAccess())));
    }

    @Override
    protected @Nullable net.minecraft.resources.ResourceLocation currentRecipeId() {
        return findRecipe().map(RecipeHolder::id).orElse(null);
    }

    @Override
    protected boolean hasValidRecipe() {
        return isCrushable(itemHandler.getStackInSlot(INPUT_SLOT));
    }

    @Override
    protected boolean canWork() {
        if (!hasEnoughEnergy()) return false;
        return findRecipe().flatMap(this::prepare).isPresent();
    }

    @Override
    protected void doWork() {
        if (!consumeEnergy()) { stopWork(); return; }
        isWorking = true;
        var recipe = findRecipe();
        if (recipe.isEmpty()) { stopWork(); return; }
        if (progress >= maxProgress) {
            var operation = prepare(recipe.get());
            if (operation.isEmpty() || !operation.get().commit()) { stopWork(); return; }
            finishWork();
            if (canWork()) isWorking = true;
        }
    }

    // ---- cost --------------------------------------------------------------------------------------------------------

    /** The energy of one operation: 80 % of the macerator's for the material, at 12 EU/t. */
    @Override
    public ProcessCost processCost() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || level == null) return ProcessCost.fixed(DEFAULT_WORK_TIME, DEFAULT_ENERGY_PER_TICK);
        return Comminution.maceratorCost(input, findRecipe().map(RecipeHolder::value).orElse(null), DEFAULT_ENERGY_PER_TICK, EFFICIENCY);
    }

    /** The stock 750 EU plus the hardest operation at the current overclock; it does not change with the input. */
    @Override
    protected long getProcessingCapacity() {
        double perOperation = MAX_OPERATION_ENERGY * upgradeStats.getEnergyUsageMultiplier() * upgradeStats.getProcessTimeMultiplier();
        return baseCapacity + upgradeStats.getEnergyStorageCount() * com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats.ENERGY_STORAGE_BONUS
            + (long) Math.ceil(perOperation);
    }

    // ---- rotor -------------------------------------------------------------------------------------------------------

    /** Runs on the share of the ticks equal to the rotor speed (none at rest). */
    @Override
    protected boolean advanceThisTick() {
        carry += speed;
        if (carry >= 1) { carry -= 1; return true; }
        return false;
    }

    /** Once a tick after production (from {@link #tick}): the rotor speeds up while the mill works, is held, or coasts down. */
    private void turnRotor() {
        double before = speed;
        holding = false;
        if (isWorking) {
            speed = Math.min(1, speed + SPIN_UP_PER_TICK);
        } else if (holdSpeed && speed > 0 && canWorkRedstone() && apiUseEnergy(HOLD_EU_PER_TICK, true) >= HOLD_EU_PER_TICK
                && apiUseEnergy(HOLD_EU_PER_TICK, false) >= HOLD_EU_PER_TICK) {
            holding = true;
        } else {
            speed = Math.max(0, speed - COAST_PER_TICK);
            if (speed == 0) carry = 0;
        }
        if (speed != before) setChanged();
    }

    /**
     * 0.1.11.110 (processing overhaul M3): why the cyclone macerator runs or stands, a {@link com.miophas.singularity_iteration.common.processing.ProcessStatus}
     * code for the screen.
     */
    public int processStatus() {
        if (level == null) return com.miophas.singularity_iteration.common.processing.ProcessStatus.NO_INPUT;
        if (!canWorkRedstone()) return com.miophas.singularity_iteration.common.processing.ProcessStatus.REDSTONE_OFF;
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return com.miophas.singularity_iteration.common.processing.ProcessStatus.NO_INPUT;
        var recipe = findRecipe();
        if (recipe.isEmpty()) return com.miophas.singularity_iteration.common.processing.ProcessStatus.NO_RECIPE;
        ItemStack result = recipe.get().value().getResultItem(level.registryAccess());
        ItemStack out = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (!out.isEmpty() && (!ItemStack.isSameItemSameComponents(out, result) || out.getCount() + result.getCount() > out.getMaxStackSize()))
            return com.miophas.singularity_iteration.common.processing.ProcessStatus.OUTPUT_FULL;
        if (isWorking()) return com.miophas.singularity_iteration.common.processing.ProcessStatus.RUNNING;
        // 0.1.11.174: idle with everything ready means it starts on the next tick; "not enough power" only when it is short
        return hasEnoughEnergy() ? com.miophas.singularity_iteration.common.processing.ProcessStatus.RUNNING : com.miophas.singularity_iteration.common.processing.ProcessStatus.NO_POWER;
    }

    /** 0.1.11.110: the work index of the input ×10 (0 without input), for the screen. */
    public int workIndexTenths() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        return input.isEmpty() ? 0 : (int) Math.round(com.miophas.singularity_iteration.common.processing.SiMaterials.workIndexOrDefault(input) * 10);
    }

    public double getRotorSpeed() { return speed; }
    public boolean isHoldingSpeed() { return holdSpeed; }
    public boolean isHolding() { return holding; }

    public void toggleHoldSpeed() {
        holdSpeed = !holdSpeed;
        setChanged();
    }

    /** Cyclone cut size (μm): finer at higher air speed, d50 ∝ 1/√speed (75 μm at full speed; none at rest). */
    public double cutSizeMicrons() {
        return speed <= 0 ? 0 : FULL_SPEED_D50 / Math.sqrt(speed);
    }

    /** Circulating load (% of the feed returned to the mill), proportional to the speed (250 % at full speed). */
    public double circulatingLoadPercent() {
        return FULL_SPEED_CIRCULATING_LOAD * speed;
    }

    // ---- slots -------------------------------------------------------------------------------------------------------

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) return isCrushable(stack);
        if (slot == BATTERY_SLOT) return isBattery(stack);
        if (slot == OUTPUT_SLOT) return false;
        if (slot >= UPGRADE_SLOT_START && slot < UPGRADE_SLOT_START + 4) return getItemAPI().isUpgrade(stack);
        return false;
    }

    private static final int[] SIDE_SLOTS = {INPUT_SLOT, BATTERY_SLOT, OUTPUT_SLOT}, INPUT_SLOTS = {INPUT_SLOT}, OUTPUT_SLOTS = {OUTPUT_SLOT};

    @Override
    protected int[] getSlotsForDirection(Direction side) { return SIDE_SLOTS; }

    @Override
    protected int[] getInputSlots() { return INPUT_SLOTS; }

    @Override
    protected int[] getOutputSlots() { return OUTPUT_SLOTS; }

    @Override
    protected int getBatterySlot() { return BATTERY_SLOT; }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) { return slot == OUTPUT_SLOT; }

    public ItemStack getResultItem() {
        if (level == null) return ItemStack.EMPTY;
        return findRecipe().map(r -> r.value().getResultItem(level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    public ItemStack getInputItem() { return itemHandler.getStackInSlot(INPUT_SLOT); }

    public ItemStack getOutputItem() { return itemHandler.getStackInSlot(OUTPUT_SLOT); }

    // ---- save --------------------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("cyclone_speed", speed);
        tag.putDouble("cyclone_carry", carry);
        tag.putBoolean("cyclone_hold", holdSpeed);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        speed = Math.max(0, Math.min(1, tag.getDouble("cyclone_speed")));
        carry = Math.max(0, Math.min(1, tag.getDouble("cyclone_carry")));
        holdSpeed = tag.getBoolean("cyclone_hold");
    }

    // ---- tick --------------------------------------------------------------------------------------------------------

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_powder_advanced_elc blockEntity) {
        if (level.isClientSide()) return;
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, blockEntity);
        blockEntity.turnRotor();
        blockEntity.syncLit(level, pos, state, com.miophas.singularity_iteration.common.block.producer.mio_icif_block_powder_advanced_elc.LIT);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.powder_advanced_elc");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PowderAdvancedElcMenu(containerId, playerInventory, this);
    }
}
