package com.miophas.singularity_iteration.core.prefab.blockentity;

import com.miophas.singularity_iteration.core.api.machine.IBurnControl;
import com.miophas.singularity_iteration.core.api.machine.IHeatGeneratorBlock;

import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
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
 * Base class for HU heat generator block entities.
 * It copies AbstractGeneratorBlockEntity, but it uses the HU heat system.
 * 
 * Heat generator properties:
 * - It burns fuel to make heat (HU).
 * - Stored heat decreases over time (heat loss).
 * - It sends heat only to the block at its front (see {@link #distributeHeat()}).
 * - It burns fuel only when the heat storage is not full and a heat consumer is at the front.
 */
@SuppressWarnings("null")
public abstract class AbstractHeatGeneratorBlockEntity extends AbstractHeatBlockEntity implements WorldlyContainer, IBurnControl, IHeatGeneratorBlock {

    // Item handler. It uses the shared slot system.
// Uses the inherited inventory; no shadow copy.

    // Remaining burn time of the current fuel (ticks)
    public int burnTime = 0;

    // Total burn time of the current fuel item (ticks)
public int burnDuration = 0;

    @Override
    public int getDefaultBurnTime() {
        return burnDuration;
    }

    @Override
    public void setBurnTime(int ticks) {
        this.burnTime = Math.max(0, ticks);
        this.setChanged();
    }

    // Heat generation rate (HU/tick)
    protected final int heatGenerationRate;

    // Fuel slot index
protected static final int FUEL_SLOT = 0;

    /**
     * Constructor with the default heat settings
     * (capacity 20000 HU, maximum output 500 HU/t, maximum temperature 1500).
     * @param type the block entity type
     * @param pos the position
     * @param state the block state
     * @param layout the slot layout
     * @param heatGenerationRate the heat generation rate (HU/tick)
     */
    public AbstractHeatGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                       SlotLayout layout, int heatGenerationRate) {
        super(type, pos, state, 20000, 0, 500, 20, 1500, 0.02f);
        this.slotLayout = layout;
        this.itemHandler = createItemHandler(layout);
        this.heatGenerationRate = heatGenerationRate;
    }

    /**
     * Constructor with custom heat settings.
     * @param type the block entity type
     * @param pos the position
     * @param state the block state
     * @param layout the slot layout
     * @param heatGenerationRate the heat generation rate (HU/tick)
     * @param capacity the heat capacity
     * @param maxExtract the maximum output rate
     * @param maxTemp the maximum temperature
     */
    public AbstractHeatGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                       SlotLayout layout, int heatGenerationRate,
                               int capacity, int maxExtract, int maxTemp) {
        super(type, pos, state, capacity, 0, maxExtract, 20, maxTemp, 0.02f);
        this.slotLayout = layout;
        this.itemHandler = createItemHandler(layout);
        this.heatGenerationRate = heatGenerationRate;
    }

    protected MachineItemHandler createItemHandler(SlotLayout layout) {
        return new MachineItemHandler(layout) {
            @Override protected void onContentsChanged(int slot) {
                if (layout != null && layout.isType(slot, com.miophas.singularity_iteration.core.prefab.inventory.SlotType.UPGRADE)) {
                    AbstractHeatGeneratorBlockEntity.this.markUpgradeStatsDirty();
                }
                AbstractHeatGeneratorBlockEntity.this.setChanged();
            }
        };
    }
    
    /**
     * Checks if a machine at the front needs heat.
     * The front is the block that the FACING property points to.
     * Returns false if the front chunk is not loaded.
     */
    protected boolean hasHeatConsumerInFront() {
        if (level == null) return false;

        // Get the block facing
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos frontPos = worldPosition.relative(facing);
        if (!level.isLoaded(frontPos)) return false;

        // Get the heat storage of the front block
    IMioIcifCapabilities.IHeatStorage frontHeat = com.miophas.singularity_iteration.core.runtime.NeighbourCaps.get(this, IMioIcifCapabilities.HEAT_STORAGE_BLOCK, facing);

        // Check if the front machine can receive heat
    if (frontHeat != null && frontHeat.canReceiveHeat()) {
            // Check if the front machine still needs heat (it is not full)
        return frontHeat.getHeatStored() < frontHeat.getMaxHeatStored();
        }

        return false;
    }

    /**
     * Server tick logic. Does nothing on the client.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, AbstractHeatGeneratorBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        
        // Record the burn state before this tick
    boolean wasBurning = blockEntity.isBurning();
        
        // Check if the heat storage is full
    boolean isHeatFull = blockEntity.getHeatStorage().getHeatStored() >= 
                            blockEntity.getHeatStorage().getMaxHeatStored();
        
        // Check if a machine at the front needs heat
        boolean hasConsumer = blockEntity.hasHeatConsumerInFront();
        
        // Generate heat while the generator burns fuel.
    if (blockEntity.isBurning()) {
            // If the heat is full or no machine at the front needs heat, stop burning
        if (isHeatFull || !hasConsumer) {
                blockEntity.burnTime = 0;
            } else {
                blockEntity.generateHeat();
                blockEntity.burnTime--;
                
                // If the burn is complete, try to use new fuel
            if (blockEntity.burnTime <= 0) {
                    blockEntity.consumeFuel();
                }
            }
        } else {
            // If not burning, the heat is not full and a machine at the front needs heat, try to use new fuel
        if (!isHeatFull && hasConsumer) {
                blockEntity.consumeFuel();
            }
        }
        
        // Apply heat loss
    blockEntity.applyHeatLoss();
        
        // Transfer heat to adjacent blocks.
    blockEntity.distributeHeat();
        
        // Check if the burn state changed
    boolean isBurning = blockEntity.isBurning();
        if (wasBurning != isBurning) {
            // Update the block state (subclasses can set the ACTIVATE property)
            blockEntity.updateBlockState(isBurning);
        }
        
        // Mark the block entity as changed
    // 0.1.10.10 (D1): save mark only (setChanged() every tick also updated the six neighbours)
    com.miophas.singularity_iteration.core.runtime.TickMarks.saved(blockEntity);
    }
    
    /**
     * Generates heat. Adds up to heatGenerationRate HU, limited by the free storage space.
     */
    protected void generateHeat() {
        long heatGenerated = Math.min((long)heatGenerationRate, 
            getHeatStorage().getMaxHeatStored() - getHeatStorage().getHeatStored());
        getHeatStorage().generateHeatInternal(heatGenerated, false);
    }
    
    /**
     * Conduction by temperature difference is disabled, as in original IC2
     * (see the base class {@link AbstractHeatBlockEntity#distributeHeat()}).
     * The heat generator sends heat only to its front, with outputHeatToFront()/pushFront().
     */
    @Override
    protected void distributeHeat() {
        // intentionally empty (temperature-gradient conduction shelved)
    }

    
    /**
     * Checks if the generator is burning fuel.
 */
    public boolean isBurning() {
        return burnTime > 0;
    }
    
    /**
     * Uses one fuel item from the fuel slot and starts a new burn.
     * Does nothing if the heat storage is full or the fuel slot has no valid fuel.
     * Puts the crafting remainder (for example an empty bucket) in the slot when the stack becomes empty.
 */
    protected void consumeFuel() {
        // Check if the heat storage is full
    boolean isHeatFull = getHeatStorage().getHeatStored() >= 
                            getHeatStorage().getMaxHeatStored();
        
        // If the heat is full, do not use fuel
    if (isHeatFull) {
            return;
        }
        
        ItemStack fuelStack = itemHandler.getStackInSlot(FUEL_SLOT);
        if (!fuelStack.isEmpty()) {
            int burnTime = getFuelBurnTime(fuelStack);
            if (burnTime > 0) {
                this.burnDuration = burnTime;
                this.burnTime = burnTime;

                // Read the container item before shrink: an empty stack always returns EMPTY,
                // so a lava bucket lost its bucket before 0.1.7.36.
                ItemStack remainder = fuelStack.getCraftingRemainingItem();
                fuelStack.shrink(1);

                // When the fuel stack is empty, put the container item in the slot.
                if (fuelStack.isEmpty()) {
                    itemHandler.setStackInSlot(FUEL_SLOT, remainder);
                }
            }
        }
    }
    
    /**
     * Gets the burn time of a fuel. Subclasses implement this method to define the valid fuels.
     * @param fuel the fuel item
     * @return the burn time (ticks); 0 or less means the item is not a fuel
     */
    public abstract int getFuelBurnTime(ItemStack fuel);
    
    /**
     * Gets the burn progress for the GUI.
     * @return the burn progress (0 to 1); 0 if there is no burn duration
     */
    public float getBurnProgress() {
        if (burnDuration == 0) {
            return 0;
        }
        return (float) burnTime / burnDuration;
    }
    
    /**
     * Updates the block state (burning or not burning).
     * Subclasses can override this method to update the block look.
     */
    protected void updateBlockState(boolean isBurning) {
        // Default: does nothing. Subclasses can override this to set the ACTIVATE property.
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
    
    // ==================== Container implementation ====================

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
        return isValidItemSlot(slot) ? itemHandler.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (amount <= 0 || slot < 0 || slot >= itemHandler.getSlots()) return ItemStack.EMPTY;
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int toRemove = Math.min(amount, stack.getCount());
        ItemStack result = stack.copyWithCount(toRemove);
        stack.shrink(toRemove);
        if (stack.isEmpty()) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }
        setChanged();
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
        return level != null && level.getBlockEntity(worldPosition) == this &&
               player.distanceToSqr(worldPosition.getX() + 0.5,
                                   worldPosition.getY() + 0.5,
                                   worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    // ==================== WorldlyContainer implementation ====================

    @Override
    public int[] getSlotsForFace(Direction side) {
        // All sides can access the fuel slot.
    return new int[]{FUEL_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        // Fuel slot: accepts only fuel
    if (slot == FUEL_SLOT) {
            return getFuelBurnTime(stack) > 0;
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // No slot allows extraction (this stops hoppers from removing fuel)
    return false;
    }
    
    /**
     * Gets an item handler for the capability system.
     * @param side the side, or null for internal access
     * @return a new item handler for this side
     */
    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return new GeneratorItemHandler(side);
    }

    // ==================== IHeatGeneratorBlock implementation ====================

    @Override
    public int getHeatOutput() {
        return isBurning() ? heatGenerationRate : 0;
    }

    @Override
    public boolean isGenerating() {
        return isBurning();
    }

    @Override
    public int getBurnTime() {
        return burnTime;
    }

    @Override
    public int getBurnDuration() {
        return burnDuration;
    }

    @Override
    public int getHeatGenerationRate() {
        return heatGenerationRate;
    }
    
    // ==================== GeneratorItemHandler inner class====================
    
    /**
     * Item handler of the heat generator.
     * The capability system uses it. It limits item insertion and extraction.
 */
    protected class GeneratorItemHandler implements IItemHandlerModifiable {
        private final Direction side;

        public GeneratorItemHandler(@Nullable Direction side) {
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
            if (!canPlaceItemThroughFace(slot, stack, side)) {
                return stack;
            }

            ItemStack currentStack = itemHandler.getStackInSlot(slot);
            if (currentStack.isEmpty()) {
                // The slot is empty; the item can go in
            int count = Math.min(stack.getCount(), getMaxStackSize());
                if (!simulate) {
                    itemHandler.setStackInSlot(slot, stack.copyWithCount(count));
                    setChanged();
                }
                if (count < stack.getCount()) {
                    return stack.copyWithCount(stack.getCount() - count);
                }
                return ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(currentStack, stack)) {
                // The slot has the same item; the stacks can merge
                int space = getMaxStackSize() - currentStack.getCount();
                int toAdd = Math.min(stack.getCount(), space);
                if (toAdd > 0) {
                    if (!simulate) {
                        currentStack.grow(toAdd);
                        setChanged();
                    }
                    if (toAdd < stack.getCount()) {
                        return stack.copyWithCount(stack.getCount() - toAdd);
                    }
                    return ItemStack.EMPTY;
                }
            }
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!canTakeItemThroughFace(slot, itemHandler.getStackInSlot(slot), side)) {
                return ItemStack.EMPTY;
            }

            ItemStack currentStack = itemHandler.getStackInSlot(slot);
            if (currentStack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            int toExtract = Math.min(amount, currentStack.getCount());
            ItemStack extracted = currentStack.copyWithCount(toExtract);

            if (!simulate) {
                currentStack.shrink(toExtract);
                if (currentStack.isEmpty()) {
                    itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
                }
                setChanged();
            }

            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return getMaxStackSize();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return canPlaceItemThroughFace(slot, stack, side);
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            itemHandler.setStackInSlot(slot, stack);
            setChanged();
        }
    }
}
