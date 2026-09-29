package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.menu.producer.PowderAdvancedElcMenu;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.GenericMachineBlockEntity;
import com.miophas.singularity_iteration.core.api.machine.IProducerBlock;
import com.miophas.singularity_iteration.core.api.machine.ISlotLayout;
import com.miophas.singularity_iteration.core.api.machine.ISlotType;
import com.miophas.singularity_iteration.core.api.machine.builder.IElectricMachineBuilder;
import com.miophas.singularity_iteration.core.api.machine.builder.IMachineBuilderAPI;
import com.miophas.singularity_iteration.core.api.recipe.IRecipeAPI;
import com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots;
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
public class mio_icif_powder_advanced_elc extends GenericMachineBlockEntity {

    public static final long DEFAULT_CAPACITY = 750L;
    public static final long DEFAULT_MAX_RECEIVE = 128L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 150;
    public static final long DEFAULT_ENERGY_PER_TICK = 5L;

    private static IMachineBuilderAPI.MachineConfiguration CACHED_CONFIGURATION;
    private static ISlotLayout CACHED_LAYOUT;
    private static ICableTier CACHED_TIER;

    private final int inputSlot;
    private final int batterySlot;
    private final int outputSlot;

    public mio_icif_powder_advanced_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.POWDER_ADVANCED_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_powder_advanced_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
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
            .setName("powder_advanced_elc")
            .setTranslationKey("container.mio_icif.powder_advanced_elc")
            .setEnergyCapacity(DEFAULT_CAPACITY)
            .setMaxReceive(DEFAULT_MAX_RECEIVE)
            .setMaxExtract(DEFAULT_MAX_EXTRACT)
            .setEnergyPerTick(DEFAULT_ENERGY_PER_TICK)
            .setProcessTime(DEFAULT_WORK_TIME)
            .setCableTier(getOrCreateTier())
            .useStandardLayout(1, 1, true, 4)
            .setSupportsUpgrades(true)
            .setMachineType(com.miophas.singularity_iteration.core.api.machine.IMachineAPI.MachineType.MACERATOR)
            .withEntityType(mio_icif_block_entities.POWDER_ADVANCED_ELC_ENTITY_TYPE.get());

        builder.buildAndRegister("mio_icif");
        return builder.getConfiguration();
    }



    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        ISlotType type = getOrCreateLayout().getType(slot);
        if (type.isInput()) return isCrushable(stack);
        if (type.isBattery()) return isBattery(stack);
        if (type.isOutput()) return false;
        if (type.isUpgrade()) return getItemAPI().isUpgrade(stack);
        return false;
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return new int[]{inputSlot, batterySlot, outputSlot};
    }

    private boolean isCrushable(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        // 槽位校验时忽略数量，避免数量大于1的配方无法放入不足数量的材料
        return MioIcifAPI.instance().getRecipeAPI().findPowderRecipe(stack.copyWithCount(64), level).isPresent();
    }

    @Override
    protected boolean hasValidRecipe() {
        ItemStack input = itemHandler.getStackInSlot(inputSlot);
        return isCrushable(input);
    }

    private Optional<? extends RecipeHolder<?>> findRecipe() {
        if (level == null) {
            return Optional.empty();
        }
        ItemStack input = itemHandler.getStackInSlot(inputSlot);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        return MioIcifAPI.instance().getRecipeAPI().findPowderRecipe(input, level);
    }

    @Override
    protected boolean canWork() {
        return (progress >= maxProgress || hasEnoughEnergy()) && getEffectiveEnergyPerTick() > 0
            && findRecipe().flatMap(this::prepareOperation).isPresent();
    }

    private Optional<RecipeSlots.Prepared> prepareOperation(RecipeHolder<?> recipe) {
        var api = MioIcifAPI.instance().getRecipeAPI();
        return RecipeSlots.prepare(itemHandler, inputSlot, api.getRecipeIngredientCount(recipe),
            new int[]{outputSlot}, java.util.List.of(api.getRecipeOutput(recipe)));
    }







    @Override
    protected void doWork() {
        var recipe = findRecipe();
        if (recipe.isEmpty() || prepareOperation(recipe.get()).isEmpty()) { stopWork(); return; }
        if (progress < maxProgress) {
            if (getEffectiveEnergyPerTick() <= 0 || !hasEnoughEnergy() || !consumeEnergy()) { stopWork(); return; }
            progress = (int) Math.min(maxProgress, (long) progress + getProgressPerTick());
        }
        isWorking = true;
        if (progress >= maxProgress) finishCrushing(recipe.get());
    }

    @Override
    protected void updateProgress() {
        // doWork owns the single paid progress increment for this tick.
    }

    private void finishCrushing(RecipeHolder<?> recipe) {
        var operation = prepareOperation(recipe);
        if (operation.isEmpty()) { stopWork(); return; }
        int completed = progress;
        progress = 0;
        if (!operation.get().commit()) { progress = completed; stopWork(); return; }
        finishWork();
        setChanged();
    }

    public ItemStack getResultItem() {
        if (level == null) return ItemStack.EMPTY;
        Optional<? extends RecipeHolder<?>> recipe = findRecipe();
        return recipe.map(r -> MioIcifAPI.instance().getRecipeAPI().getRecipeOutput(r)).orElse(ItemStack.EMPTY);
    }

    public ItemStack getInputItem() {
        return itemHandler.getStackInSlot(inputSlot);
    }

    public ItemStack getOutputItem() {
        return itemHandler.getStackInSlot(outputSlot);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_powder_advanced_elc blockEntity) {
        if (level.isClientSide()) return;
        if (blockEntity instanceof IProducerBlock producer) {
            producer.serverTick();
        }
        blockEntity.setLit(blockEntity.isWorking());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.powder_advanced_elc");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PowderAdvancedElcMenu(containerId, playerInventory, this);
    }
}