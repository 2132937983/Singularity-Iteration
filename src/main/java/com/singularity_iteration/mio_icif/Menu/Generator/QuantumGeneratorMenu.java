package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_QuantumGenerator;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class QuantumGeneratorMenu extends mio_icif_generator_menu {

    public static final int SLOT_COUNT = 0; // 量子发电机没有物品槽位

    private final ContainerLevelAccess access;
    public final mio_icif_QuantumGenerator blockEntity;

    public QuantumGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public QuantumGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_QuantumGenerator blockEntity) {
        super(mio_icif_menus.QUANTUM_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT,
            playerInventory,
            new ItemStackHandler(SLOT_COUNT),
            null, 5, blockEntity);

        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;
    }

    @Override
    protected void addMachineSlots() {
        // 量子发电机没有机器槽位
    }

    public boolean isGenerating() {
        return data.get(2) == 1;
    }

    public int getProduction() {
        return blockEntity != null ? blockEntity.getProduction() : 0;
    }

    public int getTier() {
        return blockEntity != null ? blockEntity.getTier() : 1;
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
        }
    }
}