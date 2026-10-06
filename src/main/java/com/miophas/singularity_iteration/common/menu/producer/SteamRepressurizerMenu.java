package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_steam_repressurizer;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/** IC2 layout: two fluid tanks and the player's 36 slots, no machine/upgrade slots. */
@SuppressWarnings("null")
public class SteamRepressurizerMenu extends mio_icif_machine_menu {
    public SteamRepressurizerMenu(int id, Inventory inventory) { this(id, inventory, null); }
    public SteamRepressurizerMenu(int id, Inventory inventory, @Nullable mio_icif_steam_repressurizer machine) {
        super(mio_icif_menus.STEAM_REPRESSURIZER_MENU_TYPE.get(), id, 0, inventory, null,
            machine == null ? null : machine.getContainerData(), mio_icif_steam_repressurizer.DATA_COUNT, machine);
    }
    @Override protected void addMachineSlots() { }
    @Override protected int getDataSlotCount() { return mio_icif_steam_repressurizer.DATA_COUNT; }
    public int getInputAmount() { return data.get(0); }
    public int getInputCapacity() { return data.get(1); }
    public int getOutputAmount() { return data.get(2); }
    public int getOutputCapacity() { return data.get(3); }
    public int getHeat() { return data.get(4); }
    public int getMaxHeat() { return data.get(5); }
    public FluidStack getInputFluid() { return fluid(data.get(6), getInputAmount()); }
    public FluidStack getOutputFluid() { return fluid(data.get(7), getOutputAmount()); }
    private static FluidStack fluid(int id, int amount) {
        var fluid = id < 0 ? null : BuiltInRegistries.FLUID.byId(id);
        return fluid == null || amount <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < 27 ? moveItemStackTo(stack, 27, 36, false)
            : moveItemStackTo(stack, 0, 27, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
