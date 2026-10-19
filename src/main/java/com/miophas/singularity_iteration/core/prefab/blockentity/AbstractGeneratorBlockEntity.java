package com.miophas.singularity_iteration.core.prefab.blockentity;

import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.storage.EUApi;
import com.miophas.singularity_iteration.core.api.energy.storage.IEUEnergyStorage;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyTile;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.EnergyCompatibility;
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
 * Base block entity for all generators. It extends the energy block entity.
 * It uses fuel items to make energy.
 * It implements {@link WorldlyContainer} so that hoppers and pipes can move items.
 * It uses the EU (Energy Unit) energy system.
 *
 * <p>Fuel handling goes through {@link IItemAPI}. Addons can register custom fuel types.
 */
@SuppressWarnings("null")
public abstract class AbstractGeneratorBlockEntity extends AbstractEnergyBlockEntity implements WorldlyContainer, com.miophas.singularity_iteration.core.api.machine.IGeneratorBlock {
    
    // ==================== API method layer ====================
    // Fuel and energy operations go through protected methods. Subclasses can override them to change the behavior.

    /**
     * Returns the item API instance.
     */
    protected IItemAPI getItemAPI() {
        return MioIcifAPI.instance().getItemAPI();
    }

    /**
     * Returns the burn time of a fuel item through the API, in ticks.
     * Subclasses can override this method to support custom fuels.
     */
    protected int apiGetFuelBurnTime(ItemStack fuel) {
        // First, query the API (when the fuel is a mod battery).
        if (getItemAPI().isBattery(fuel)) {
            long energy = getItemAPI().getBatteryStored(fuel);
            // 100 EU of stored battery energy gives 1 tick of burn time (subclasses can change this).
            return (int) (energy / 100);
        }
        // Fall back to the abstract method of the subclass.
        return getFuelBurnTime(fuel);
    }

    /**
     * Returns true if the item is a battery, through the API.
     */
    protected boolean apiIsBattery(ItemStack stack) {
        return getItemAPI().isBattery(stack);
    }

    /**
     * Removes energy from a battery item through the API.
     *
     * @param amount the maximum energy to remove, in EU
     * @param simulate if true, do not change the item
     * @return the energy removed, in EU
     */
    protected long apiDischargeBattery(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().dischargeBattery(stack, amount, simulate);
    }

    /**
     * Returns the energy that is stored in a battery item, in EU, through the API.
     */
    protected long apiGetBatteryStored(ItemStack stack) {
        return getItemAPI().getBatteryStored(stack);
    }

    /**
     * Charges a battery item through the API.
     *
     * @param amount the maximum energy to add, in EU
     * @param simulate if true, do not change the item
     * @return the energy added, in EU
     */
    protected long apiChargeBattery(ItemStack stack, long amount, boolean simulate) {
        return getItemAPI().chargeBattery(stack, amount, simulate);
    }

    /**
     * Returns the energy capacity of a battery item, in EU, through the API.
     */
    protected long apiGetBatteryCapacity(ItemStack stack) {
        return getItemAPI().getBatteryCapacity(stack);
    }

    /**
     * Returns the charge rate of a battery item, in EU/tick, through the API.
     */
    protected long apiGetChargeRate(ItemStack stack) {
        return getItemAPI().getChargeRate(stack);
    }

    /**
     * Returns true if the battery item is full, through the API.
     */
    protected boolean apiIsBatteryFull(ItemStack stack) {
        return getItemAPI().isBatteryFull(stack);
    }

    // ------------------------------------------------------------------ output scale (0.1.8.4)

    /**
     * Output multiplier of a generator, by block entity. The mod sets it from its configuration
     * ("GeneratorOutput", one value per generator); addons and other loaders get 1.
     */
    public static volatile java.util.function.ToDoubleFunction<net.minecraft.world.level.block.entity.BlockEntity> OUTPUT_SCALE = be -> 1.0;
    /** Fraction of an EU left over by {@link #scaleOutput(long)}, carried to the next tick (not saved). */
    private double outputCarry;

    /** The output multiplier of this generator (0 stops it). */
    protected double outputScale() {
        double m = OUTPUT_SCALE.applyAsDouble(this);
        return Double.isFinite(m) && m > 0 ? m : 0;
    }

    /** {@code eu} times the output multiplier, in whole EU; the fraction is carried to later ticks. */
    protected long scaleOutput(long eu) {
        double m = outputScale();
        if (m == 1.0 || eu <= 0) return m == 0 ? 0 : eu;
        double v = eu * m + outputCarry;
        long whole = (long) Math.floor(v);
        outputCarry = v - whole;
        return whole;
    }

    /** An exact amount (network-controlled storage) times the output multiplier. */
    protected com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount scaleOutput(
            com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount amount) {
        double m = outputScale();
        return m == 1.0 ? amount : com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount.fromDouble(amount.toDouble() * m);
    }

    /**
     * Generates energy into the internal storage (internal generation), through the API.
     * This call bypasses the maxReceive limit.
     *
     * @param amount the energy to generate, in EU
     */
    protected long apiGenerateEnergy(long amount) {
        return apiGenerateEnergy(amount, false);
    }

    /**
     * Returns the stored energy, in EU, through the API.
     * This method reads energyStorage directly. This prevents infinite recursion through getEnergyAPI().
     */
    @Override
    protected long apiGetStoredEnergy() {
        return energyStorage.getAmount();
    }

    /**
     * Returns the maximum energy capacity, in EU, through the API.
     * This method reads energyStorage directly. This prevents infinite recursion through getEnergyAPI().
     */
    @Override
    protected long apiGetMaxEnergy() {
        return energyStorage.getCapacity();
    }

    /**
     * Extracts energy, in EU, through the API.
     * This method reads energyStorage directly. This prevents infinite recursion through getEnergyAPI().
     */
    @Override
    protected long apiExtractEnergy(long amount, boolean simulate) {
        return energyStorage.extract(amount, simulate);
    }
    
    // Slot layout
    protected SlotLayout slotLayout;
    // Item handler
protected MachineItemHandler itemHandler;

    // Remaining burn time of the current fuel, in ticks
    public int burnTime = 0;

    // Total burn time of the current fuel item, in ticks
public int burnDuration = 0;

    // Energy generation rate (EU/tick)
    protected final long energyGenerationRate;

    // Index of the fuel slot
protected static final int FUEL_SLOT = 0;

    // Index of the charge slot
protected static final int CHARGE_SLOT = 1;
    
    /**
     * Creates a generator with the default energy storage.
     *
     * @param pos the block position
     * @param state the block state
     * @param type the block entity type
     * @param layout the slot layout
     * @param energyGenerationRate the energy generation rate (EU/tick)
     */
    public AbstractGeneratorBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                     SlotLayout layout, long energyGenerationRate) {
        super(pos, state, type);
        this.slotLayout = layout;
        this.itemHandler = createItemHandler(layout);
        this.energyGenerationRate = energyGenerationRate;
        // A generator is a power source.
        setAsPowerSource(energyGenerationRate);
    }

    /**
     * Creates a generator with a custom energy storage.
     *
     * @param pos the block position
     * @param state the block state
     * @param type the block entity type
     * @param layout the slot layout
     * @param energyGenerationRate the energy generation rate (EU/tick)
     * @param capacity the energy capacity (EU)
     * @param maxReceive the maximum input rate (EU/tick)
     * @param maxExtract the maximum output rate (EU/tick)
     * @param cableTier the cable tier
     */
    public AbstractGeneratorBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                     SlotLayout layout, long energyGenerationRate,
                                     long capacity, long maxReceive, long maxExtract, CableTier cableTier) {
        super(pos, state, type, capacity, maxReceive, maxExtract, cableTier);
        this.slotLayout = layout;
        this.itemHandler = createItemHandler(layout);
        this.energyGenerationRate = energyGenerationRate;
        // A generator is a power source.
        setAsPowerSource(energyGenerationRate);
    }

    /**
     * Creates the item handler. The handler marks this block entity as changed when its contents change.
     */
    protected MachineItemHandler createItemHandler(SlotLayout layout) {
        MachineItemHandler handler = new MachineItemHandler(layout) {
            @Override
            protected void onContentsChanged(int slot) {
                AbstractGeneratorBlockEntity.this.setChanged();
            }
        };
        return handler;
    }
    
    /**
     * Runs the update logic each tick and generates energy. Server side only.
     *
     * <p>Important: energy distribution must run before the burn check.
     * The generator first tries to send energy out, and then decides whether to stop burning.
     * This prevents a stop when the internal storage is full but the grid can still take the energy.
     *
     * <p>Note: if the generator distributes energy through the grid
     * ({@link #shouldDirectlyDistributeEnergy()} returns false), the direct distribution is skipped.
     * The grid then extracts energy through extractPowerForConsumer.
     *
     * <p>Burn stop logic: when the energy storage is full and the grid has no demand,
     * the current fuel continues until burnTime decreases to 0. The burn does not stop immediately.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, AbstractGeneratorBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        if (blockEntity.energyStorage.scexNetworkControlled()) {
            blockEntity.tickIndependentThermal(level, pos);
            return;
        }
        
        // Record the burn state before this tick.
    boolean wasBurning = blockEntity.isBurning();
        
        // First, charge the item in the charge slot.
    blockEntity.chargeItems();
        
        if (blockEntity.shouldDirectlyDistributeEnergy()) {
            blockEntity.distributeEnergy();
        } else {
            blockEntity.distributeEnergyToCompatSinks();
        }
        
        // After distribution, check whether the energy storage is full.
        // Stop burning only when the energy really cannot go out (the storage is full and distribution failed).
    boolean isEnergyFull = blockEntity.apiGetStoredEnergy() >= 
                              blockEntity.apiGetMaxEnergy();
        
        // If the generator burns, let the current fuel finish.
        if (blockEntity.isBurning()) {
            if (isEnergyFull) {
                // Storage full and no grid demand: decrease the burn time to 0. Do not generate energy. Do not use new fuel.
                blockEntity.burnTime--;
                // When the current fuel ends, do not use new fuel (the storage is full and cannot output).
                if (blockEntity.burnTime <= 0) {
                    blockEntity.burnTime = 0;
                }
            } else {
                // Storage not full: generate energy and decrease the burn time.
                blockEntity.generateEnergy();
                blockEntity.burnTime--;
                
                // When the burn ends, try to use new fuel.
            if (blockEntity.burnTime <= 0) {
                    blockEntity.consumeFuel();
                }
            }
        } else {
            // If the generator does not burn and the storage is not full, try to use new fuel.
        if (!isEnergyFull) {
                blockEntity.consumeFuel();
            }
        }
        
        // Check whether the burn state changed.
    boolean isBurning = blockEntity.isBurning();
        if (wasBurning != isBurning) {
            // Update the block state.
            blockEntity.updateActiveState(level, pos, isBurning);
        }
        
        // Mark the block entity as changed.
    // 0.1.10.10 (D1): save mark only (setChanged() every tick also updated the six neighbours)
    com.miophas.singularity_iteration.core.runtime.TickMarks.saved(blockEntity);
    }
    
    /**
     * Independent R12 thermal cycle from frozen public game observations.
     * The separate independent network performs all wire transfers after world
     * ticks; this branch never calls old grid or neighbour energy adapters.
     */
    protected void tickIndependentThermal(Level world, BlockPos at) {
        long previousEnergy = energyStorage.getAmount();
        int previousBurn = burnTime;
        chargeItems();
        long room = energyStorage.scexExactAmount().roomBelow(energyStorage.getCapacity()).whole();
        if (burnTime <= 0 && room >= energyGenerationRate) consumeFuel();
        boolean activeThisTick = burnTime > 0;
        if (activeThisTick) {
            energyStorage.scexGenerateEnergy(scaleOutput(com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount.of(energyGenerationRate)), false);
            burnTime--;
        }
        updateActiveState(world, at, activeThisTick);
        if (previousBurn != burnTime || previousEnergy != energyStorage.getAmount()) {
            // Save burn progress without comparator/world notifications on every
            // active tick. Never load a chunk merely to dirty its saved state.
            if (world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                var chunk = serverLevel.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
                if (chunk != null) chunk.setUnsaved(true);
            }
        }
    }

    /** Updates a declared active property; machines with other state models may override this hook. */
    protected void updateActiveState(Level world, BlockPos position, boolean active) {
        BlockState state = world.getBlockState(position);
        var property = state.getBlock().getStateDefinition().getProperty("active");
        if (property instanceof net.minecraft.world.level.block.state.properties.BooleanProperty flag
                && state.getValue(flag) != active) world.setBlock(position, state.setValue(flag, active), 3);
    }

    /**
     * Sends energy to adjacent block entities.
     * The generator outputs energy on all six sides.
     */
    protected void distributeEnergy() {
        // The independent network owns all neighbour transfers for controlled generators.
        if (energyStorage.scexNetworkControlled() || apiGetStoredEnergy() <= 0) {
            return;
        }
        
        // Output energy in all six directions.
    for (Direction direction : Direction.values()) {
            outputEnergyToDirection(direction);
        }
    }
    
    /**
     * Sends energy to adjacent compatible blocks that are not an {@link IEnergyTile}.
     * When the generator distributes energy through the grid ({@link #shouldDirectlyDistributeEnergy()} returns false),
     * the grid can only send energy to IEnergySink nodes. It cannot find energy receivers of other mods, such as AE2.
     * This method fills that gap. It pushes energy to adjacent blocks that are not an IEnergyTile
     * but have a compatible energy storage.
     */
    protected void distributeEnergyToCompatSinks() {
        if (energyStorage.scexNetworkControlled() || apiGetStoredEnergy() <= 0) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (apiGetStoredEnergy() <= 0) break;
            outputEnergyToCompatSink(direction);
        }
    }

    /**
     * Outputs energy to a compatible block in the given direction that is not an {@link IEnergyTile}.
     * It skips IEnergyTile blocks (the grid handles them). It only pushes energy to energy storages of external mods, such as AE2.
     */
    private void outputEnergyToCompatSink(Direction direction) {
        BlockPos adjacentPos = worldPosition.relative(direction);

        if (level.getBlockEntity(adjacentPos) instanceof IEnergyTile) {
            return;
        }

        try {
            long energyToTransfer = Math.min(energyStorage.getAmount(), energyStorage.getMaxExtract());
            long pushed = EnergyCompatibility.get().push(level, adjacentPos, direction, energyToTransfer);
            if (pushed > 0) {
                energyStorage.extract(pushed, false);
            }
        } catch (Exception e) {
            com.miophas.singularity_iteration.core.runtime.diagnostics.QuietFailures.report(
                "generator-push:" + getClass().getName(), "Generator energy push into " + adjacentPos, e);
        }
    }

    /**
     * Returns whether the generator sends energy directly to adjacent blocks.
     *
     * <p>The default is true: the generator outputs energy directly to adjacent blocks.
     * A generator that uses the grid to distribute energy (for example, the thermal generator) must override this method and return false.
     * The grid then extracts energy from the generator storage through extractPowerForConsumer.
     *
     * @return true = distribute energy directly (default), false = distribute energy through the grid
     */
    protected boolean shouldDirectlyDistributeEnergy() {
        return true;
    }
    
    /**
     * Outputs energy in the given direction.
     */
    private void outputEnergyToDirection(Direction direction) {
        BlockPos adjacentPos = worldPosition.relative(direction);

        try {
            IEUEnergyStorage adjacentStorage = level.getCapability(EUApi.SIDED, adjacentPos, direction.getOpposite());

            if (adjacentStorage == null) adjacentStorage = EnergyCompatibility.get().findEuStorage(level, adjacentPos, direction.getOpposite());

            if (adjacentStorage != null) {
                long energyToTransfer = Math.min(energyStorage.getAmount(), energyStorage.getMaxExtract());
                long energyReceived = adjacentStorage.receive(energyToTransfer, false);
                if (energyReceived > 0) {
                    energyStorage.extract(energyReceived, false);
                }
                return;
            }

            long energyToTransfer = Math.min(energyStorage.getAmount(), energyStorage.getMaxExtract());
            long pushed = EnergyCompatibility.get().pushDirect(level, adjacentPos, direction, energyToTransfer, energyStorage.getCableTier());
            if (pushed > 0) {
                energyStorage.extract(pushed, false);
            }
        } catch (Exception e) {
            // Safe fallback: do not crash. Skip only this output, but report it.
            com.miophas.singularity_iteration.core.runtime.diagnostics.QuietFailures.report(
                "generator-output:" + getClass().getName(), "Generator output towards " + adjacentPos, e);
        }
    }
    
    /**
     * Generates energy for one tick. The amount is the generation rate, limited by the free storage space.
     */
    protected void generateEnergy() {
        long energyGenerated = Math.min(scaleOutput(energyGenerationRate),
            apiGetMaxEnergy() - apiGetStoredEnergy());
        // Write the energy through the API.
        apiGenerateEnergy(energyGenerated);
    }
    
    /**
     * Charges the item in the charge slot.
     */
    protected void chargeItems() {
        // Check whether a charge slot exists.
        if (itemHandler == null || itemHandler.getSlots() <= CHARGE_SLOT) {
            return;
        }

        ItemStack chargeStack = itemHandler.getStackInSlot(CHARGE_SLOT);
        if (chargeStack.isEmpty()) {
            return;
        }
        
        // Check whether the item is a battery.
        if (apiIsBattery(chargeStack)) {
            long currentEnergy = apiGetBatteryStored(chargeStack);
            long batteryMaxEnergy = apiGetBatteryCapacity(chargeStack);
            long batteryChargeRate = apiGetChargeRate(chargeStack);
            
            if (currentEnergy >= batteryMaxEnergy) {
                return;
            }
            
            long availableEnergy = apiGetStoredEnergy();
            if (availableEnergy <= 0) {
                return;
            }
            
            long effectiveChargeRate = Math.min(batteryChargeRate, apiGetMaxExtract());
            long energyToCharge = Math.min(effectiveChargeRate, batteryMaxEnergy - currentEnergy);
            energyToCharge = Math.min(energyToCharge, availableEnergy);
            
            long energyExtracted = apiExtractEnergy(energyToCharge, false);
            
            if (energyExtracted > 0) {
                apiChargeBattery(chargeStack, energyExtracted, false);
            }
        }
    }
    
    /** Lamp state of a generator: green while generating, red when its buffer is full (output blocked), amber otherwise (standby / low output). */
    @Override
    public com.miophas.singularity_iteration.core.api.machine.MachineStatus machineStatus() {
        if (isBurning() || runFlag()) return com.miophas.singularity_iteration.core.api.machine.MachineStatus.RUNNING;
        long cap = getEnergyStorage().getCapacity();
        if (cap > 0 && getEnergyStorage().getAmount() >= cap) return com.miophas.singularity_iteration.core.api.machine.MachineStatus.BLOCKED;
        return com.miophas.singularity_iteration.core.api.machine.MachineStatus.NO_POWER;
    }

    /**
     * Returns true if the generator burns fuel now (burnTime is more than 0).
     */
    public boolean isBurning() {
        return burnTime > 0;
    }

    /**
     * Returns the remaining burn time, in ticks.
     */
    public int getBurnTime() {
        return burnTime;
    }

    /**
     * Returns the maximum burn time (the total burn time of the current fuel), in ticks.
     */
    public int getMaxBurnTime() {
        return burnDuration;
    }

    @Override
    public int getDefaultBurnTime() {
        return getMaxBurnTime();
    }

    @Override
    public void setBurnTime(int ticks) {
        this.burnTime = ticks;
        this.setChanged();
    }

    /**
     * Returns the energy generation rate (EU/tick).
     */
    public long getEnergyGenerationRate() {
        return energyGenerationRate;
    }

    // ==================== IGeneratorBlock implementation ====================

    @Override
    public long getPowerOutput() {
        return energyGenerationRate;
    }

    @Override
    public net.minecraft.world.item.ItemStack getFuelSlotItem() {
        return itemHandler != null ? itemHandler.getStackInSlot(FUEL_SLOT) : net.minecraft.world.item.ItemStack.EMPTY;
    }

    @Override
    public net.minecraft.world.item.ItemStack getChargeSlotItem() {
        return itemHandler != null ? itemHandler.getStackInSlot(CHARGE_SLOT) : net.minecraft.world.item.ItemStack.EMPTY;
    }

    @Override
    public CableTier getCableTier() {
        return energyStorage.getCableTier();
    }
    
    /**
     * Uses one fuel item from the fuel slot and starts a new burn.
     * It does nothing when the energy storage is full.
     */
    protected void consumeFuel() {
        // Check whether the energy storage is full.
    boolean isEnergyFull = getEnergyStorage().getAmount() >=
                              getEnergyStorage().getCapacity();

        // If the energy storage is full, do not use fuel.
    if (isEnergyFull) {
            return;
        }

        ItemStack fuelStack = itemHandler.getStackInSlot(FUEL_SLOT);
        if (!fuelStack.isEmpty()) {
            int burnTime = getFuelBurnTime(fuelStack);
            if (burnTime > 0) {
                this.burnDuration = burnTime;
                this.burnTime = burnTime;

                // Get the container item before the fuel is used (for example, a lava bucket gives an empty bucket).
                ItemStack remainingItem = fuelStack.getCraftingRemainingItem();

                // Use the fuel.
            fuelStack.shrink(1);

                // If the fuel stack is empty, put in the container item (if there is one).
            if (fuelStack.isEmpty()) {
                    itemHandler.setStackInSlot(FUEL_SLOT, remainingItem);
                }
            }
        }
    }
    
    /**
     * Returns the burn time of a fuel item. Subclasses override this method to define the valid fuels.
     *
     * @param fuel the fuel item
     * @return the burn time (ticks). 0 or less means that the item is not a fuel.
     */
    public abstract int getFuelBurnTime(ItemStack fuel);
    
    /**
     * Returns the burn progress for the GUI.
     *
     * @return the burn progress (0 to 1)
     */
    public float getBurnProgress() {
        if (burnDuration == 0) {
            return 0;
        }
        return (float) burnTime / burnDuration;
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
    
    // Container implementation
    @Override
    public int getContainerSize() {
        return itemHandler != null ? itemHandler.getSlots() : 0;
    }

    @Override
    public boolean isEmpty() {
        if (itemHandler == null) return true;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return isValidItemSlot(slot) ? itemHandler.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!isValidItemSlot(slot) || amount <= 0) return ItemStack.EMPTY;
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int toRemove = Math.min(amount, stack.getCount());
        ItemStack result = stack.copyWithCount(toRemove);
        stack.shrink(toRemove);
        if (stack.isEmpty()) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!isValidItemSlot(slot)) return ItemStack.EMPTY;
        ItemStack stack = itemHandler.getStackInSlot(slot);
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!isValidItemSlot(slot)) return;
        itemHandler.setStackInSlot(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
    }

    private boolean isValidItemSlot(int slot) {
        return itemHandler != null && slot >= 0 && slot < itemHandler.getSlots();
    }

    @Override
    public boolean stillValid(Player player) {
        return player != null && level != null && !isRemoved() && level.getBlockEntity(worldPosition) == this &&
               player.distanceToSqr(worldPosition.getX() + 0.5,
                                   worldPosition.getY() + 0.5,
                                   worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        if (itemHandler != null) {
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                itemHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    // ==================== WorldlyContainer implementation ====================

    @Override
    public int[] getSlotsForFace(Direction side) {
        // All sides can access the fuel slot and the charge slot.
    return new int[]{FUEL_SLOT, CHARGE_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        // Fuel slot: accept only fuel.
    if (slot == FUEL_SLOT) {
            return getFuelBurnTime(stack) > 0;
        }
        // Charge slot: accept only battery items.
        if (slot == CHARGE_SLOT) {
            return isBattery(stack);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // The charge slot allows extraction (a player can take out a full battery).
        if (slot == CHARGE_SLOT) {
            return true;
        }
        // The fuel slot does not allow extraction (this stops hoppers from removing the fuel).
    return false;
    }

    /**
     * Returns true if the item is a battery.
     *
     * @param stack the item stack
     * @return true if the item is a battery
     */
    protected boolean isBattery(ItemStack stack) {
        return getItemAPI().isBattery(stack);
    }

    /**
     * Returns the item handler for the capability system.
     *
     * @param side the side, or null for no side
     * @return a new item handler that applies the side rules
     */
    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return new GeneratorItemHandler(side);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    // ==================== GeneratorItemHandler inner class ====================

    /**
     * The item handler of the generator.
     * The capability system uses it. It limits item insertion and extraction.
     */
    protected class GeneratorItemHandler implements IItemHandlerModifiable {
        private final Direction side;

        public GeneratorItemHandler(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int getSlots() {
            return itemHandler != null ? itemHandler.getSlots() : 0;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return itemHandler != null ? itemHandler.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (itemHandler == null) return stack;
            if (!canPlaceItemThroughFace(slot, stack, side)) {
                return stack;
            }
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (itemHandler == null) return ItemStack.EMPTY;
            if (!canTakeItemThroughFace(slot, itemHandler.getStackInSlot(slot), side)) {
                return ItemStack.EMPTY;
            }
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler != null ? itemHandler.getSlotLimit(slot) : getMaxStackSize();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return canPlaceItemThroughFace(slot, stack, side);
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (itemHandler != null) {
                itemHandler.setStackInSlot(slot, stack);
            }
        }
    }
}