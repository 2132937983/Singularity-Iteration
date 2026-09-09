package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class WindKineticGeneratorMenu extends mio_icif_generator_menu {
    public static final int SLOT_COUNT = 1;
    public static final int ROTOR_SLOT = 0;
    private static final int DATA_STATUS = 0;
    private static final int DATA_KINETIC_OUTPUT = 1;
    private static final int DATA_ROTOR_HEALTH = 2;

    public WindKineticGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public WindKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public WindKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.WIND_KINETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, 3);
        addCustomMoveRule(
            stack -> stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Rotors.mio_icif_rotor,
            ROTOR_SLOT, ROTOR_SLOT + 1
        );
    }

    public WindKineticGeneratorMenu(int containerId, Inventory playerInventory,
                                    @Nullable com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Wind_Kinetic_Generator blockEntity,
                                    @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.WIND_KINETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
            itemHandler, data, 3, blockEntity);
        addCustomMoveRule(
            stack -> stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Rotors.mio_icif_rotor,
            ROTOR_SLOT, ROTOR_SLOT + 1
        );
    }

    public int getStatus() { return data.get(DATA_STATUS); }
    public boolean isGenerating() {
        return getStatus() == com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Wind_Kinetic_Generator.STATUS_GENERATING;
    }
    public int getKineticOutput() { return data.get(DATA_KINETIC_OUTPUT); }
    public int getRotorHealthPercent() { return data.get(DATA_ROTOR_HEALTH); }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, ROTOR_SLOT, 80, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Rotors.mio_icif_rotor;
            }
        });
    }
}


