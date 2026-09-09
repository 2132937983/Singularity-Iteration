package com.singularity_iteration.mio_icif.Menu.OilRig;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("null")
public class OilRigPanelMenu extends AbstractContainerMenu {

    public static final int DATA_DRILL_X = 0;
    public static final int DATA_DRILL_Y = 1;
    public static final int DATA_DRILL_Z = 2;
    public static final int DATA_CORE_STATE = 3;
    public static final int DATA_COUNT = 4;

    private final ContainerData data;

    public OilRigPanelMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainerData(DATA_COUNT));
    }

    public OilRigPanelMenu(int containerId, Inventory playerInventory, ContainerData data) {
        super(mio_icif_menus.OIL_RIG_PANEL_MENU_TYPE.get(), containerId);
        this.data = data;
        addDataSlots(this.data);
        addPlayerInventorySlots(playerInventory);
    }

    public OilRigPanelMenu(int containerId, Inventory playerInventory,
                           com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_oil_rig_core_entity core) {
        super(mio_icif_menus.OIL_RIG_PANEL_MENU_TYPE.get(), containerId);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_DRILL_X -> core.getDrillCoordinate().getX();
                    case DATA_DRILL_Y -> core.getDrillCoordinate().getY();
                    case DATA_DRILL_Z -> core.getDrillCoordinate().getZ();
                    case DATA_CORE_STATE -> core.getCoreState();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        addDataSlots(this.data);
        addPlayerInventorySlots(playerInventory);
    }

    public OilRigPanelMenu(int containerId, Inventory playerInventory,
                           com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_oil_rig_panel_entity panel) {
        super(mio_icif_menus.OIL_RIG_PANEL_MENU_TYPE.get(), containerId);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_DRILL_X -> panel.getDrillCoord().getX();
                    case DATA_DRILL_Y -> panel.getDrillCoord().getY();
                    case DATA_DRILL_Z -> panel.getDrillCoord().getZ();
                    case DATA_CORE_STATE -> panel.getCoreState();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        addDataSlots(this.data);
        addPlayerInventorySlots(playerInventory);
    }

    public OilRigPanelMenu(int containerId, Inventory playerInventory,
                           com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_dimension_oil_rig_core_entity core) {
        super(mio_icif_menus.OIL_RIG_PANEL_MENU_TYPE.get(), containerId);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_DRILL_X -> core.getDrillCoordinate().getX();
                    case DATA_DRILL_Y -> core.getDrillCoordinate().getY();
                    case DATA_DRILL_Z -> core.getDrillCoordinate().getZ();
                    case DATA_CORE_STATE -> core.getCoreState();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        addDataSlots(this.data);
        addPlayerInventorySlots(playerInventory);
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    public int getDrillX() { return data.get(DATA_DRILL_X); }
    public int getDrillY() { return data.get(DATA_DRILL_Y); }
    public int getDrillZ() { return data.get(DATA_DRILL_Z); }
    public int getCoreState() { return data.get(DATA_CORE_STATE); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}