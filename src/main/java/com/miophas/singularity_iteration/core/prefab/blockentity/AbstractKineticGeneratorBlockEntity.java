package com.miophas.singularity_iteration.core.prefab.blockentity;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.api.machine.IBurnControl;
import com.miophas.singularity_iteration.core.api.machine.IKineticGeneratorBlock;

import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
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
 * Base block entity for KU kinetic generators.
 * It works like AbstractHeatGeneratorBlockEntity, but it uses the KU kinetic system.
 * 
 * Properties of a kinetic generator:
 * - It burns fuel or uses another energy source to make kinetic energy (KU).
 * - Kinetic energy has friction loss.
 * - It can send kinetic energy to adjacent blocks.
 * - A transfer occurs only when the source RPM is higher than the target RPM.
 * - It needs a mechanical transmission to send kinetic energy.
 */
@SuppressWarnings("null")
public abstract class AbstractKineticGeneratorBlockEntity extends AbstractKineticBlockEntity implements WorldlyContainer, IBurnControl, IKineticGeneratorBlock {

    // Item inventory handler
    protected MachineItemHandler itemHandler;

    // Remaining burn time (ticks)
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

    // Kinetic generation rate (KU/tick)
    protected final int kineticGenerationRate;

    // Index of the fuel slot
    protected static final int FUEL_SLOT = 0;

    /**
     * Constructor.
     * @param type the block entity type
     * @param pos the position
     * @param state the block state
     * @param layout the slot layout
     * @param kineticGenerationRate the kinetic generation rate (KU/tick)
     */
    public AbstractKineticGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                       SlotLayout layout, int kineticGenerationRate) {
 // The generator does not store kinetic energy. The capacity is 0 and the maximum output is the generation rate.
        super(type, pos, state, 0, 0, kineticGenerationRate, 10000, 0.0f);
        this.itemHandler = createItemHandler(layout);
        this.kineticGenerationRate = kineticGenerationRate;
    }

    /**
     * Constructor with custom kinetic parameters.
     * @param type the block entity type
     * @param pos the position
     * @param state the block state
     * @param layout the slot layout
     * @param kineticGenerationRate the kinetic generation rate (KU/tick)
     * @param maxExtract the maximum output rate (KU/tick)
     * @param maxRPM the maximum RPM
     */
    public AbstractKineticGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                       SlotLayout layout, int kineticGenerationRate,
                                       int maxExtract, int maxRPM) {
 // A block that makes kinetic energy does not store it. The capacity is 0.
        super(type, pos, state, 0, 0, maxExtract, maxRPM, 0.0f);
        this.itemHandler = createItemHandler(layout);
        this.kineticGenerationRate = kineticGenerationRate;
    }

    protected MachineItemHandler createItemHandler(SlotLayout layout) {
        return new MachineItemHandler(layout) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }
    
    /**
     * Checks if the block in front (on the FACING side) can receive kinetic energy and is not full.
     */
    protected boolean hasKineticConsumerInFront() {
        if (level == null) return false;

        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos frontPos = worldPosition.relative(facing);
        if (!level.isLoaded(frontPos)) return false;

        IMioIcifCapabilities.IKineticStorage frontKinetic = com.miophas.singularity_iteration.core.runtime.NeighbourCaps.get(this, IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, facing);
        if (frontKinetic == null) {
            frontKinetic = MioIcifAPI.instance().getCapabilities().adaptKineticStorage(
                level.getBlockEntity(frontPos));
        }

        if (frontKinetic != null && frontKinetic.canReceiveKinetic()) {
            return frontKinetic.getKineticStored() < frontKinetic.getMaxKineticStored();
        }

        return false;
    }
    
    /**
     * Server tick logic. It does nothing on the client side.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, AbstractKineticGeneratorBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // Record the burn state before this tick
        boolean wasBurning = blockEntity.isBurning();

        // Check if a machine in front needs kinetic energy
        boolean hasConsumer = blockEntity.hasKineticConsumerInFront();

        // If the generator burns, make and send kinetic energy
        if (blockEntity.isBurning()) {
            // If no machine in front needs kinetic energy, stop the burn
            if (!hasConsumer) {
                blockEntity.burnTime = 0;
            } else {
                // Send kinetic energy directly to the adjacent blocks
                blockEntity.outputKineticDirectly();
                blockEntity.burnTime--;

                // If the burn is complete, try to consume new fuel
                if (blockEntity.burnTime <= 0) {
                    blockEntity.consumeFuel();
                }
            }
        } else {
            // If the generator does not burn and a machine in front needs kinetic energy, try to consume new fuel
            if (hasConsumer) {
                blockEntity.consumeFuel();
            }
        }

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
     * Sends kinetic energy directly to adjacent blocks.
     * The generator has no storage, so it sends the energy directly.
     * The front block gets energy first, then the other sides.
     * A target gets energy only when its RPM is lower than the RPM of this generator.
     */
    protected void outputKineticDirectly() {
        if (level == null) return;

        long remainingKinetic = kineticGenerationRate;
        int myRPM = kineticStorage.getRPM();

        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos frontPos = worldPosition.relative(facing);
        if (!level.isLoaded(frontPos)) return;
        IMioIcifCapabilities.IKineticStorage frontKinetic = com.miophas.singularity_iteration.core.runtime.NeighbourCaps.get(this, IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, facing);
        if (frontKinetic == null) {
            frontKinetic = MioIcifAPI.instance().getCapabilities().adaptKineticStorage(
                level.getBlockEntity(frontPos));
        }

        if (frontKinetic != null && frontKinetic.canReceiveKinetic()) {
            int adjacentRPM = frontKinetic.getRPM();
            if (myRPM > adjacentRPM) {
                long maxReceive = frontKinetic.getMaxKineticStored() - frontKinetic.getKineticStored();
                long kineticToTransfer = Math.min(remainingKinetic, maxReceive);
                if (kineticToTransfer > 0) {
                    long received = frontKinetic.receiveKinetic(kineticToTransfer, false);
                    remainingKinetic -= received;
                }
            }
        }

        if (remainingKinetic > 0) {
            for (Direction direction : Direction.values()) {
                if (direction == facing) continue;

                BlockPos adjacentPos = worldPosition.relative(direction);
                if (!level.isLoaded(adjacentPos)) continue;
                IMioIcifCapabilities.IKineticStorage adjacentKinetic = com.miophas.singularity_iteration.core.runtime.NeighbourCaps.get(this, IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, direction);
                if (adjacentKinetic == null) {
                    adjacentKinetic = MioIcifAPI.instance().getCapabilities().adaptKineticStorage(
                        level.getBlockEntity(adjacentPos));
                }

                if (adjacentKinetic != null && adjacentKinetic.canReceiveKinetic()) {
                    int adjacentRPM = adjacentKinetic.getRPM();
                    if (myRPM > adjacentRPM) {
                        long maxReceive = adjacentKinetic.getMaxKineticStored() - adjacentKinetic.getKineticStored();
                        long kineticToTransfer = Math.min(remainingKinetic, maxReceive);
                        if (kineticToTransfer > 0) {
                            long received = adjacentKinetic.receiveKinetic(kineticToTransfer, false);
                            remainingKinetic -= received;
                            if (remainingKinetic <= 0) break;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Checks if the generator burns fuel now.
     */
    public boolean isBurning() {
        return burnTime > 0;
    }
    
    /**
     * Consumes one fuel item from the fuel slot and starts a new burn.
     */
    protected void consumeFuel() {
 // The generator has no storage. It can consume fuel when a consumer is present.
        ItemStack fuelStack = itemHandler.getStackInSlot(FUEL_SLOT);
        if (!fuelStack.isEmpty()) {
            int burnTime = getFuelBurnTime(fuelStack);
            if (burnTime > 0) {
                this.burnDuration = burnTime;
                this.burnTime = burnTime;

                // Read the container item (for example the empty bucket of a lava bucket)
                // before shrink: an empty stack always returns EMPTY (fixed in 0.1.7.36).
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
     * Gets the burn time of a fuel item.
     * Subclasses implement this method to define the valid fuels.
     * @param fuel the fuel item
     * @return the burn time (ticks). 0 or less means the item is not a valid fuel.
     */
    public abstract int getFuelBurnTime(ItemStack fuel);
    
    /**
     * Gets the burn progress for the GUI.
     * @return the remaining burn fraction (0 to 1)
     */
    public float getBurnProgress() {
        if (burnDuration == 0) {
            return 0;
        }
        return (float) burnTime / burnDuration;
    }
    
    /**
     * Updates the block state (burning or not burning).
     * Subclasses can override this method to update the block appearance.
     */
    protected void updateBlockState(boolean isBurning) {
        // Default implementation does nothing. Subclasses can override it to set the ACTIVATE property.
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

    public MachineItemHandler getItemHandler() {
        return itemHandler;
    }

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
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int toRemove = Math.min(amount, stack.getCount());
        ItemStack result = stack.copyWithCount(toRemove);
        if (toRemove >= stack.getCount()) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        } else {
            itemHandler.setStackInSlot(slot, stack.copyWithCount(stack.getCount() - toRemove));
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
        // All sides can access the fuel slot
        return new int[]{FUEL_SLOT};
    }
    
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        // Fuel slot: accept only fuel
        if (slot == FUEL_SLOT) {
            return getFuelBurnTime(stack) > 0;
        }
        return false;
    }
    
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // Do not allow extraction from the fuel slot (this stops hoppers from removing fuel)
        return false;
    }
    
    /**
     * Creates an item handler for automation mods.
     * It accepts only valid fuel in the fuel slot and does not allow extraction.
     */
    public IItemHandler createItemHandler() {
        return new IItemHandlerModifiable() {
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
                if (slot != FUEL_SLOT || getFuelBurnTime(stack) <= 0) {
                    return stack;
                }

                ItemStack existing = itemHandler.getStackInSlot(slot);
                if (existing.isEmpty()) {
                    int limit = Math.min(stack.getCount(), getMaxStackSize());
                    if (!simulate) {
                        itemHandler.setStackInSlot(slot, stack.copyWithCount(limit));
                        setChanged();
                    }
                    return stack.getCount() > limit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                    int limit = Math.min(existing.getCount() + stack.getCount(), getMaxStackSize());
                    int added = limit - existing.getCount();
                    if (!simulate && added > 0) {
                        existing.grow(added);
                        setChanged();
                    }
                    return stack.getCount() > added ? stack.copyWithCount(stack.getCount() - added) : ItemStack.EMPTY;
                }
                return stack;
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                // Do not allow fuel extraction
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return getMaxStackSize();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return slot == FUEL_SLOT && getFuelBurnTime(stack) > 0;
            }

            @Override
            public void setStackInSlot(int slot, @NotNull ItemStack stack) {
                itemHandler.setStackInSlot(slot, stack);
            }
        };
    }

    // ==================== IKineticGeneratorBlock implementation ====================

    @Override
    public int getKineticOutput() {
        return isBurning() ? kineticGenerationRate : 0;
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
    public int getKineticGenerationRate() {
        return kineticGenerationRate;
    }

    @Override
    public int getRotorRPM() {
        return kineticStorage != null ? kineticStorage.getRPM() : 0;
    }
}
