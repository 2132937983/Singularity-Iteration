package com.miophas.singularity_iteration.core.prefab.blockentity;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotType;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.machine.ISlotLayout;
import com.miophas.singularity_iteration.core.api.machine.IWorkCompleteCallback;
import com.miophas.singularity_iteration.core.api.machine.builder.IMachineBuilderAPI;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Generic machine block entity (internal implementation) for the API builders.
 *
 * <p>This is a generic producer machine. Addon mods change its behavior through callbacks.
 *
 * <p><strong>This is an internal implementation class. Addons must not extend it directly.</strong>
 * Addon developers must implement their own BlockEntity and use the builder interfaces of IMachineBuilderAPI.
 *
 * <p>Example:
 * <pre>{@code
 * // Create the BlockEntityType during mod initialization
 * public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericMachineBlockEntity>> MY_MACHINE =
 *     BLOCK_ENTITIES.register("my_machine", () ->
 *         BlockEntityType.Builder.of(GenericMachineBlockEntity::new, ModBlocks.MY_MACHINE.get()).build(null));
 *
 * // Use the API builder to create the machine definition
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
 * // Set the work handler
 * GenericMachineBlockEntity entity = ...;
 * entity.setWorkHandler((machine, inputs) -> {
 *     // Find the recipe
 *     var recipe = findRecipe(inputs[0]);
 *     if (recipe != null) {
 *         // Use the input items
 *         machine.consumeInput(0, 1);
 *         // Insert the output items
 *         machine.insertOutput(recipe.getResult());
 *     }
 * });
 * }</pre>
 */
public class GenericMachineBlockEntity extends AbstractProcessingMachineBlockEntity
    implements com.miophas.singularity_iteration.core.api.machine.IMachineConfigurable {

    // Callbacks
    private BiConsumer<GenericMachineBlockEntity, ItemStack[]> workHandler;
    private Runnable tickCallback;
    private Runnable workStartCallback;
    private IWorkCompleteCallback workCompleteCallback;
    
    // Machine configuration (for API queries)
    @Nullable
    private IMachineBuilderAPI.MachineConfiguration configuration;
    
    // Work state of the previous tick. It is used to detect the start of work.
    private boolean wasWorkingLastTick = false;

    // Fluid tanks that the builder declares. The configuration is injected after construction, so the tanks are created lazily.
    @Nullable
    private List<net.neoforged.neoforge.fluids.capability.templates.FluidTank> builderTanks;

    /**
     * Simple constructor for NeoForge BlockEntityType.Builder.of().
     *
     * <p>Example:
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
     * Sets the machine configuration (the builder injects it).
     */
    public void setConfiguration(@Nullable IMachineBuilderAPI.MachineConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Returns the tanks that the builder declares with {@code addFluidTank}.
     *
     * <p>The tanks are created only when the builder really declares tanks. Thus a machine without declared fluid tanks still returns null,
     * and the fluid automation upgrade does not take control of it. Subclasses that supply their own fluid handler (and override this method) are not affected.
     */
    @Override
    protected net.neoforged.neoforge.fluids.capability.IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        List<net.neoforged.neoforge.fluids.capability.templates.FluidTank> tanks = ensureTanks();
        if (tanks.isEmpty()) {
            return null;
        }
        if (tanks.size() == 1) {
            return tanks.get(0);
        }
        var entries = new ArrayList<com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup.Tank>(tanks.size());
        for (var tank : tanks) {
            entries.add(new com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup.Tank(tank,
                com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup.Role.IO));
        }
        return new com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup(entries);
    }

    private List<net.neoforged.neoforge.fluids.capability.templates.FluidTank> ensureTanks() {
        if (builderTanks != null) {
            return builderTanks;
        }
        var config = configuration;
        int count = config == null ? 0 : Math.max(0, config.fluidTanks());
        int capacity = config == null ? 0 : config.tankCapacity();
        if (count <= 0 || capacity <= 0) {
            builderTanks = List.of();
            return builderTanks;
        }
        var tanks = new ArrayList<net.neoforged.neoforge.fluids.capability.templates.FluidTank>(count);
        for (int i = 0; i < count; i++) {
            tanks.add(new net.neoforged.neoforge.fluids.capability.templates.FluidTank(capacity) {
                @Override
                protected void onContentsChanged() {
                    setChanged();
                }
            });
        }
        builderTanks = List.copyOf(tanks);
        return builderTanks;
    }

    /** Each tank is saved under its own key. This prevents conflicts with keys that specific machines already use, such as {@code fluid}/{@code InputTank}. */
    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        List<net.neoforged.neoforge.fluids.capability.templates.FluidTank> tanks = ensureTanks();
        for (int i = 0; i < tanks.size(); i++) {
            var tankTag = new net.minecraft.nbt.CompoundTag();
            tanks.get(i).writeToNBT(registries, tankTag);
            tag.put(tankKey(i), tankTag);
        }
    }

    @Override
    public void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        List<net.neoforged.neoforge.fluids.capability.templates.FluidTank> tanks = ensureTanks();
        for (int i = 0; i < tanks.size(); i++) {
            String key = tankKey(i);
            if (tag.contains(key, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                tanks.get(i).readFromNBT(registries, tag.getCompound(key));
            }
        }
    }

    private static String tankKey(int index) {
        return "scex_generic_tank_" + index;
    }

    /**
     * Returns the machine configuration, or null if no configuration is set.
     */
    @Nullable
    public IMachineBuilderAPI.MachineConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Sets the work handler.
     *
     * @param handler the callback that receives (entity, inputs) and processes the recipe.
     *                inputs are copies of the input slot items. Use them only for recipe matching.
     *                To change the real items, call {@link #consumeInput(int, int)} and {@link #insertOutput(ItemStack)}.
     */
    public void setWorkHandler(BiConsumer<GenericMachineBlockEntity, ItemStack[]> handler) {
        this.workHandler = handler;
    }

    /**
     * Sets the callback for each tick (the builder injects it). It runs on the server side only.
     */
    public void setTickCallback(Runnable tickCallback) {
        this.tickCallback = tickCallback;
    }

    /**
     * Sets the callback for the start of work (the builder injects it).
     */
    public void setWorkStartCallback(Runnable workStartCallback) {
        this.workStartCallback = workStartCallback;
    }

    /**
     * Sets the callback for the end of work (the builder injects it).
     */
    public void setWorkCompleteCallback(IWorkCompleteCallback workCompleteCallback) {
        this.workCompleteCallback = workCompleteCallback;
    }

    /**
     * Removes the given number of items from an input slot.
     *
     * @param inputIndex the input slot index (0-based, relative to the first input slot)
     * @param amount the number of items to remove
     * @return the number of items that were removed (0 if the index is not valid)
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
     * Inserts an item into the output slots.
     *
     * @param stack the item to insert
     * @return the remaining items that were not inserted (empty if all items were inserted)
     */
    public ItemStack insertOutput(ItemStack stack) {
        return com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots.insertOutput(itemHandler, getOutputSlots(), stack);
    }

    /**
     * Returns the item handler (for more complex item operations).
     */
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // Default implementation: return the slots that match the slot type of each side.
        int inputCount = slotLayout.getCount(SlotType.INPUT);
        int outputCount = slotLayout.getCount(SlotType.OUTPUT);
        int batteryCount = slotLayout.getCount(SlotType.BATTERY);
        int upgradeCount = slotLayout.getCount(SlotType.UPGRADE);

        int inputStart = slotLayout.getStart(SlotType.INPUT);
        int outputStart = slotLayout.getStart(SlotType.OUTPUT);
        int batteryStart = slotLayout.getStart(SlotType.BATTERY);

        return switch (side) {
            case DOWN -> {
                // Bottom: output slots
                int[] slots = new int[outputCount];
                for (int i = 0; i < outputCount; i++) {
                    slots[i] = outputStart + i;
                }
                yield slots;
            }
            case UP -> {
                // Top: input slots + battery slots
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
                // Other sides: all slots except upgrade slots
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
        // Default: check whether an input slot contains an item.
        int inputStart = slotLayout.getStart(SlotType.INPUT);
        int inputCount = slotLayout.getCount(SlotType.INPUT);
        for (int i = 0; i < inputCount; i++) {
            if (!itemHandler.getStackInSlot(inputStart + i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Reject detached/replaced owners without loading any chunk. */
    protected final boolean scexLiveWorkOwner() {
        if (isRemoved() || !(level instanceof net.minecraft.server.level.ServerLevel world)
                || !world.getServer().isSameThread()
                || !world.shouldTickBlocksAt(net.minecraft.world.level.ChunkPos.asLong(worldPosition))) return false;
        var chunk = world.getChunkSource().getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        return chunk != null && chunk.getBlockEntity(worldPosition,
            net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK) == this;
    }

    @Override
    protected void doWork() {
        if (!scexLiveWorkOwner()) { stopWork(); return; }
        // First, make sure that the recipe is valid. This prevents energy use when no valid work is possible.
        if (!canWork() || !canProcessWithHandler() || !scexLiveWorkOwner()) {
            stopWork();
            return;
        }

        // Machine work is an internal debit, independent of the external output limit.
        long energyCost = getEffectiveEnergyPerTick();
        if (energyCost < 0) { stopWork(); return; }
        if (energyCost > 0) {
            var payment = getEnergyStorageInternal().scexReserveInternal(energyCost);
            if (payment == null) { stopWork(); return; }
            payment.commit();
        }

        // Increase the progress.
        int progressPerTick = getProgressPerTick();
        progress += progressPerTick;

        // Check whether the work is complete.
        if (progress >= maxProgress) {
            progress = 0;
            isWorking = true;
            onWorkComplete();
        } else {
            isWorking = true;
        }
    }

    /**
     * Checks whether the workHandler can process the current inputs.
     * This check does not use any items. The output check is only approximate.
     */
    private boolean canProcessWithHandler() {
        if (workHandler == null) return false;
        if (getLevel() == null || getLevel().isClientSide) return false;

        int inputStart = slotLayout.getStart(SlotType.INPUT);
        int inputCount = slotLayout.getCount(SlotType.INPUT);
        
        // Check whether an input item exists.
        boolean hasInput = false;
        for (int i = 0; i < inputCount; i++) {
            if (!itemHandler.getStackInSlot(inputStart + i).isEmpty()) {
                hasInput = true;
                break;
            }
        }
        if (!hasInput) return false;

        // Check whether an output slot has space (approximate check).
        int outputStart = slotLayout.getStart(SlotType.OUTPUT);
        int outputCount = slotLayout.getCount(SlotType.OUTPUT);
        for (int i = 0; i < outputCount; i++) {
            ItemStack outputStack = itemHandler.getStackInSlot(outputStart + i);
            if (outputStack.isEmpty() || outputStack.getCount() < outputStack.getMaxStackSize()) {
                return true; // Output space is available
            }
        }
        return false;
    }

    /**
     * Called when one work cycle is complete. Server side only.
     */
    protected void onWorkComplete() {
        if (!scexLiveWorkOwner()) { stopWork(); return; }
        if (getLevel() != null && !getLevel().isClientSide) {
            if (workCompleteCallback != null) {
                workCompleteCallback.onWorkComplete(getLevel(), getBlockPos(), ItemStack.EMPTY);
            }
        }
        if (workHandler == null || !scexLiveWorkOwner()) { stopWork(); return; }
        // Run the callback only on the server side. This prevents a client/server desync.
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
     * Returns the effective energy use for each tick (EU/tick), after the upgrade multiplier.
     */
    public long getEffectiveEnergyPerTick() {
        return (long) (energyPerTick * upgradeStats.getEnergyUsageMultiplier());
    }

    /**
     * Returns the progress added for each tick (minimum 1), after the upgrade multiplier.
     */
    public int getProgressPerTick() {
        return Math.max(1, (int) Math.ceil(1.0 / upgradeStats.getProcessTimeMultiplier()));
    }

    // ==================== IProducerBlock implementation ====================

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
            // Detect the start of work: the state changes from not working to working.
            if (isWorking && !wasWorkingLastTick && workStartCallback != null) {
                workStartCallback.run();
            }
            wasWorkingLastTick = isWorking;
            
            // Run the tick callback.
            if (tickCallback != null) {
                tickCallback.run();
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        
        // If this instance was made by the simple constructor, try to get the callbacks from the callback registry.
        // Get them from the registry only when a callback is not set yet. This prevents an overwrite of callbacks that are already set.
        if (getLevel() != null && !getLevel().isClientSide) {
            BlockEntityType<?> type = getType();
            if (type != null) {
                var callbacks = com.miophas.singularity_iteration.core.runtime.machine.builder.MachineBuilderAPIImpl.getCallbacks(type);
                if (callbacks != null) {
                    // Wrap the callbacks as Runnable and inject them (only when not set yet).
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
