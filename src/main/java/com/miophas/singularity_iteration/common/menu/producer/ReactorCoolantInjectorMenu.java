package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.blockentity.producer.AbstractReactorCoolantInjector;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Menu of the coolant injectors, laid out like the IC2 1.12 {@code rci_rsh.xml} / {@code rci_lzh.xml}
 * GUI (176 x 166): the 3 x 3 coolant grid at (61, 16), the energy gauge at (7, 55) and the player
 * inventory at (7, 83). The 4 upgrade slots go to the shared upgrade dock.
 */
@SuppressWarnings("null")
public abstract class ReactorCoolantInjectorMenu extends mio_icif_machine_menu {

    public static final int GRID_X = 62, GRID_Y = 17;
    private static final int DATA_ENERGY = 0, DATA_MAX_ENERGY = 1, DATA_COOLANT = 2, DATA_RESERVE = 3;

    protected ReactorCoolantInjectorMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                                         @Nullable IItemHandler itemHandler, @Nullable ContainerData data,
                                         @Nullable AbstractReactorCoolantInjector blockEntity) {
        super(type, containerId, AbstractReactorCoolantInjector.SLOT_COUNT, playerInventory, itemHandler, data,
            AbstractReactorCoolantInjector.DATA_COUNT, blockEntity);
    }

    public int getEnergy() { return this.data.get(DATA_ENERGY); }
    public int getMaxEnergy() { return this.data.get(DATA_MAX_ENERGY); }
    /** Coolant blocks in the 9 slots. */
    public int getCoolantCount() { return this.data.get(DATA_COOLANT); }
    /** Coolant blocks in the reserve of an injector saved before 0.1.10.14. */
    public int getReserveCount() { return this.data.get(DATA_RESERVE); }

    @Override
    protected void addMachineSlots() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new SlotItemHandler(itemHandler, row * 3 + col, GRID_X + col * 18, GRID_Y + row * 18));
            }
        }
        for (int i = 0; i < AbstractReactorCoolantInjector.UPGRADE_SLOT_COUNT; i++) {
            addUpgradeSlot(itemHandler, AbstractReactorCoolantInjector.UPGRADE_SLOT_START + i, 152, 8 + i * 18);
        }
    }

    @Override
    protected int getInputSlotStart() { return 0; }

    @Override
    protected int getInputSlotCount() { return AbstractReactorCoolantInjector.INPUT_SLOT_COUNT; }

    @Override
    protected void registerMachineMoveRules() {
        super.registerMachineMoveRules();
        addCustomMoveRule(stack -> MioIcifAPI.instance().getItemAPI().isUpgrade(stack), AbstractReactorCoolantInjector.UPGRADE_SLOT_START,
            AbstractReactorCoolantInjector.UPGRADE_SLOT_START + AbstractReactorCoolantInjector.UPGRADE_SLOT_COUNT);
    }
}
