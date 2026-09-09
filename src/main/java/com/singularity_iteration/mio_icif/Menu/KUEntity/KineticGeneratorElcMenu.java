package com.singularity_iteration.mio_icif.Menu.KUEntity;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 电力动能机的容器菜单独?
 */
@SuppressWarnings({"null"}) public class KineticGeneratorElcMenu extends mio_icif_machine_menu {

    public static final int BATTERY_SLOT = 0;
    public static final int MOTOR_SLOT_START = 1;
    public static final int MOTOR_SLOT_COUNT = 10;
    public static final int SLOT_COUNT = 11;

    private static final int BATTERY_SLOT_X = 8;
    private static final int BATTERY_SLOT_Y = 62;
    private static final int MOTOR_START_X = 44;
    private static final int MOTOR_START_Y = 27;
    private static final int MOTOR_COLS = 5;
    private static final int MOTOR_ROWS = 2;
    private static final int MOTOR_SPACING = 18;

    private static final int DATA_IS_WORKING = 0;
    private static final int DATA_ENERGY = 1;
    private static final int DATA_MAX_ENERGY = 2;
    private static final int DATA_MOTOR_COUNT = 3;
    private static final int DATA_KINETIC_GENERATION = 4;
    private static final int DATA_MAX_KINETIC_GENERATION = 5;
    private static final int DATA_COUNT = 6;

    public KineticGeneratorElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public KineticGeneratorElcMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public KineticGeneratorElcMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.KINETIC_GENERATOR_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public void setSyncData(boolean isWorking, int energy, int maxEnergy, int motorCount, int kineticGeneration, int maxKineticGeneration) {
        this.data.set(DATA_IS_WORKING, isWorking ? 1 : 0);
        this.data.set(DATA_ENERGY, energy);
        this.data.set(DATA_MAX_ENERGY, maxEnergy);
        this.data.set(DATA_MOTOR_COUNT, motorCount);
        this.data.set(DATA_KINETIC_GENERATION, kineticGeneration);
        this.data.set(DATA_MAX_KINETIC_GENERATION, maxKineticGeneration);
    }

    public boolean isWorking() {
        return this.data.get(DATA_IS_WORKING) == 1;
    }

    public int getEnergy() {
        return this.data.get(DATA_ENERGY);
    }

    public int getMaxEnergy() {
        return this.data.get(DATA_MAX_ENERGY);
    }

    public int getMotorCount() {
        return this.data.get(DATA_MOTOR_COUNT);
    }

    public int getKineticGeneration() {
        return this.data.get(DATA_KINETIC_GENERATION);
    }

    public int getMaxKineticGeneration() {
        return this.data.get(DATA_MAX_KINETIC_GENERATION);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, BATTERY_SLOT_X, BATTERY_SLOT_Y) {
            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat;
            }
        });

        for (int row = 0; row < MOTOR_ROWS; row++) {
            for (int col = 0; col < MOTOR_COLS; col++) {
                int slotIndex = MOTOR_SLOT_START + row * MOTOR_COLS + col;
                int slotX = MOTOR_START_X + col * MOTOR_SPACING;
                int slotY = MOTOR_START_Y + row * MOTOR_SPACING;
                this.addSlot(new SlotItemHandler(itemHandler, slotIndex, slotX, slotY) {
                    @Override
                    public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                        return stack.getItem() == com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources.MOTOR.get();
                    }
                });
            }
        }
    }
}

