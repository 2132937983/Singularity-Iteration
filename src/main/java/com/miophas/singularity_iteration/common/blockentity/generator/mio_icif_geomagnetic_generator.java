package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.block.generator.mio_icif_Block_Geomagnetic_Generator;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.IEnergyStorageAccess;
import com.miophas.singularity_iteration.core.prefab.blockentity.GenericGeneratorBlockEntity;
import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import com.miophas.singularity_iteration.core.api.registry.IMioIcifRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.registries.Registries;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_geomagnetic_generator extends GenericGeneratorBlockEntity {

    public static final int SLOT_COUNT = 1;
    public static final int BATTERY_SLOT = 0;

    public static final long ENERGY_GENERATION_RATE = 30720L;
    public static final long ENERGY_CAPACITY = 400000000L;
    public static final long MAX_RECEIVE = 0L;
    public static final long MAX_EXTRACT = 400000000L;

    private static final ResourceLocation ANTENNA_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "generator/block_geomagnetic_antenna");
    private static final ResourceLocation PEDESTAL_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "generator/block_geomagnetic_pedestal");
    private static final ResourceLocation ENTITY_TYPE_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "geomagnetic_generator");

    private int currentTick = 0;
    private float lastSourceValue = 0.0f;
    private boolean shouldMachineWork = false;

    // ------------------------------------------------------------------ 0.1.10.7: cached world checks
    /** Ticks between structure checks when no antenna or pedestal reported a change. */
    public static final int STRUCTURE_RECHECK = 40;
    /** Ticks between biome checks (the biome of a position changes only by commands). */
    public static final int BIOME_RECHECK = 600;
    /** Ticks between field (blocks below) checks, as before (it was also checked every tick while the value was 0). */
    public static final int SOURCE_RECHECK = 15;
    private static volatile net.minecraft.world.level.block.Block antennaBlock, pedestalBlock;
    /** True when the structure must be checked at the next tick (placement, load, a structure block changed). */
    private boolean structureDirty = true;
    private boolean sourceValid = false;
    private float biomeMultiplier = 1.0f;
    private long biomeCheckedAt = Long.MIN_VALUE;
    /** Phase of the periodic checks, so that many generators do not check in the same tick. */
    private int phase = -1;

    public mio_icif_geomagnetic_generator(BlockPos pos, BlockState state) {
        this(pos, state, resolveEntityType());
    }

    public mio_icif_geomagnetic_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            SlotLayout.builder().battery().build(), ENERGY_GENERATION_RATE, ENERGY_CAPACITY, MAX_RECEIVE, MAX_EXTRACT,
            MioIcifAPI.instance().getEnergyNetAPI().getCableTier("zpmv"));
    }

    private static BlockEntityType<?> resolveEntityType() {
        BlockEntityType<?> type = MioIcifAPI.instance().getRegistries().getBlockEntityType(ENTITY_TYPE_ID);
        return type != null ? type : BlockEntityType.Builder.of(mio_icif_geomagnetic_generator::new, net.minecraft.world.level.block.Blocks.AIR).build(null);
    }

    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        return 0;
    }

    private boolean checkStructureCompleted() {
        if (level == null) return false;

        // 0.1.10.7: the two blocks are looked up once (was: two registry lookups every tick)
        var antennaBlock = mio_icif_geomagnetic_generator.antennaBlock;
        var pedestalBlock = mio_icif_geomagnetic_generator.pedestalBlock;
        if (antennaBlock == null || pedestalBlock == null) {
            IMioIcifRegistries registries = MioIcifAPI.instance().getRegistries();
            mio_icif_geomagnetic_generator.antennaBlock = antennaBlock = registries.getBlock(ANTENNA_ID);
            mio_icif_geomagnetic_generator.pedestalBlock = pedestalBlock = registries.getBlock(PEDESTAL_ID);
        }

        if (antennaBlock == null || pedestalBlock == null) return false;

        BlockState topestAntenna = level.getBlockState(getBlockPos().above(2));
        BlockState midAntenna = level.getBlockState(getBlockPos().above(1));
        BlockState ABase = level.getBlockState(getBlockPos().offset(1, -1, 0));
        BlockState BBase = level.getBlockState(getBlockPos().offset(-1, -1, 0));
        BlockState CBase = level.getBlockState(getBlockPos().offset(0, -1, 1));
        BlockState DBase = level.getBlockState(getBlockPos().offset(0, -1, -1));

        if (topestAntenna.getBlock() == antennaBlock
            && midAntenna.getBlock() == antennaBlock) {
            if (ABase.getBlock() == pedestalBlock
                && BBase.getBlock() == pedestalBlock
                && CBase.getBlock() == pedestalBlock
                && DBase.getBlock() == pedestalBlock) {
                return true;
            }
        }
        return false;
    }

    private float getMagneticSource() {
        // 0.1.10.7: a field of 0 is cached as well (before, a generator with no field read up to
        // 20 blocks below it every tick)
        if (sourceValid && currentTick % SOURCE_RECHECK != 0) {
            return lastSourceValue;
        }
        float source = 1.0f;
        int seaLevelDelta = getBlockPos().getY() - level.getSeaLevel();
        if (seaLevelDelta < 0) {
            source = (float) getBlockPos().getY() / (float) (level.getSeaLevel() + 1);
        }

        int baseDelta = (getBlockPos().getY() >= 20) ? 20 : getBlockPos().getY();
        int airBlockCount = 0;
        for (int i = 0; i < baseDelta; i++) {
            BlockState tempState = level.getBlockState(getBlockPos().below(i));
            if (tempState.isAir() || tempState.getBlock() == Blocks.WATER) {
                airBlockCount++;
            }
        }
        if (baseDelta != 0) {
            source *= (baseDelta - airBlockCount) / (float) baseDelta;
        } else {
            source = 0;
        }
        lastSourceValue = source;
        sourceValid = true;
        return source;
    }

    /** 0.1.10.7: the biome multiplier, worked out on the first tick and every {@link #BIOME_RECHECK} ticks (was: every tick, with a new string). */
    private float cachedBiomeMultiplier() {
        long now = level.getGameTime();
        if (biomeCheckedAt == Long.MIN_VALUE || now - biomeCheckedAt >= BIOME_RECHECK) {
            biomeMultiplier = getBiomeMultiplier();
            biomeCheckedAt = now;
        }
        return biomeMultiplier;
    }

    private float getBiomeMultiplier() {
        if (level == null) return 1.0f;
        Biome biome = level.getBiome(getBlockPos()).value();
        var biomeKey = level.registryAccess().registryOrThrow(Registries.BIOME).getKey(biome);
        if (biomeKey == null) return 1.0f;
        String keyStr = biomeKey.toString();
        if (keyStr.contains("nether") || keyStr.contains("hell")) {
            return 1.2f;
        } else if (keyStr.contains("mountain") || keyStr.contains("hill") || keyStr.contains("peaks")) {
            return 1.1f;
        }
        return 1.0f;
    }

    @Override
    protected void consumeFuel() {
    }

    @Override
    protected void generateEnergy() {
    }

    @Override
    protected void chargeItems() {
        ItemStack chargeStack = itemHandler.getStackInSlot(BATTERY_SLOT);
        if (chargeStack.isEmpty()) return;

        IItemAPI itemAPI = MioIcifAPI.instance().getItemAPI();
        if (itemAPI.isBattery(chargeStack)) {
            long currentEnergy = itemAPI.getBatteryStored(chargeStack);
            long batteryMaxEnergy = itemAPI.getBatteryCapacity(chargeStack);
            long batteryChargeRate = itemAPI.getChargeRate(chargeStack);

            if (currentEnergy >= batteryMaxEnergy) return;

            IEnergyStorageAccess storage = getEnergyStorage();
            long availableEnergy = storage.getAmount();
            if (availableEnergy <= 0) return;

            long energyToCharge = Math.min(batteryChargeRate, batteryMaxEnergy - currentEnergy);
            energyToCharge = Math.min(energyToCharge, availableEnergy);

            long energyExtracted = storage.useEnergy(energyToCharge, false);
            itemAPI.chargeBattery(chargeStack, energyExtracted, false);
            setChanged();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_geomagnetic_generator blockEntity) {
        if (level.isClientSide()) return;

        boolean wasWorking = blockEntity.shouldMachineWork;
        if (blockEntity.phase < 0) blockEntity.phase = Math.floorMod(pos.hashCode(), STRUCTURE_RECHECK);
        long energyBefore = blockEntity.getEnergyStorage().getAmount();

        // 0.1.10.7: the structure is checked when an antenna or pedestal changed next to it, after
        // loading, and every STRUCTURE_RECHECK ticks (was: every tick)
        if (blockEntity.structureDirty || (level.getGameTime() + blockEntity.phase) % STRUCTURE_RECHECK == 0) {
            blockEntity.structureDirty = false;
            blockEntity.shouldMachineWork = blockEntity.checkStructureCompleted();
        }

        blockEntity.chargeItems();

        if (blockEntity.shouldDirectlyDistributeEnergy()) {
            blockEntity.distributeEnergy();
        } else {
            blockEntity.distributeEnergyToCompatSinks();
        }

        IEnergyStorageAccess storage = blockEntity.getEnergyStorage();
        boolean isEnergyFull = storage.getAmount() >= storage.getCapacity();

        if (blockEntity.shouldMachineWork && !isEnergyFull) {
            float ratio = blockEntity.getMagneticSource();
            ratio *= blockEntity.cachedBiomeMultiplier();

            if ((int)(ratio * 6) > 0) {
                long scaledGeneration = (long)(ENERGY_GENERATION_RATE * ratio);
                scaledGeneration = com.miophas.singularity_iteration.common.energy.GeneratorOutput.eu(blockEntity, Math.max(1, scaledGeneration));   // 0.1.8.4 output multiplier
                long energyToGenerate = Math.min(scaledGeneration, storage.getCapacity() - storage.getAmount());
                if (energyToGenerate > 0) {
                    storage.generateEnergy(energyToGenerate, false);
                }
                blockEntity.burnTime = 1;
                blockEntity.burnDuration = 1;
            }
        }

        if (blockEntity.currentTick + 1 < Integer.MAX_VALUE) {
            blockEntity.currentTick++;
        } else {
            blockEntity.currentTick = 0;
        }

        boolean isWorking = blockEntity.shouldMachineWork;
        if (wasWorking != isWorking) {
            BlockState newState = level.getBlockState(pos);
            if (newState.hasProperty(mio_icif_Block_Geomagnetic_Generator.ACTIVE)) {
                newState = newState.setValue(mio_icif_Block_Geomagnetic_Generator.ACTIVE, isWorking);
                level.setBlock(pos, newState, 3);
            }
            blockEntity.setChanged();
        } else if (blockEntity.getEnergyStorage().getAmount() != energyBefore) {
            // 0.1.10.7: only the stored energy changed: the chunk must be saved, but no neighbour or
            // comparator update is needed (was: setChanged() every tick, also when nothing changed)
            com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank.markUnsaved(blockEntity);
        }
    }

    /**
     * 0.1.10.7: an antenna or pedestal at {@code changed} was placed or removed: the generators
     * whose structure can include it check their structure at their next tick.
     */
    public static void structureChanged(Level level, BlockPos changed) {
        if (level.isClientSide()) return;
        BlockPos[] owners = {changed.below(1), changed.below(2),   // antennas: one and two blocks above the generator
            changed.offset(-1, 1, 0), changed.offset(1, 1, 0), changed.offset(0, 1, -1), changed.offset(0, 1, 1)};   // pedestals
        for (BlockPos owner : owners) {
            if (level.isLoaded(owner) && level.getBlockEntity(owner) instanceof mio_icif_geomagnetic_generator generator) {
                generator.structureDirty = true;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (itemHandler != null) {
            tag.put("Items", itemHandler.serializeNBT(registries));
        }
        tag.putInt("CurrentTick", currentTick);
        tag.putFloat("LastSourceValue", lastSourceValue);
        tag.putBoolean("ShouldMachineWork", shouldMachineWork);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        structureDirty = true;
        sourceValid = false;
        biomeCheckedAt = Long.MIN_VALUE;
        if (itemHandler != null && tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
        currentTick = tag.getInt("CurrentTick");
        lastSourceValue = tag.getFloat("LastSourceValue");
        shouldMachineWork = tag.getBoolean("ShouldMachineWork");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (itemHandler != null) {
            tag.put("Items", itemHandler.serializeNBT(registries));
        }
        tag.putBoolean("ShouldMachineWork", shouldMachineWork);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (itemHandler != null && tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
        shouldMachineWork = tag.getBoolean("ShouldMachineWork");
    }

    @Override
    public int getContainerSize() {
        return itemHandler != null ? itemHandler.getSlots() : 0;
    }

    @Override
    public boolean isEmpty() {
        if (itemHandler == null) return true;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return itemHandler != null ? itemHandler.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (itemHandler == null) return ItemStack.EMPTY;
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (itemHandler == null) return ItemStack.EMPTY;
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        setChanged();
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (itemHandler != null) {
            itemHandler.setStackInSlot(slot, stack);
            setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (player == null || level == null || isRemoved()) return false;
        return level.getBlockEntity(getBlockPos()) == this
            && player.distanceToSqr(worldPosition.getX() + 0.5,
                                   worldPosition.getY() + 0.5,
                                   worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{BATTERY_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return slot == BATTERY_SLOT && MioIcifAPI.instance().getItemAPI().isBattery(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == BATTERY_SLOT;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.geomagnetic_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.generator.GeomagneticGeneratorMenu(containerId, inventory, this);
    }

    public boolean isStructureComplete() {
        return shouldMachineWork;
    }
}