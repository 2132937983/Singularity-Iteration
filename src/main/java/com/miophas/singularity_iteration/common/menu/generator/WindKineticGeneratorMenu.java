package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_generator_menu;
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
        super(mio_icif_menus.WIND_KINETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Wind_Kinetic_Generator.DATA_COUNT);
        addCustomMoveRule(
            stack -> stack.getItem() instanceof com.miophas.singularity_iteration.common.item.rotors.mio_icif_rotor,
            ROTOR_SLOT, ROTOR_SLOT + 1
        );
    }

    public WindKineticGeneratorMenu(int containerId, Inventory playerInventory,
                                    @Nullable com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Wind_Kinetic_Generator blockEntity,
                                    @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.WIND_KINETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
            itemHandler, data, com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Wind_Kinetic_Generator.DATA_COUNT, blockEntity);
        addCustomMoveRule(
            stack -> stack.getItem() instanceof com.miophas.singularity_iteration.common.item.rotors.mio_icif_rotor,
            ROTOR_SLOT, ROTOR_SLOT + 1
        );
    }

    public int getStatus() { return data.get(DATA_STATUS); }
    public boolean isGenerating() {
        return getStatus() == com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Wind_Kinetic_Generator.STATUS_GENERATING;
    }
    public int getKineticOutput() { return data.get(DATA_KINETIC_OUTPUT); }
    public int getRotorHealthPercent() { return data.get(DATA_ROTOR_HEALTH); }

    /** 0.1.11.9: 248 x 204 with the wind rose and the power curve; the rotor slot on the left. */
    public static final int GUI_WIDTH = 248, GUI_HEIGHT = 244;
    public static final int SLOT_X = 10, SLOT_Y = 46;
    public static final int INVENTORY_X = 44, INVENTORY_Y = 162, HOTBAR_Y = 220;

    /** Synced value {@code i} (see the block entity's getContainerData). */
    public int value(int i) { return data.get(i); }

    /** Button 0: storm protection on / off. */
    @Override
    public boolean clickMenuButton(net.minecraft.world.entity.player.Player player, int id) {
        if (id != 0 || !(getBlockEntity() instanceof com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Wind_Kinetic_Generator g)
            || !stillValid(player)) return false;
        g.toggleStormProtection();
        return true;
    }

    @Override
    protected int getPlayerInventoryY() { return INVENTORY_Y; }

    @Override
    protected int getPlayerHotbarY() { return HOTBAR_Y; }

    @Override
    protected void addPlayerInventory(Inventory playerInventory, int startY, int hotbarY) {
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j)
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, j + i * 9 + 9, INVENTORY_X + j * 18, startY + i * 18));
        for (int i = 0; i < 9; ++i) this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, i, INVENTORY_X + i * 18, hotbarY));
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, ROTOR_SLOT, SLOT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof com.miophas.singularity_iteration.common.item.rotors.mio_icif_rotor;
            }
        });
    }
}


