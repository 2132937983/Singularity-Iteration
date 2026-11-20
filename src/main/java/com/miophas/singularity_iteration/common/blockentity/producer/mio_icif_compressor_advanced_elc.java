package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.menu.producer.CompressorAdvancedElcMenu;
import com.miophas.singularity_iteration.common.processing.Pressing;
import com.miophas.singularity_iteration.common.processing.ProcessStatus;
import com.miophas.singularity_iteration.common.recipe.compressor.mio_icif_CompressorRecipe;
import com.miophas.singularity_iteration.common.recipe.compressor.mio_icif_CompressorRecipeInput;
import com.miophas.singularity_iteration.common.recipe.mio_icif_ModRecipes;
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
 * 进阶高压压缩机 (advanced compressor). 0.1.11.112 (processing overhaul M5): a hydraulic press with an accumulator.
 * <ul>
 *   <li><b>Accumulator.</b> The pump charges a gas-loaded accumulator from the grid (MV, up to 128 EU/t); the machine's
 *       energy buffer is that accumulator, 20,000 EU at a full 32 MPa (more with overclockers and storage upgrades). The
 *       oil pressure shown is the stored share of it.</li>
 *   <li><b>Stroke.</b> A pressing does the recipe's work (its time × EU/t, as the compressor does) in a quarter of the
 *       recipe's time, drawn from the accumulator at four times the recipe's power. A stroke starts only when the
 *       accumulator holds the whole stroke (the pressure of the operation, e.g. a dense plate 12.8 MPa, the industrial
 *       diamond 28.8 MPa); until then the press waits and the screen says it is charging.</li>
 *   <li><b>Rate.</b> With a strong supply the pump keeps up and the press runs every stroke back to back: four times the
 *       compressor's speed. With a weak one it runs as fast as the pump refills the accumulator.</li>
 * </ul>
 * It used to be a {@code GenericMachineBlockEntity} (25 EU/t × 70 ticks for everything, registered through the machine
 * builder from its constructor). The slots keep their order (input, output, battery, four upgrades) and the stored energy
 * carries over as accumulator charge.
 */
@SuppressWarnings("null")
public class mio_icif_compressor_advanced_elc extends AbstractStandardMachineBlockEntity implements ProcessCost.Source {
    public static final int INPUT_SLOT = 0, OUTPUT_SLOT = 1, BATTERY_SLOT = 2, UPGRADE_SLOT_START = 3, SLOT_COUNT = 7;
    private static final SlotLayout LAYOUT = SlotLayout.builder().input(1).output(1).battery().upgrade(4).build();

    public static final long DEFAULT_MAX_RECEIVE = 128L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    /** The stroke takes this share of the recipe's time (and four times its power). */
    public static final int STROKE_SPEED = 4;
    /** Stock stroke (a recipe without its own time): 300 / 4 = 75 ticks at 8 EU/t. */
    public static final int DEFAULT_WORK_TIME = Pressing.STOCK_TICKS / STROKE_SPEED;
    public static final long DEFAULT_ENERGY_PER_TICK = Pressing.STOCK_POWER * STROKE_SPEED;

    private final RecipeLookupCache<mio_icif_CompressorRecipeInput, mio_icif_CompressorRecipe> recipeCache =
        new RecipeLookupCache<>(mio_icif_ModRecipes.COMPRESSOR_TYPE);
    private final RecipeLookupCache<mio_icif_CompressorRecipeInput, mio_icif_CompressorRecipe> slotCache =
        new RecipeLookupCache<>(mio_icif_ModRecipes.COMPRESSOR_TYPE);

    /** The press is waiting for the accumulator this tick (for the screen). */
    private boolean charging;

    private static ISlotLayout menuLayout;

    public mio_icif_compressor_advanced_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.COMPRESSOR_ADVANCED_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_compressor_advanced_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type, Pressing.ACCUMULATOR_ENERGY, DEFAULT_MAX_RECEIVE, DEFAULT_MAX_EXTRACT, DEFAULT_WORK_TIME, LAYOUT,
            DEFAULT_ENERGY_PER_TICK, CableTier.MV);
    }

    /** The slot layout for the menu (input 0, output 1, battery 2, upgrades 3 to 6), the same as the machine's. */
    public static ISlotLayout getOrCreateLayout() {
        if (menuLayout == null) menuLayout = MioIcifAPI.instance().getMachineBuilderAPI().createStandardLayout(1, 1, true, 4);
        return menuLayout;
    }

    // ---- recipe ------------------------------------------------------------------------------------------------------

    private Optional<RecipeHolder<mio_icif_CompressorRecipe>> findRecipe() {
        if (level == null) return Optional.empty();
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return Optional.empty();
        return recipeCache.find(level, new mio_icif_CompressorRecipeInput(input));
    }

    private boolean isCompressible(ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        return slotCache.find(level, new mio_icif_CompressorRecipeInput(stack.copyWithCount(64))).isPresent();
    }

    private Optional<RecipeSlots.Prepared> prepare(RecipeHolder<mio_icif_CompressorRecipe> recipe) {
        return RecipeSlots.prepare(itemHandler, INPUT_SLOT, recipe.value().getIngredientCount(),
            new int[]{OUTPUT_SLOT}, java.util.List.of(recipe.value().getResult()));
    }

    @Override
    protected @Nullable net.minecraft.resources.ResourceLocation currentRecipeId() {
        return findRecipe().map(RecipeHolder::id).orElse(null);
    }

    @Override
    protected boolean hasValidRecipe() {
        return isCompressible(itemHandler.getStackInSlot(INPUT_SLOT));
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

    // ---- cost and accumulator ----------------------------------------------------------------------------------------

    /** The recipe's work in a quarter of its time: the same energy at four times the power. */
    @Override
    public ProcessCost processCost() {
        ProcessCost press = Pressing.cost(findRecipe().map(RecipeHolder::value).orElse(null));
        int ticks = Math.max(1, (press.ticks() + STROKE_SPEED - 1) / STROKE_SPEED);
        long power = Math.max(1L, (press.energy() + ticks - 1) / ticks);
        return new ProcessCost(power * ticks, power);
    }

    /** The accumulator: 20,000 EU at the current overclock (it holds the hardest stroke), plus storage upgrades. */
    @Override
    protected long getProcessingCapacity() {
        double scale = Math.max(1.0, upgradeStats.getEnergyUsageMultiplier() * upgradeStats.getProcessTimeMultiplier());
        return (long) Math.ceil(Pressing.ACCUMULATOR_ENERGY * scale)
            + upgradeStats.getEnergyStorageCount() * com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats.ENERGY_STORAGE_BONUS;
    }

    /** The energy of the stroke at the current overclock (EU). */
    public long strokeEnergy() {
        return getEffectiveEnergyPerTick() * Math.max(1, getMaxProgress());
    }

    /** A stroke starts only when the accumulator holds all of it; once started it runs to the end. */
    @Override
    protected boolean advanceThisTick() {
        if (progress > 0) { charging = false; return true; }
        charging = apiGetStoredEnergy() < strokeEnergy();
        return !charging;
    }

    /** Oil pressure now (MPa): the stored share of the accumulator. */
    public double pressureMpa() {
        long cap = Math.max(1, getEffectiveCapacity());
        return Pressing.FULL_PRESSURE_MPA * Math.min(1.0, (double) apiGetStoredEnergy() / cap);
    }

    /** Pressure the next stroke needs (MPa). */
    public double requiredPressureMpa() {
        long cap = Math.max(1, getEffectiveCapacity());
        return Pressing.FULL_PRESSURE_MPA * Math.min(1.0, (double) strokeEnergy() / cap);
    }

    /** Why the press runs, charges or stands (a {@link ProcessStatus} code). */
    public int processStatus() {
        if (level == null) return ProcessStatus.NO_INPUT;
        if (!canWorkRedstone()) return ProcessStatus.REDSTONE_OFF;
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return ProcessStatus.NO_INPUT;
        var recipe = findRecipe();
        if (recipe.isEmpty()) return ProcessStatus.NO_RECIPE;
        if (prepare(recipe.get()).isEmpty()) return ProcessStatus.OUTPUT_FULL;
        if (charging) return ProcessStatus.CHARGING;
        if (isWorking()) return ProcessStatus.RUNNING;
        return ProcessStatus.NO_POWER;
    }

    // ---- slots -------------------------------------------------------------------------------------------------------

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) return isCompressible(stack);
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
        return findRecipe().map(r -> r.value().getResult()).orElse(ItemStack.EMPTY);
    }

    public ItemStack getInputItem() { return itemHandler.getStackInSlot(INPUT_SLOT); }

    public ItemStack getOutputItem() { return itemHandler.getStackInSlot(OUTPUT_SLOT); }

    // ---- tick --------------------------------------------------------------------------------------------------------

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_compressor_advanced_elc blockEntity) {
        if (level.isClientSide()) return;
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, blockEntity);
        if (!blockEntity.isWorking()) blockEntity.charging = false;
        blockEntity.syncLit(level, pos, state, com.miophas.singularity_iteration.common.block.producer.mio_icif_block_compressor_advanced_elc.LIT);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.compressor_advanced_elc");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CompressorAdvancedElcMenu(containerId, playerInventory, this);
    }
}
