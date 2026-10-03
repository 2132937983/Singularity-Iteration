package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.menu.producer.MetalFormerAdvancedMenu;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.GenericMachineBlockEntity;
import com.miophas.singularity_iteration.core.api.machine.IMachineAPI;
import com.miophas.singularity_iteration.core.api.machine.IProducerBlock;
import com.miophas.singularity_iteration.core.api.machine.ISlotLayout;
import com.miophas.singularity_iteration.core.api.machine.ISlotType;
import com.miophas.singularity_iteration.core.api.machine.builder.IElectricMachineBuilder;
import com.miophas.singularity_iteration.core.api.machine.builder.IMachineBuilderAPI;
import com.miophas.singularity_iteration.core.api.recipe.IRecipeAPI;
import com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings("null")
public class mio_icif_metal_former_advanced extends GenericMachineBlockEntity implements com.miophas.singularity_iteration.common.block.BlockStateMirror {

    public static final long DEFAULT_CAPACITY = 1000L;
    public static final long DEFAULT_MAX_RECEIVE = 128L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 20;
    public static final long DEFAULT_ENERGY_PER_TICK = 50L;

    private static IMachineBuilderAPI.MachineConfiguration CACHED_CONFIGURATION;
    private static ISlotLayout CACHED_LAYOUT;
    private static ICableTier CACHED_TIER;

    private final int inputSlot;
    private final int batterySlot;
    private final int outputSlot;

    private IMachineAPI.MetalFormerMode currentMode = IMachineAPI.MetalFormerMode.ROLLING;

    public mio_icif_metal_former_advanced(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.METAL_FORMER_ADVANCED_ENTITY_TYPE.get());
    }

    public mio_icif_metal_former_advanced(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            getOrCreateLayout(),
            DEFAULT_ENERGY_PER_TICK,
            getOrCreateTier());
        ISlotLayout layout = getOrCreateLayout();
        this.inputSlot = layout.getInputSlots()[0];
        this.batterySlot = layout.getBatterySlots()[0];
        this.outputSlot = layout.getOutputSlots()[0];
        setConfiguration(getOrCreateConfiguration());
    }

    public static ISlotLayout getOrCreateLayout() {
        if (CACHED_LAYOUT == null) {
            CACHED_LAYOUT = MioIcifAPI.instance().getMachineBuilderAPI()
                .createStandardLayout(1, 1, true, 4);
        }
        return CACHED_LAYOUT;
    }

    public static ICableTier getOrCreateTier() {
        if (CACHED_TIER == null) {
            CACHED_TIER = MioIcifAPI.instance().getEnergyNetAPI().getCableTier("mv");
        }
        return CACHED_TIER;
    }

    public static IMachineBuilderAPI.MachineConfiguration getOrCreateConfiguration() {
        if (CACHED_CONFIGURATION == null) {
            CACHED_CONFIGURATION = buildMachineDefinition();
        }
        return CACHED_CONFIGURATION;
    }

    private static IMachineBuilderAPI.MachineConfiguration buildMachineDefinition() {
        IElectricMachineBuilder builder = MioIcifAPI.instance().getMachineBuilderAPI()
            .createElectricMachineBuilder()
            .setName("metal_former_advanced")
            .setTranslationKey("container.mio_icif.metal_former_advanced")
            .setEnergyCapacity(DEFAULT_CAPACITY)
            .setMaxReceive(DEFAULT_MAX_RECEIVE)
            .setMaxExtract(DEFAULT_MAX_EXTRACT)
            .setEnergyPerTick(DEFAULT_ENERGY_PER_TICK)
            .setProcessTime(DEFAULT_WORK_TIME)
            .setCableTier(getOrCreateTier())
            .useStandardLayout(1, 1, true, 4)
            .setSupportsUpgrades(true)
            .setMachineType(IMachineAPI.MachineType.METAL_FORMER)
            .withEntityType(mio_icif_block_entities.METAL_FORMER_ADVANCED_ENTITY_TYPE.get());

        builder.buildAndRegister("mio_icif");
        return builder.getConfiguration();
    }



    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        ISlotType type = getOrCreateLayout().getType(slot);
        if (type.isInput()) return isValidMetalFormerInput(stack);
        if (type.isBattery()) return isBattery(stack);
        if (type.isOutput()) return false;
        if (type.isUpgrade()) return getItemAPI().isUpgrade(stack);
        return false;
    }

    private boolean isValidMetalFormerInput(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (isBattery(stack)) return false;
        if (getItemAPI().isUpgrade(stack)) return false;
        if (level == null) return true;

        IRecipeAPI recipeAPI = MioIcifAPI.instance().getRecipeAPI();
        return recipeAPI.findRollingRecipe(stack, level).isPresent()
            || recipeAPI.findCuttingRecipe(stack, level).isPresent()
            || recipeAPI.findExtrudingRecipe(stack, level).isPresent();
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return new int[]{inputSlot, batterySlot, outputSlot};
    }

    @Override
    protected boolean canWork() {
        return getEffectiveEnergyPerTick() > 0 && (progress >= maxProgress || hasEnoughEnergy())
            && prepareOperation().isPresent();
    }

    private Optional<RecipeSlots.Prepared> prepareOperation() {
        var recipe = findRecipeForMode();
        if (recipe.isEmpty()) return Optional.empty();
        var api = MioIcifAPI.instance().getRecipeAPI();
        return RecipeSlots.prepare(itemHandler, inputSlot, api.getRecipeIngredientCount(recipe.get()),
            new int[]{outputSlot}, java.util.List.of(api.getRecipeOutput(recipe.get())));
    }

    @Override
    protected boolean shouldResetProgress() {
        ItemStack input = itemHandler.getStackInSlot(inputSlot);
        if (input.isEmpty()) return true;
        return findRecipeForMode().isEmpty();
    }

    private Optional<? extends RecipeHolder<?>> findRecipeForMode() {
        if (level == null) return Optional.empty();
        ItemStack input = itemHandler.getStackInSlot(inputSlot);
        if (input.isEmpty()) return Optional.empty();

        IRecipeAPI recipeAPI = MioIcifAPI.instance().getRecipeAPI();
        return switch (currentMode) {
            case ROLLING -> recipeAPI.findRollingRecipe(input, level);
            case CUTTING -> recipeAPI.findCuttingRecipe(input, level);
            case EXTRUDING -> recipeAPI.findExtrudingRecipe(input, level);
        };
    }







    @Override
    protected void doWork() {
        if (prepareOperation().isEmpty()) { stopWork(); return; }
        if (progress < maxProgress) {
            if (getEffectiveEnergyPerTick() <= 0 || !hasEnoughEnergy() || !consumeEnergy()) { stopWork(); return; }
            progress = (int) Math.min(maxProgress, (long) progress + getProgressPerTick());
        }
        isWorking = true;
        if (progress >= maxProgress) finishProcessing();
    }

    @Override
    protected void updateProgress() { /* doWork owns the paid increment. */ }

    private void finishProcessing() {
        var operation = prepareOperation();
        if (operation.isEmpty()) { stopWork(); return; }
        int completed = progress;
        progress = 0;
        if (!operation.get().commit()) { progress = completed; stopWork(); return; }
        finishWork();
        setChanged();
    }

    public int getRecipeProcessingTime() {
        Optional<? extends RecipeHolder<?>> recipe = findRecipeForMode();
        if (recipe.isEmpty()) return DEFAULT_WORK_TIME;
        return MioIcifAPI.instance().getRecipeAPI().getRecipeProcessTime(recipe.get());
    }

    public long getRecipeEnergyPerTick() {
        Optional<? extends RecipeHolder<?>> recipe = findRecipeForMode();
        if (recipe.isEmpty()) return DEFAULT_ENERGY_PER_TICK;
        return MioIcifAPI.instance().getRecipeAPI().getRecipeEnergyPerTick(recipe.get());
    }

    @Override
    public IMachineAPI.MetalFormerMode getMetalFormerMode() {
        return currentMode;
    }

    @Override
    public void setMetalFormerMode(IMachineAPI.MetalFormerMode mode) {
        if (mode == null) return;
        stopWork();
        this.currentMode = mode;
        this.progress = 0;
        setLit(isWorking());
        mirrorToBlockState();   // the advanced former never showed its mode on the block before
        setChanged();
    }

    @Override
    public void mirrorToBlockState() {
        var prop = com.miophas.singularity_iteration.common.block.producer.mio_icif_block_metal_former_advanced.MODE;
        if (level == null || level.isClientSide || !getBlockState().hasProperty(prop)) return;
        var want = com.miophas.singularity_iteration.common.block.producer.mio_icif_block_metal_former_advanced.MetalFormerMode.valueOf(currentMode.name());
        if (getBlockState().getValue(prop) != want) level.setBlockAndUpdate(worldPosition, getBlockState().setValue(prop, want));
    }

    public void setMode(IMachineAPI.MetalFormerMode mode) {
        setMetalFormerMode(mode);
    }

    public IMachineAPI.MetalFormerMode getMode() {
        return getMetalFormerMode();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Mode", currentMode.getId());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int modeOrdinal = tag.getInt("Mode");
        this.currentMode = IMachineAPI.MetalFormerMode.fromId(modeOrdinal);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_metal_former_advanced blockEntity) {
        if (level.isClientSide()) return;
        if (blockEntity instanceof IProducerBlock producer) {
            producer.serverTick();
        }
        blockEntity.setLit(blockEntity.isWorking());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.metal_former_advanced");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MetalFormerAdvancedMenu(containerId, playerInventory, this);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return getSlotsForDirection(side);
    }
}