package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_QuantumGenerator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class QuantumGeneratorMenu extends mio_icif_generator_menu {

    public static final int SLOT_COUNT = 0; // 量子发电机没有物品槽位
    /** 0.1.11.131: the vanilla-frame generator layout (docs/gui_processing_machines.md): 248 x 244, inventory at (44, 162). */
    public static final int GUI_WIDTH = 248, GUI_HEIGHT = 244, INVENTORY_X = 44, INVENTORY_Y = 162, HOTBAR_Y = 220;
    /** Synced values: energy, capacity, on, rated EU/t, tier, EU made in the last tick. */
    public static final int DATA_COUNT = 6;

    private final ContainerLevelAccess access;
    public final mio_icif_QuantumGenerator blockEntity;

    public QuantumGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public QuantumGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_QuantumGenerator blockEntity) {
        super(mio_icif_menus.QUANTUM_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT,
            playerInventory,
            new ItemStackHandler(SLOT_COUNT),
            null, DATA_COUNT, blockEntity);

        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;
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

    /** 0.1.11.131: synced value {@code i} (see {@link #DATA_COUNT}). */
    public int value(int i) { return data.get(i); }

    @Override
    protected void addMachineSlots() {
        // 量子发电机没有机器槽位
    }

    public boolean isGenerating() {
        return data.get(2) == 1;
    }

    /** 0.1.11.11: the synced values (the client menu has no block entity, so this showed 0 EU/t, tier 1). */
    public int getProduction() {
        return data.get(3);
    }

    public int getTier() {
        return Math.max(1, data.get(4));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, blockEntity != null ?
            blockEntity.getBlockState().getBlock() : null);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity != null) {
            int energy = (int) blockEntity.getEnergyStorage().getAmount();
            int maxEnergy = (int) blockEntity.getEnergyStorage().getCapacity();
            this.setSyncData(0, energy);
            this.setSyncData(1, maxEnergy);
            this.setSyncData(2, blockEntity.isActive() ? 1 : 0);
            this.setSyncData(3, blockEntity.getProduction());
            this.setSyncData(4, blockEntity.getTier());
            this.setSyncData(5, (int) Math.min(Integer.MAX_VALUE, blockEntity.getLastGenerated()));
        }
    }
}