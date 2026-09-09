package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Water_Kinetic_Generator;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.api.item.IKineticRotor;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class WaterKineticGeneratorMenu extends mio_icif_generator_menu {
    public static final int SLOT_COUNT = 1;
    public static final int ROTOR_SLOT = 0;

    public WaterKineticGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public WaterKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public WaterKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.WATER_KINETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, 8);
        addCustomMoveRule(
            stack -> stack.getItem() instanceof IKineticRotor,
            ROTOR_SLOT, ROTOR_SLOT + 1
        );
    }

    public WaterKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_Water_Kinetic_Generator blockEntity) {
        super(mio_icif_menus.WATER_KINETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
            blockEntity != null ? blockEntity.getItemHandler() : null, null, 8, blockEntity);
        addCustomMoveRule(
            stack -> stack.getItem() instanceof IKineticRotor,
            ROTOR_SLOT, ROTOR_SLOT + 1
        );
    }

    public int getKinetic() { return data.get(0); }
    public int getMaxKinetic() { return data.get(1); }
    public boolean isGenerating() { return data.get(2) == 1; }
    public int getKineticOutput() { return data.get(3); }
    public int getBiomeType() { return data.get(4); }
    public boolean isInNaturalWater() { return data.get(5) == 1; }
    public int getRotorDurability() { return data.get(6); }
    public int getRotorMaxDurability() { return data.get(7); }

    public String getBiomeTypeName() {
        int biomeType = getBiomeType();
        return switch (biomeType) {
            case 1 -> "gui.mio_icif.water_kinetic.biome_river";
            case 2 -> "gui.mio_icif.water_kinetic.biome_ocean";
            default -> "gui.mio_icif.water_kinetic.biome_other";
        };
    }

    public String getStatusReason() {
        if (!isInNaturalWater()) return "gui.mio_icif.water_kinetic.not_in_water";
        if (getBiomeType() == 0) return "gui.mio_icif.water_kinetic.biome_other";
        return "gui.mio_icif.water_kinetic.unknown";
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity instanceof mio_icif_Water_Kinetic_Generator generator) {
            setSyncData(0, (int) generator.getKineticStorage().getKineticStored());
            setSyncData(1, (int) generator.getKineticStorage().getMaxKineticStored());
            setSyncData(2, generator.isGenerating() ? 1 : 0);
            setSyncData(3, generator.getCurrentKineticOutput());
            setSyncData(4, generator.getCurrentBiomeType().ordinal());
            setSyncData(5, generator.isInNaturalWater() ? 1 : 0);
            setSyncData(6, generator.getRotorDurability());
            setSyncData(7, generator.getRotorMaxDurability());
        }
        super.broadcastChanges();
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, ROTOR_SLOT, 80, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (!(stack.getItem() instanceof IKineticRotor)) return false;
                return !stack.is(com.singularity_iteration.mio_icif.Items.mio_icif_items.ROTOR_WOOD.get());
            }
        });
    }
}