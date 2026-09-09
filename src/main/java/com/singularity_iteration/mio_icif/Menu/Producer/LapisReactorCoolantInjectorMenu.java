package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_lapis_reactor_coolant_injector;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 青金石反应堆冷却液注入器的容器菜单类
 */
@SuppressWarnings({"null"}) public class LapisReactorCoolantInjectorMenu extends mio_icif_machine_menu {

    public static final int SLOT_COUNT = 54;
    public static final int BATTERY_SLOT = 53;

    private static final int START_X = 8;
    private static final int START_Y = 18;
    private static final int SLOT_SIZE = 18;
    private static final int COLUMNS = 9;
    private static final int ROWS = 6;

    private static final int DATA_ENERGY = 0;
    private static final int DATA_MAX_ENERGY = 1;
    private static final int DATA_LAPIS_COUNT = 2;
    private static final int DATA_COUNT = 3;

    public LapisReactorCoolantInjectorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public LapisReactorCoolantInjectorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public LapisReactorCoolantInjectorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.LAPIS_REACTOR_COOLANT_INJECTOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public LapisReactorCoolantInjectorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_lapis_reactor_coolant_injector blockEntity) {
        this(containerId, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, blockEntity);
    }

    public LapisReactorCoolantInjectorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler,
                                           @Nullable ContainerData data, @Nullable mio_icif_lapis_reactor_coolant_injector blockEntity) {
        super(mio_icif_menus.LAPIS_REACTOR_COOLANT_INJECTOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              itemHandler, data, DATA_COUNT, blockEntity);
    }

    public void setSyncData(int energy, int maxEnergy, int lapisCount) {
        this.data.set(DATA_ENERGY, energy);
        this.data.set(DATA_MAX_ENERGY, maxEnergy);
        this.data.set(DATA_LAPIS_COUNT, lapisCount);
    }

    public int getEnergy() {
        return this.data.get(DATA_ENERGY);
    }

    public int getMaxEnergy() {
        return this.data.get(DATA_MAX_ENERGY);
    }

    public int getLapisBlockCount() {
        return this.data.get(DATA_LAPIS_COUNT);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_lapis_reactor_coolant_injector injector) {
            setSyncData(DATA_ENERGY, (int) injector.getEnergyStorage().getAmount());
            setSyncData(DATA_MAX_ENERGY, (int) injector.getEnergyStorage().getCapacity());
            setSyncData(DATA_LAPIS_COUNT, injector.getLapisBlockCount());
        }
    }

    @Override
    protected int getPlayerInventoryY() { return 140; }

    @Override
    protected int getPlayerHotbarY() { return 198; }

    @Override
    protected void addMachineSlots() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int slotIndex = row * COLUMNS + col;
                int x = START_X + col * SLOT_SIZE;
                int y = START_Y + row * SLOT_SIZE;

                if (slotIndex == BATTERY_SLOT) {
                    this.addSlot(new SlotItemHandler(itemHandler, slotIndex, x, y) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat;
                        }
                    });
                } else {
                    this.addSlot(new SlotItemHandler(itemHandler, slotIndex, x, y) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return stack.is(net.minecraft.world.item.Items.LAPIS_BLOCK);
                        }
                    });
                }
            }
        }
    }
}


