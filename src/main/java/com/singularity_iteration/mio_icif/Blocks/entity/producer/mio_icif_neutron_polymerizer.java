package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Menu.Producer.NeutronPolymerizerMenu;
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
public class mio_icif_neutron_polymerizer extends GenericMachineBlockEntity {

    public static final long DEFAULT_CAPACITY = 2304000L;
    public static final long DEFAULT_MAX_RECEIVE = 2048L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 1500;
    public static final long DEFAULT_ENERGY_PER_TICK = 1536L;

    private static IMachineBuilderAPI.MachineConfiguration CACHED_CONFIGURATION;
    private static ISlotLayout CACHED_LAYOUT;
    private static ICableTier CACHED_TIER;

    private final int inputSlot;
    private final int batterySlot;
    private final int[] outputSlots;

    public mio_icif_neutron_polymerizer(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.NEUTRON_POLYMERIZER_ENTITY_TYPE.get());
    }

    public mio_icif_neutron_polymerizer(BlockPos pos, BlockState state, BlockEntityType<?> type) {
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
        this.outputSlots = layout.getOutputSlots();
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
            CACHED_TIER = MioIcifAPI.instance().getEnergyNetAPI().getCableTier("ev");
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
            .setName("neutron_polymerizer")
            .setTranslationKey("container.mio_icif.neutron_polymerizer")
            .setEnergyCapacity(DEFAULT_CAPACITY)
            .setMaxReceive(DEFAULT_MAX_RECEIVE)
            .setMaxExtract(DEFAULT_MAX_EXTRACT)
            .setEnergyPerTick(DEFAULT_ENERGY_PER_TICK)
            .setProcessTime(DEFAULT_WORK_TIME)
            .setCableTier(getOrCreateTier())
            .useStandardLayout(1, 1, true, 4)
            .setSupportsUpgrades(true)
            .setMachineType(com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType.NEUTRON_POLYMERIZER)
            .withEntityType(mio_icif_block_entities.NEUTRON_POLYMERIZER_ENTITY_TYPE.get());

        builder.buildAndRegister("mio_icif");
        return builder.getConfiguration();
    }

    private void handleWork(GenericMachineBlockEntity machine, ItemStack[] inputs) {
        if (level == null || level.isClientSide) return;
        if (inputs.length == 0 || inputs[0].isEmpty()) return;

        IRecipeAPI recipeAPI = MioIcifAPI.instance().getRecipeAPI();
        Optional<? extends RecipeHolder<?>> recipe = recipeAPI.findNeutronPolymerizerRecipe(inputs[0], level);
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
        if (type.isInput()) return isPolymerizable(stack);
        if (type.isBattery()) return isBattery(stack);
        if (type.isOutput()) return false;
        if (type.isUpgrade()) return getItemAPI().isUpgrade(stack);
        return false;
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        int[] allSlots = new int[1 + outputSlots.length + 1];
        allSlots[0] = inputSlot;
        System.arraycopy(outputSlots, 0, allSlots, 1, outputSlots.length);
        allSlots[allSlots.length - 1] = batterySlot;
        return allSlots;
    }

    private boolean isPolymerizable(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        return MioIcifAPI.instance().getRecipeAPI().findNeutronPolymerizerRecipe(stack, level).isPresent();
    }

    @Override
    protected boolean hasValidRecipe() {
        ItemStack input = itemHandler.getStackInSlot(inputSlot);
        return isPolymerizable(input);
    }

    private Optional<? extends RecipeHolder<?>> findRecipe() {
        if (level == null) {
            return Optional.empty();
        }
        ItemStack input = itemHandler.getStackInSlot(inputSlot);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        return MioIcifAPI.instance().getRecipeAPI().findNeutronPolymerizerRecipe(input, level);
    }

    @Override
    protected boolean canWork() {
        ItemStack input = itemHandler.getStackInSlot(inputSlot);
        if (input.isEmpty()) {
            return false;
        }

        if (!hasEnoughEnergy()) {
            return false;
        }

        Optional<? extends RecipeHolder<?>> recipe = findRecipe();
        if (recipe.isEmpty()) {
            return false;
        }

        IRecipeAPI recipeAPI = MioIcifAPI.instance().getRecipeAPI();
        ItemStack result = recipeAPI.getRecipeOutput(recipe.get());
        int ingredientCount = recipeAPI.getRecipeIngredientCount(recipe.get());
        if (input.getCount() < ingredientCount) return false;
        return canFitOutput(result);
    }

    private boolean canFitOutput(ItemStack result) {
        for (int outputSlot : outputSlots) {
            ItemStack existing = itemHandler.getStackInSlot(outputSlot);
            if (canFitInSlot(existing, result)) return true;
        }
        return false;
    }

    private boolean canFitInSlot(ItemStack existing, ItemStack result) {
        if (existing.isEmpty()) return true;
        if (!ItemStack.isSameItem(existing, result)) return false;
        return existing.getCount() + result.getCount() <= existing.getMaxStackSize();
    }

    private int getTargetOutputSlot(ItemStack result) {
        for (int outputSlot : outputSlots) {
            ItemStack existing = itemHandler.getStackInSlot(outputSlot);
            if (canFitInSlot(existing, result)) return outputSlot;
        }
        return -1;
    }

    @Override
    protected void doWork() {
        if (!consumeEnergy()) {
            stopWork();
            return;
        }

        isWorking = true;

        Optional<? extends RecipeHolder<?>> recipe = findRecipe();
        if (recipe.isEmpty()) {
            stopWork();
            return;
        }

        if (progress >= maxProgress) {
            finishPolymerizing(recipe.get());
        }
    }

    private void finishPolymerizing(RecipeHolder<?> recipe) {
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

        int ingredientCount = recipeAPI.getRecipeIngredientCount(recipe);
        ItemStack input = itemHandler.getStackInSlot(inputSlot);
        input.shrink(ingredientCount);

        finishWork();

        if (canWork()) {
            isWorking = true;
        }
    }

    public ItemStack getResultItem() {
        if (level == null) return ItemStack.EMPTY;
        Optional<? extends RecipeHolder<?>> recipe = findRecipe();
        return recipe.map(r -> MioIcifAPI.instance().getRecipeAPI().getRecipeOutput(r)).orElse(ItemStack.EMPTY);
    }

    public ItemStack getInputItem() {
        return itemHandler.getStackInSlot(inputSlot);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_neutron_polymerizer blockEntity) {
        if (level.isClientSide()) return;
        if (blockEntity instanceof IProducerBlock producer) {
            producer.serverTick();
        }
        blockEntity.setLit(blockEntity.isWorking());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.neutron_polymerizer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NeutronPolymerizerMenu(containerId, playerInventory, this);
    }
}