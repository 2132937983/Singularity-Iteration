package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_steam_kinetic_generator;
import com.miophas.singularity_iteration.common.energy.SteamPhysics;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_generator_menu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Container menu for the steam kinetic generator. 0.1.11.16: 248 x 204 (the mimic screen): the turbine slot on the left,
 * the upgrade slot docked; the data carries the steam, the expansion and the turbine's wear.
 */
@SuppressWarnings({"null"}) public class SteamKineticGeneratorMenu extends mio_icif_generator_menu {

    public static final int TURBINE_X = 10, TURBINE_Y = 30;
    public static final int INVENTORY_X = 44, INVENTORY_Y = 162, HOTBAR_Y = 220;
    /**
     * 0 hot steam, 1 has turbine, 2 waterlogged, 3 working, 4 KU/t, 5 (unused), 6 water mB, 7 throttled, 8 venting,
     * 9 steam mB, 10 steam capacity, 11 KU per mB x100, 12 exhaust condensed, 13 exhaust quality x1000,
     * 14 turbine life per mille, 15 steam fluid id (-1 none), 16 water capacity.
     */
    public static final int DATA_COUNT = 17;
    private static final int SLOT_COUNT = 5; // extra(2, shelved) + turbine(1) + upgrade(2, IC2 used 1)

    public SteamKineticGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, (IItemHandler) null, null);
    }

    public SteamKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public SteamKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.STEAM_KINETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public SteamKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_steam_kinetic_generator blockEntity) {
        super(mio_icif_menus.STEAM_KINETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null, null, DATA_COUNT, blockEntity);
    }

    public int value(int i) { return data.get(i); }

    public boolean isHotSteam() { return data.get(0) == 1; }
    public boolean hasTurbine() { return data.get(1) == 1; }
    public boolean isTurbineFilledWithWater() { return data.get(2) == 1; }
    public boolean isWorking() { return data.get(3) == 1; }
    public int getLastKineticOutput() { return data.get(4); }
    public int getWaterAmount() { return data.get(6); }
    public boolean isThrottled() { return data.get(7) == 1; }
    public boolean isVentingSteam() { return data.get(8) == 1; }

    @Override
    protected int getPlayerInventoryY() { return INVENTORY_Y; }

    @Override
    protected int getPlayerHotbarY() { return HOTBAR_Y; }

    @Override
    protected void addPlayerInventory(Inventory playerInventory, int startY, int hotbarY) {
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j)
            this.addSlot(new Slot(playerInventory, j + i * 9 + 9, INVENTORY_X + j * 18, startY + i * 18));
        for (int i = 0; i < 9; ++i) this.addSlot(new Slot(playerInventory, i, INVENTORY_X + i * 18, hotbarY));
    }

    @Override
    protected void addMachineSlots() {
        // slots 0-1 (the steam/water cell slots) are shelved: kept in the save layout, not in the GUI
        this.addSlot(new SlotItemHandler(itemHandler, mio_icif_steam_kinetic_generator.TURBINE_SLOT, TURBINE_X, TURBINE_Y));
        this.addUpgradeSlot(mio_icif_steam_kinetic_generator.UPGRADE_SLOT_START, TURBINE_X, TURBINE_Y + 30);
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity instanceof mio_icif_steam_kinetic_generator g) {
            boolean hot = g.isHotSteam();
            var state = hot ? SteamPhysics.SUPERHEATED : SteamPhysics.STEAM;
            double back = g.isExhaustCondensed() ? SteamPhysics.VACUUM : SteamPhysics.ATMOSPHERE;
            ItemStack turbine = g.getItemHandler().getStackInSlot(mio_icif_steam_kinetic_generator.TURBINE_SLOT);
            int life = turbine.isEmpty() || !turbine.isDamageableItem() ? (turbine.isEmpty() ? 0 : 1000)
                : (int) Math.round(1000.0 * (turbine.getMaxDamage() - turbine.getDamageValue()) / turbine.getMaxDamage());
            var steam = g.getSteamTank().getFluid();
            int[] values = {hot ? 1 : 0, g.hasTurbine() ? 1 : 0, g.isTurbineFilledWithWater() ? 1 : 0, g.isWorking() ? 1 : 0,
                g.getLastKineticOutput(), 0, g.getWaterTank().getFluidAmount(), g.isThrottled() ? 1 : 0, g.isVentingSteam() ? 1 : 0,
                g.getSteamTank().getFluidAmount(), g.getSteamTank().getCapacity(), (int) Math.round(g.kuPerMb(hot) * 100),
                g.isExhaustCondensed() ? 1 : 0,
                (int) Math.round(SteamPhysics.exhaustQuality(state, back, mio_icif_steam_kinetic_generator.KINETIC_TURBINE_EFFICIENCY) * 1000),
                life, steam.isEmpty() ? -1 : net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(steam.getFluid()), g.getWaterTank().getCapacity()};
            for (int i = 0; i < values.length; i++) data.set(i, values[i]);
        }
        super.broadcastChanges();
    }
}
