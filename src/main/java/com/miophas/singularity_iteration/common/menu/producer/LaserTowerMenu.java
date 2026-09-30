package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_laser_tower;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import org.jetbrains.annotations.Nullable;

/**
 * Laser tower GUI: battery slot, energy read-out and the adjustable scan box.
 *
 * <p>Range buttons go through vanilla {@code clickMenuButton}, so no custom packet is
 * needed; the server clamps every change to the tower's limits.
 */
@SuppressWarnings("null")
public class LaserTowerMenu extends mio_icif_machine_menu {
    public static final int DATA_H = 0;
    public static final int DATA_V = 1;
    public static final int DATA_MAX_H = 2;
    public static final int DATA_MAX_V = 3;
    public static final int DATA_TARGETS = 4;
    public static final int DATA_LAST_VOLLEY = 5;
    public static final int DATA_ENERGY_LO = 6;
    public static final int DATA_ENERGY_HI = 7;
    public static final int DATA_CAPACITY_LO = 8;
    public static final int DATA_CAPACITY_HI = 9;
    public static final int DATA_COST = 10;
    public static final int DATA_COUNT = 11;

    /** Button ids: H-1, H+1, V-1, V+1, H-8, H+8, V-8, V+8. */
    public static final int BUTTON_COUNT = 8;

    public static final int BATTERY_SLOT = 0;
    public static final int SLOT_COUNT = 1;
    public static final int GUI_HEIGHT = 182;

    public LaserTowerMenu(int containerId, Inventory playerInventory, @Nullable RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, null, null);
    }

    public LaserTowerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public LaserTowerMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_laser_tower tower,
                          @Nullable ContainerData data) {
        super(mio_icif_menus.LASER_TOWER_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
            tower != null ? tower.getItemHandler() : null, data, DATA_COUNT, tower);
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(BATTERY_SLOT, 8, 66);
    }

    @Override protected int getPlayerInventoryY() { return 100; }
    @Override protected int getPlayerHotbarY() { return 158; }
    @Override protected int getBatterySlotIndex() { return BATTERY_SLOT; }
    @Override protected int getDataSlotCount() { return DATA_COUNT; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(blockEntity instanceof mio_icif_laser_tower tower) || id < 0 || id >= BUTTON_COUNT) return false;
        int step = id < 4 ? 1 : 8;
        int sign = (id & 1) == 0 ? -1 : 1;
        boolean horizontal = (id % 4) < 2;
        tower.adjustRange(horizontal ? sign * step : 0, horizontal ? 0 : sign * step);
        return true;
    }

    private int word(int index) { return data.get(index) & 0xFFFF; }

    public int horizontalRange() { return data.get(DATA_H); }
    public int verticalRange() { return data.get(DATA_V); }
    public int maxHorizontal() { return data.get(DATA_MAX_H); }
    public int maxVertical() { return data.get(DATA_MAX_V); }
    public int maxTargets() { return data.get(DATA_TARGETS); }
    public int lastVolley() { return data.get(DATA_LAST_VOLLEY); }
    public int costPerTarget() { return word(DATA_COST); }
    public long energy() { return ((long) word(DATA_ENERGY_HI) << 16) | word(DATA_ENERGY_LO); }
    public long capacity() { return ((long) word(DATA_CAPACITY_HI) << 16) | word(DATA_CAPACITY_LO); }
}
