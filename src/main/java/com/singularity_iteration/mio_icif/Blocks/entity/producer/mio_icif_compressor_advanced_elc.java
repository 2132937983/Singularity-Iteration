package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Menu.Producer.CompressorAdvancedElcMenu;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.internal.machine.GenericMachineBlockEntity;
import com.singularity_iteration.mio_icif.api.machine.IProducerBlock;
import com.singularity_iteration.mio_icif.api.machine.ISlotLayout;
import com.singularity_iteration.mio_icif.api.machine.ISlotType;
import com.singularity_iteration.mio_icif.api.machine.builder.IElectricMachineBuilder;
import com.singularity_iteration.mio_icif.api.machine.builder.IMachineBuilderAPI;
import com.singularity_iteration.mio_icif.api.recipe.IRecipeAPI;
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

import java.util.Optional;

@SuppressWarnings("null")
public class mio_icif_compressor_advanced_elc extends GenericMachineBlockEntity {

    public static final long DEFAULT_CAPACITY = 1750L;
    public static final long DEFAULT_MAX_RECEIVE = 128L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 70;
    public static final long DEFAULT_ENERGY_PER_TICK = 25L;

    private static IMachineBuilderAPI.MachineConfiguration CACHED_CONFIGURATION;
    private static ISlotLayout CACHED_LAYOUT;
    private static ICableTier CACHED_TIER;

    private final int inputSlot;
    private final int batterySlot;
    private final int outputSlot;

    public mio_icif_compressor_advanced_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.COMPRESSOR_ADVANCED_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_compressor_advanced_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
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
        setWorkHandler(this::handleWork);
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
            .setName("compressor_advanced_elc")
            .setTranslationKey("container.mio_icif.compressor_advanced_elc")
            .setEnergyCapacity(DEFAULT_CAPACITY)
            .setMaxReceive(DEFAULT_MAX_RECEIVE)
            .setMaxExtract(DEFAULT_MAX_EXTRACT)
            .setEnergyPerTick(DEFAULT_ENERGY_PER_TICK)
            .setProcessTime(DEFAULT_WORK_TIME)
            .setCableTier(getOrCreateTier())
            .useStandardLayout(1, 1, true, 4)
            .setSupportsUpgrades(true)
            .setMachineType(com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType.COMPRESSOR)
            .withEntityType(mio_icif_block_entities.COMPRESSOR_ADVANCED_ELC_ENTITY_TYPE.get());

        builder.buildAndRegister("mio_icif");
        return builder.getConfiguration();
    }

    private void handleWork(GenericMachineBlockEntity machine, ItemStack[] inputs) {
        if (level == null || level.isClientSide) return;
        if (inputs.length == 0 || inputs[0].isEmpty()) return;

        IRecipeAPI recipeAPI = MioIcifAPI.instance().getRecipeAPI();
        Optional<? extends RecipeHolder<?>> recipe = recipeAPI.findCompressorRecipe(inputs[0], level);
        if (recipe.isEmpty()) return;

        ItemStack result = recipeAPI.getRecipeOutput(recipe.get());
        if (result.isEmpty()) return;

        int targetSlot = getTargetOutputSlot(result);
        if (targetSlot == -1) return;

        ItemStack currentOutput = itemHandler.getStackInSlot(targetSlot);
        if (currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(targetSlot, result.copy());
        } else {
            currentOutput.grow(result.getCount());
        }

        int ingredientCount = recipeAPI.getRecipeIngredientCount(recipe.get());
        consumeInput(inputSlot, ingredientCount);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        ISlotType type = getOrCreateLayout().getType(slot);
        if (type.isInput()) return isCompressible(stack);
        if (type.isBattery()) return isBattery(stack);
        if (type.isOutput()) return false;
        if (type.isUpgrade()) return getItemAPI().isUpgrade(stack);
        return false;
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return new int[]{inputSlot, batterySlot, outputSlot};
    }

    private boolean isCompressible(ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        return MioIcifAPI.instance().getRecipeAPI().findCompressorRecipe(stack, level).isPresent();
    }

    @Override
    protected boolean hasValidRecipe() {
        return isCompressible(itemHandler.getStackInSlot(inputSlot));
    }

    private Optional<? extends RecipeHolder<?>> findRecipe() {
        if (level == null) return Optional.empty();
        ItemStack input = itemHandler.getStackInSlot(inputSlot);
        if (input.isEmpty()) return Optional.empty();
        return MioIcifAPI.instance().getRecipeAPI().findCompressorRecipe(input, level);
    }

    @Override
    protected boolean canWork() {
        ItemStack input = itemHandler.getStackInSlot(inputSlot);
        if (input.isEmpty() || !hasEnoughEnergy()) return false;

        Optional<? extends RecipeHolder<?>> recipe = findRecipe();
        if (recipe.isEmpty()) return false;

        IRecipeAPI recipeAPI = MioIcifAPI.instance().getRecipeAPI();
        ItemStack result = recipeAPI.getRecipeOutput(recipe.get());
        int ingredientCount = recipeAPI.getRecipeIngredientCount(recipe.get());
        if (input.getCount() < ingredientCount) return false;
        return canFitOutput(result);
    }

    private boolean canFitOutput(ItemStack result) {
        ItemStack existing = itemHandler.getStackInSlot(outputSlot);
        return canFitInSlot(existing, result);
    }

    private boolean canFitInSlot(ItemStack existing, ItemStack result) {
        if (existing.isEmpty()) return true;
        if (!ItemStack.isSameItem(existing, result)) return false;
        return existing.getCount() + result.getCount() <= existing.getMaxStackSize();
    }

    private int getTargetOutputSlot(ItemStack result) {
        ItemStack existing = itemHandler.getStackInSlot(outputSlot);
        if (canFitInSlot(existing, result)) return outputSlot;
        return -1;
    }

    @Override
    protected void doWork() {
        if (!consumeEnergy()) { stopWork(); return; }
        isWorking = true;
        Optional<? extends RecipeHolder<?>> recipe = findRecipe();
        if (recipe.isEmpty()) { stopWork(); return; }
        if (progress >= maxProgress) finishCompressing(recipe.get());
    }

    private void finishCompressing(RecipeHolder<?> recipe) {
        IRecipeAPI recipeAPI = MioIcifAPI.instance().getRecipeAPI();
        ItemStack result = recipeAPI.getRecipeOutput(recipe);
        int targetSlot = getTargetOutputSlot(result);
        if (targetSlot == -1) return;

        ItemStack currentOutput = itemHandler.getStackInSlot(targetSlot);
        if (currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(targetSlot, result.copy());
        } else {
            currentOutput.grow(result.getCount());
        }

        itemHandler.getStackInSlot(inputSlot).shrink(recipeAPI.getRecipeIngredientCount(recipe));
        finishWork();
        if (canWork()) isWorking = true;
    }

    public ItemStack getResultItem() {
        if (level == null) return ItemStack.EMPTY;
        return findRecipe().map(r -> MioIcifAPI.instance().getRecipeAPI().getRecipeOutput(r)).orElse(ItemStack.EMPTY);
    }

    public ItemStack getInputItem() { return itemHandler.getStackInSlot(inputSlot); }
    public ItemStack getOutputItem() { return itemHandler.getStackInSlot(outputSlot); }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_compressor_advanced_elc blockEntity) {
        if (level.isClientSide()) return;
        if (blockEntity instanceof IProducerBlock producer) {
            producer.serverTick();
        }
        blockEntity.setLit(blockEntity.isWorking());
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