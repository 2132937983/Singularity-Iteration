package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_nuclear_reactor_generator;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactor;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class NuclearReactorGeneratorMenu extends mio_icif_generator_menu {
    public static final int REACTOR_SLOT_START = 0;
    public static final int MAX_REACTOR_SLOT_COUNT = 54;
    public static final int TOTAL_SLOT_COUNT = 54;

    private static final int REACTOR_SLOTS_X = 26;
    private static final int REACTOR_SLOTS_Y = 25;
    private static final int REACTOR_SLOTS_ROWS = 6;
    private static final int SLOT_SIZE = 18;

    private static final int PLAYER_INV_START_X = 26;

    public NuclearReactorGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null, null);
    }

    public NuclearReactorGeneratorMenu(int containerId, Inventory playerInventory,
                                        @Nullable mio_icif_nuclear_reactor_generator blockEntity,
                                        @Nullable IItemHandler itemHandler,
                                        @Nullable ContainerData data) {
        super(mio_icif_menus.NUCLEAR_REACTOR_GENERATOR_MENU_TYPE.get(), containerId, TOTAL_SLOT_COUNT,
              playerInventory, itemHandler, data, 6, blockEntity);
    }

    @Override
    protected int getPlayerInventoryY() { return 161; }

    @Override
    protected int getPlayerHotbarY() { return 219; }

    @Override
    protected void addPlayerInventory(Inventory playerInventory, int startY, int hotbarY) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, PLAYER_INV_START_X + j * 18, startY + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, PLAYER_INV_START_X + i * 18, hotbarY));
        }
    }

    @Override
    protected void addMachineSlots() {
        for (int row = 0; row < REACTOR_SLOTS_ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = row * 9 + col;
                int slotX = REACTOR_SLOTS_X + col * SLOT_SIZE;
                int slotY = REACTOR_SLOTS_Y + row * SLOT_SIZE;
                final int slotCol = col;
                this.addSlot(new SlotItemHandler(itemHandler, slotIndex, slotX, slotY) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        if (slotCol >= getAvailableColumns()) return false;
                        return stack.getItem() instanceof mio_icif_reactor;
                    }
                    @Override
                    public int getMaxStackSize() { return 1; }
                    @Override
                    public int getMaxStackSize(ItemStack stack) { return 1; }
                });
            }
        }
    }

    public int getEnergy() { return data.get(0); }
    public int getMaxEnergy() { return data.get(1); }
    public int getHeat() { return data.get(2); }
    public int getMaxHeat() { return data.get(3); }
    public int getOutputPower() { return data.get(4); }
    public int getAvailableColumns() { return data.get(5); }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_nuclear_reactor_generator gen) {
            setSyncData(0, (int) gen.getEnergyStorage().getAmount());
            setSyncData(1, (int) gen.getEnergyStorage().getCapacity());
            setSyncData(2, (int) gen.getHeatStorage().getHeatStored());
            setSyncData(3, (int) gen.getHeatStorage().getMaxHeatStored());
            setSyncData(4, gen.getCurrentOutput());
            setSyncData(5, gen.getAvailableColumns());
        }
    }
}