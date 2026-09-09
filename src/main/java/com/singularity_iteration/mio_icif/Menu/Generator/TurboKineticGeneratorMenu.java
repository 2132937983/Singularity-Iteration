package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_turbo_kinetic_generator;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class TurboKineticGeneratorMenu extends mio_icif_generator_menu {

    private static final int DATA_ENERGY = 0;
    private static final int DATA_MAX_ENERGY = 1;
    private static final int DATA_KINETIC = 2;
    private static final int DATA_MAX_KINETIC = 3;
    private static final int DATA_IS_WORKING = 4;
    private static final int DATA_ENERGY_OUTPUT = 5;
    private static final int DATA_COUNT = 6;
    private static final int SLOT_COUNT = 0;

    public TurboKineticGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public TurboKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public TurboKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.TURBO_KINETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public TurboKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_turbo_kinetic_generator blockEntity, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.TURBO_KINETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT, blockEntity);
    }

    public void setSyncData(int energy, int maxEnergy, int kinetic, int maxKinetic, boolean isWorking, int energyOutput) {
        this.data.set(DATA_ENERGY, energy);
        this.data.set(DATA_MAX_ENERGY, maxEnergy);
        this.data.set(DATA_KINETIC, kinetic);
        this.data.set(DATA_MAX_KINETIC, maxKinetic);
        this.data.set(DATA_IS_WORKING, isWorking ? 1 : 0);
        this.data.set(DATA_ENERGY_OUTPUT, energyOutput);
    }

    public int getEnergy() {
        return this.data.get(DATA_ENERGY);
    }

    public int getMaxEnergy() {
        return this.data.get(DATA_MAX_ENERGY);
    }

    public int getKinetic() {
        return this.data.get(DATA_KINETIC);
    }

    public int getMaxKinetic() {
        return this.data.get(DATA_MAX_KINETIC);
    }

    public boolean isWorking() {
        return this.data.get(DATA_IS_WORKING) == 1;
    }

    public int getEnergyOutput() {
        return this.data.get(DATA_ENERGY_OUTPUT);
    }

    public int getEnergyProgressPixels() {
        int energy = getEnergy();
        int maxEnergy = getMaxEnergy();
        if (maxEnergy <= 0) return 0;
        return (int) ((energy * KINETIC_ENERGY_BAR2_WIDTH) / maxEnergy);
    }

    public int getKineticProgressPixels() {
        int kinetic = getKinetic();
        int maxKinetic = getMaxKinetic();
        if (maxKinetic <= 0) return 0;
        return (int) ((kinetic * KINETIC_ENERGY_BAR2_WIDTH) / maxKinetic);
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity instanceof mio_icif_turbo_kinetic_generator generator) {
            setSyncData(
                (int) generator.getEnergyStorage().getAmount(),
                (int) generator.getEnergyStorage().getCapacity(),
                (int) generator.getKineticStorage().getKineticStored(),
                (int) generator.getKineticStorage().getMaxKineticStored(),
                generator.isBurning(),
                (int) generator.getLastProduction()
            );
        }
        super.broadcastChanges();
    }

    @Override
    protected void addMachineSlots() {
    }
}