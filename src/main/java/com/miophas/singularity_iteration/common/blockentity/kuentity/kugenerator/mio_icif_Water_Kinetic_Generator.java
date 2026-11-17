package com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractKineticGeneratorBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.api.item.IKineticRotor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;


@SuppressWarnings("null")
public class mio_icif_Water_Kinetic_Generator extends AbstractKineticGeneratorBlockEntity
        implements com.miophas.singularity_iteration.common.machine.ClientRunState {
    /** EN: 0.1.8.1: water-wheel loop sound; "IsGenerating" is synced. ZH: 0.1.8.1：水轮循环音效，“IsGenerating”已同步。JA: 0.1.8.1：水車音。「IsGenerating」は同期済み。 */
    @Override public boolean clientRunning() { return isGenerating; }

    public static final int SLOT_COUNT = 1;
    public static final int ROTOR_SLOT = 0;

    public static final int MAX_RPM = 10000;

    public static final int UPDATE_INTERVAL = 20;
    public static final int ROTOR_DAMAGE_INTERVAL = 10;

    @SuppressWarnings("null")
    public enum BiomeType {
        NONE("none", 0),
        RIVER("river", 100),
        OCEAN("ocean", 150);

        public final String name;
        public final int amplitudeBonus;

        BiomeType(String name, int amplitudeBonus) {
            this.name = name;
            this.amplitudeBonus = amplitudeBonus;
        }
    }

    private boolean isGenerating = false;
    private int currentKineticOutput = 0;
    private int ticksUntilUpdate = UPDATE_INTERVAL;
    /** 0.1.11.22: client only, the rotor's drawn angle and the time it was last drawn (kept per machine). */
    public float rotorAngle;
    public long rotorRenderTime;
    private int ticksUntilRotorDamage = ROTOR_DAMAGE_INTERVAL;
    private boolean isInValidBiome = false;
    private boolean isInNaturalWater = false;
    private BiomeType currentBiomeType = BiomeType.NONE;
    /** 0.1.11.18: the last scan of the rotor's disc, the current's source and speed, and the wake factor. */
    private WaterPhysics.Disc disc;
    private WaterPhysics.Source source = WaterPhysics.Source.NONE;
    private double biomeSpeed, wakeFactor = 1;
    private int meanOutput;
    private long lastVisualSyncTick = Long.MIN_VALUE;
    private ItemStack lastVisualRotor = ItemStack.EMPTY;
    private int lastVisualOutput;
    private boolean lastVisualGenerating;

    public mio_icif_Water_Kinetic_Generator(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }


    public mio_icif_Water_Kinetic_Generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type != null ? type : com.miophas.singularity_iteration.common.registry.mio_icif_block_entities.WATER_KINETIC_GENERATOR_ENTITY_TYPE.get(),
              pos, state, SlotLayout.builder().rotor().build(), 0, 0, MAX_RPM);
    }


    public ItemStack getRotorStack() {
        return itemHandler.getStackInSlot(ROTOR_SLOT);
    }

    public void setRotorStack(ItemStack stack) {
        itemHandler.setStackInSlot(ROTOR_SLOT, stack);
        setChanged();
    }


    @Nullable
    private IKineticRotor getRotor() {
        ItemStack stack = getRotorStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof IKineticRotor rotor)) {
            return null;
        }
        return rotor;
    }

 
    private void checkBiome() {
        // 0.1.11.22: the biome does not change; look once (with the source the disc uses)
        if (biomeChecked) return;
        if (level == null) {
            isInValidBiome = false;
            currentBiomeType = BiomeType.NONE;
            return;
        }

        String biomeName = level.getBiome(worldPosition).unwrapKey()
                .map(key -> key.location().toString()).orElse("");

        // 0.1.11.11: biome tags first ("deep" in the name also matched the Deep Dark); names only for untagged biomes
        var biome = level.getBiome(worldPosition);
        if (biome.is(net.minecraft.tags.BiomeTags.IS_RIVER) || biomeName.contains("river")) {
            isInValidBiome = true;
            currentBiomeType = BiomeType.RIVER;
        } else if (biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN) || biome.is(net.minecraft.tags.BiomeTags.IS_BEACH)
                || biomeName.contains("ocean") || biomeName.contains("beach")) {
            isInValidBiome = true;
            currentBiomeType = BiomeType.OCEAN;
        } else {
            isInValidBiome = false;
            currentBiomeType = BiomeType.NONE;
        }
        biomeSourceCached = WaterPhysics.biomeSource(level, worldPosition);
        biomeChecked = true;
    }

    private boolean biomeChecked;
    private WaterPhysics.Source biomeSourceCached = WaterPhysics.Source.NONE;


    private void checkNaturalWater() {
        if (level == null) {
            isInNaturalWater = false;
            return;
        }

        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos frontPos = worldPosition.relative(facing);

        FluidState frontFluidState = level.getFluidState(frontPos);
        if (!frontFluidState.is(Fluids.WATER) && !frontFluidState.is(Fluids.FLOWING_WATER)) {
            isInNaturalWater = false;
            return;
        }

        // 0.1.11.18: the block in front must be water; how much of the disc is water now sets the output (WaterPhysics)
        isInNaturalWater = true;
    }

    /**
     * 0.1.11.18: the power of the flow through the rotor's disc (WaterPhysics): a river's steady current, an ocean's tide
     * or the flowing water of a canal, over the cells of the disc that are water, less the wake of a rotor in front. The
     * block in front must be water. It was a four-peak cosine of the time of day in rivers and oceans, whatever the water.
     */
    private int calculateKineticOutput() {
        IKineticRotor rotor = getRotor();
        ItemStack rotorStack = getRotorStack();
        disc = null;
        wakeFactor = 1;
        meanOutput = 0;
        if (rotor == null || level == null) return 0;
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos front = worldPosition.relative(facing);
        checkBiome();
        WaterPhysics.Source biome = biomeSourceCached;
        biomeSpeed = WaterPhysics.biomeSpeed(level, worldPosition, biome);
        int diameter = Math.min(WaterPhysics.MAX_DIAMETER, rotor.getDiameter(rotorStack));
        disc = WaterPhysics.scan(level, front, facing, diameter, biomeSpeed);
        source = biome != WaterPhysics.Source.NONE ? biome : disc.cubes() > 0 ? WaterPhysics.Source.CANAL : WaterPhysics.Source.NONE;
        meanOutput = WaterPhysics.meanOutput(diameter, source);
        if (!isInNaturalWater) return 0;
        // the wake of a rotor in line in front of this one, facing the same way
        for (int k = 2; k <= WaterPhysics.WAKE_RANGE; k++) {
            BlockPos at = worldPosition.relative(facing, k);
            if (!level.isLoaded(at)) break;
            if (level.getBlockEntity(at) instanceof mio_icif_Water_Kinetic_Generator other && other.getRotor() != null
                    && other.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING) == facing) {
                wakeFactor = 1 - WaterPhysics.wakeDeficit(k, other.getRotor().getDiameter(other.getRotorStack()));
                break;
            }
        }
        double v3 = disc.power() * wakeFactor * wakeFactor * wakeFactor;
        return (int) Math.round(meanOutput * v3);
    }

    public WaterPhysics.Disc getDisc() { return disc; }
    public WaterPhysics.Source getSource() { return source; }
    public double getWakeFactor() { return wakeFactor; }
    public int getMeanOutput() { return meanOutput; }

    private void damageRotor() {
        ItemStack rotorStack = getRotorStack();
        if (rotorStack.isEmpty()) return;

        IKineticRotor rotor = getRotor();
        if (rotor == null) return;

        rotor.damageRotor(rotorStack, 1);

        if (rotorStack.isEmpty()) {
            itemHandler.setStackInSlot(ROTOR_SLOT, ItemStack.EMPTY);
        }

        setChanged();
    }

 
    private void generateAndOutputKineticToBack() {
        if (currentKineticOutput <= 0 || level == null) return;

        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        Direction backDirection = facing.getOpposite();
        BlockPos backPos = worldPosition.relative(backDirection);

        IMioIcifCapabilities.IKineticStorage backKinetic = com.miophas.singularity_iteration.core.runtime.NeighbourCaps.get(this, IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, backDirection);
        if (backKinetic == null) {
            BlockEntity be = level.getBlockEntity(backPos);
            backKinetic = MioIcifAPI.instance().getCapabilities().adaptKineticStorage(be);
        }

        if (backKinetic != null && backKinetic.canReceiveKinetic()) {
            long transferred = backKinetic.receiveKinetic(currentKineticOutput, false);
            if (transferred > 0) setChanged();
        }
    }


    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_Water_Kinetic_Generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        blockEntity.applyFrictionLoss();

        blockEntity.ticksUntilUpdate--;

        if (blockEntity.ticksUntilUpdate <= 0) {
            blockEntity.ticksUntilUpdate = UPDATE_INTERVAL;
            blockEntity.checkBiome();
            blockEntity.checkNaturalWater();
            blockEntity.currentKineticOutput = blockEntity.calculateKineticOutput();
        }

        blockEntity.ticksUntilRotorDamage--;
        if (blockEntity.ticksUntilRotorDamage <= 0) {
            blockEntity.ticksUntilRotorDamage = ROTOR_DAMAGE_INTERVAL;
            if (blockEntity.currentKineticOutput > 0) {
                blockEntity.damageRotor();
            }
        }

        boolean wasGenerating = blockEntity.isGenerating;
        if (blockEntity.currentKineticOutput > 0) {
            blockEntity.isGenerating = true;
            blockEntity.generateAndOutputKineticToBack();
        } else {
            blockEntity.isGenerating = false;
        }

        if (wasGenerating != blockEntity.isGenerating) {
            blockEntity.setChanged();
        }
        blockEntity.syncVisualState();
    }

    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        return 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("TicksUntilUpdate", ticksUntilUpdate);
        tag.putInt("TicksUntilRotorDamage", ticksUntilRotorDamage);
        tag.putInt("CurrentKineticOutput", currentKineticOutput);
        tag.putBoolean("IsGenerating", isGenerating);
        tag.putBoolean("IsInValidBiome", isInValidBiome);
        tag.putBoolean("IsInNaturalWater", isInNaturalWater);
        tag.putInt("BiomeType", currentBiomeType.ordinal());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ticksUntilUpdate = tag.getInt("TicksUntilUpdate");
        if (ticksUntilUpdate == 0) ticksUntilUpdate = UPDATE_INTERVAL;
        ticksUntilRotorDamage = tag.getInt("TicksUntilRotorDamage");
        if (ticksUntilRotorDamage == 0) ticksUntilRotorDamage = ROTOR_DAMAGE_INTERVAL;
        currentKineticOutput = tag.getInt("CurrentKineticOutput");
        isGenerating = tag.getBoolean("IsGenerating");
        isInValidBiome = tag.getBoolean("IsInValidBiome");
        isInNaturalWater = tag.getBoolean("IsInNaturalWater");
        int biomeTypeOrdinal = tag.getInt("BiomeType");
        if (biomeTypeOrdinal >= 0 && biomeTypeOrdinal < BiomeType.values().length) {
            currentBiomeType = BiomeType.values()[biomeTypeOrdinal];
        }
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        // Only the single rotor (including appearance components) and public
        // visual state belong on the wire. Persistence keeps its complete NBT.
        CompoundTag tag = new CompoundTag();
        tag.put("Items", itemHandler.serializeNBT(registries));
        tag.putInt("CurrentKineticOutput", currentKineticOutput);
        tag.putBoolean("IsGenerating", isGenerating);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        // The parent applies saved inventory/timers; a compact update must not.
        if (tag.contains("Items")) itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        if (tag.contains("CurrentKineticOutput")) currentKineticOutput = tag.getInt("CurrentKineticOutput");
        if (tag.contains("IsGenerating")) isGenerating = tag.getBoolean("IsGenerating");
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) handleUpdateTag(tag, registries);
    }

    /** Called once after the server tick; setChanged itself only marks saving dirty. */
    private void syncVisualState() {
        if (level == null || level.isClientSide()) return;
        long now = level.getGameTime();
        if (now == lastVisualSyncTick) return;
        ItemStack rotor = getRotorStack();
        boolean discrete = rotor.isEmpty() != lastVisualRotor.isEmpty()
            || rotor.getItem() != lastVisualRotor.getItem() || isGenerating != lastVisualGenerating;
        if (!discrete && lastVisualSyncTick != Long.MIN_VALUE && now - lastVisualSyncTick < UPDATE_INTERVAL) return;
        boolean changed = discrete || currentKineticOutput != lastVisualOutput
            || !ItemStack.matches(rotor, lastVisualRotor);
        if (!changed) return;
        lastVisualSyncTick = now;
        lastVisualRotor = rotor.copy();
        lastVisualOutput = currentKineticOutput;
        lastVisualGenerating = isGenerating;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.water_kinetic_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.generator.WaterKineticGeneratorMenu(containerId, playerInventory, this);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != ROTOR_SLOT) return false;
        if (!(stack.getItem() instanceof IKineticRotor rotor)) return false;
        return rotor.isWaterCompatible();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == ROTOR_SLOT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{ROTOR_SLOT};
    }

    public net.neoforged.neoforge.items.IItemHandler createItemHandler() {
        return new net.neoforged.neoforge.items.IItemHandlerModifiable() {
            @Override
            public int getSlots() { return itemHandler.getSlots(); }

            @Override
            public @org.jetbrains.annotations.NotNull ItemStack getStackInSlot(int slot) {
                return itemHandler.getStackInSlot(slot);
            }

            @Override
            public @org.jetbrains.annotations.NotNull ItemStack insertItem(int slot, @org.jetbrains.annotations.NotNull ItemStack stack, boolean simulate) {
                if (slot != ROTOR_SLOT || !(stack.getItem() instanceof IKineticRotor rotor)) return stack;
                if (!rotor.isWaterCompatible()) return stack;

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
            public @org.jetbrains.annotations.NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot != ROTOR_SLOT) return ItemStack.EMPTY;
                ItemStack existing = itemHandler.getStackInSlot(slot);
                if (existing.isEmpty()) return ItemStack.EMPTY;
                int toExtract = Math.min(amount, existing.getCount());
                ItemStack extracted = existing.copyWithCount(toExtract);
                if (!simulate) {
                    existing.shrink(toExtract);
                    if (existing.isEmpty()) itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
                    setChanged();
                }
                return extracted;
            }

            @Override
            public int getSlotLimit(int slot) { return getMaxStackSize(); }

            @Override
            public boolean isItemValid(int slot, @org.jetbrains.annotations.NotNull ItemStack stack) {
                if (slot != ROTOR_SLOT || !(stack.getItem() instanceof IKineticRotor rotor)) return false;
                return rotor.isWaterCompatible();
            }

            @Override
            public void setStackInSlot(int slot, @org.jetbrains.annotations.NotNull ItemStack stack) {
                itemHandler.setStackInSlot(slot, stack);
            }
        };
    }

    // ==================== Getters for the UI display ====================

    public int getCurrentKineticOutput() { return currentKineticOutput; }
    public boolean isGenerating() { return isGenerating; }

    @Override public int getKineticOutput() { return isGenerating ? currentKineticOutput : 0; }
    @Override public int getBurnTime() { return 0; }
    @Override public int getBurnDuration() { return 0; }
    @Override public int getKineticGenerationRate() { return currentKineticOutput; }
    @Override public int getRotorRPM() { return kineticStorage != null ? kineticStorage.getRPM() : 0; }
    public boolean isInValidBiome() { return isInValidBiome; }
    public boolean isInNaturalWater() { return isInNaturalWater; }
    public BiomeType getCurrentBiomeType() { return currentBiomeType; }

    public int getRotorDurability() {
        ItemStack rotorStack = getRotorStack();
        if (rotorStack.isEmpty()) return 0;
        IKineticRotor rotor = getRotor();
        if (rotor == null) return 0;
        return rotor.getDurability(rotorStack);
    }

    public int getRotorMaxDurability() {
        ItemStack rotorStack = getRotorStack();
        if (rotorStack.isEmpty()) return 0;
        IKineticRotor rotor = getRotor();
        if (rotor == null) return 0;
        return rotor.getMaxDurability();
    }

    @Nullable
    public IMioIcifCapabilities.IKineticStorage getKineticStorageCapability(@Nullable Direction side) {
        return kineticStorage;
    }
}
