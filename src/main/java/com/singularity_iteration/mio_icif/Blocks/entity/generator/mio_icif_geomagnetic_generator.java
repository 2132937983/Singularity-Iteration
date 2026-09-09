package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Geomagnetic_Generator;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.IEnergyStorageAccess;
import com.singularity_iteration.mio_icif.api.internal.energy.GenericGeneratorBlockEntity;
import com.singularity_iteration.mio_icif.api.item.IItemAPI;
import com.singularity_iteration.mio_icif.api.registry.IMioIcifRegistries;
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

        IMioIcifRegistries registries = MioIcifAPI.instance().getRegistries();
        var antennaBlock = registries.getBlock(ANTENNA_ID);
        var pedestalBlock = registries.getBlock(PEDESTAL_ID);

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
        if (lastSourceValue != 0 && currentTick % 15 != 0) {
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
        return source;
    }

    private boolean isBiomeBoosted() {
        if (level == null) return false;
        Biome biome = level.getBiome(getBlockPos()).value();
        return !biome.warmEnoughToRain(BlockPos.ZERO)
            || biome.getBaseTemperature() > 1.0f;
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

        blockEntity.shouldMachineWork = blockEntity.checkStructureCompleted();

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
            if (blockEntity.isBiomeBoosted()) {
                ratio *= 1.2f;
            }

            if ((int) ratio * 6 > 0) {
                long energyToGenerate = Math.min(ENERGY_GENERATION_RATE, storage.getCapacity() - storage.getAmount());
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
        }

        blockEntity.setChanged();
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
        if (level == null) return true;
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
        return new com.singularity_iteration.mio_icif.Menu.Generator.GeomagneticGeneratorMenu(containerId, inventory, this);
    }

    public boolean isStructureComplete() {
        return shouldMachineWork;
    }
}