package com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator;

import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.mio_icif_KineticU_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import com.singularity_iteration.mio_icif.api.item.IKineticRotor;
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
public class mio_icif_Water_Kinetic_Generator extends mio_icif_KineticU_Generator {

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
    private int ticksUntilRotorDamage = ROTOR_DAMAGE_INTERVAL;
    private boolean isInValidBiome = false;
    private boolean isInNaturalWater = false;
    private BiomeType currentBiomeType = BiomeType.NONE;

    public mio_icif_Water_Kinetic_Generator(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }


    public mio_icif_Water_Kinetic_Generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type != null ? type : com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities.WATER_KINETIC_GENERATOR_ENTITY_TYPE.get(),
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
        if (level == null) {
            isInValidBiome = false;
            currentBiomeType = BiomeType.NONE;
            return;
        }

        String biomeName = level.getBiome(worldPosition).unwrapKey()
                .map(key -> key.location().toString()).orElse("");

        if (biomeName.contains("river")) {
            isInValidBiome = true;
            currentBiomeType = BiomeType.RIVER;
        } else if (biomeName.contains("ocean") || biomeName.contains("beach") || biomeName.contains("deep")) {
            isInValidBiome = true;
            currentBiomeType = BiomeType.OCEAN;
        } else {
            isInValidBiome = false;
            currentBiomeType = BiomeType.NONE;
        }
    }


    private void checkNaturalWater() {
        if (level == null) {
            isInNaturalWater = false;
            return;
        }

        Direction facing = getBlockState().getValue(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
        BlockPos frontPos = worldPosition.relative(facing);

        FluidState frontFluidState = level.getFluidState(frontPos);
        if (!frontFluidState.is(Fluids.WATER) && !frontFluidState.is(Fluids.FLOWING_WATER)) {
            isInNaturalWater = false;
            return;
        }

        int connectedWaterCount = 0;
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = frontPos.relative(direction);
            FluidState adjacentFluid = level.getFluidState(adjacentPos);
            if (adjacentFluid.is(Fluids.WATER) || adjacentFluid.is(Fluids.FLOWING_WATER)) {
                connectedWaterCount++;
            }
        }

        isInNaturalWater = connectedWaterCount >= 2;
    }

    private int calculateKineticOutput() {
        IKineticRotor rotor = getRotor();
        ItemStack rotorStack = getRotorStack();

        if (rotor == null) {
            return 0;
        }

        if (!isInValidBiome) {
            return 0;
        }

        if (!isInNaturalWater) {
            return 0;
        }

        // 誹???衡膀?碩
        int diameter = rotor.getDiameter(rotorStack);
        int amplitude = (diameter - 3) * 25 + currentBiomeType.amplitudeBonus;

        long timeOfDay = level != null ? level.getDayTime() % 24000L : 0L;
        double t = (double) timeOfDay;
        double angle = (Math.PI / 3000.0) * t;
        double cosValue = Math.cos(angle);
        double kineticOutput = amplitude - amplitude * cosValue;

        return (int) Math.round(kineticOutput);
    }

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

        Direction facing = getBlockState().getValue(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
        Direction backDirection = facing.getOpposite();
        BlockPos backPos = worldPosition.relative(backDirection);

        IMioIcifCapabilities.IKineticStorage backKinetic = level.getCapability(
            IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, backPos, facing);
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
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        super.onDataPacket(net, pkt, registries);
        CompoundTag tag = pkt.getTag();
        if (tag != null) loadAdditional(tag, registries);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.water_kinetic_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Generator.WaterKineticGeneratorMenu(containerId, playerInventory, this);
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

    // ==================== Getterよ猭ㄑUI?ボ ====================

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