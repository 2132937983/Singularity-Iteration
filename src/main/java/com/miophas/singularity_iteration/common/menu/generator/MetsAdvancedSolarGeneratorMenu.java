package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_MetsAdvancedSolarGenerator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class MetsAdvancedSolarGeneratorMenu extends mio_icif_generator_menu implements ModernSolarView {

    public static final int BATTERY_SLOT = 0;
    public static final int SLOT_COUNT = 1;

    private static final int BATTERY_SLOT_X = 115;
    private static final int BATTERY_SLOT_Y = 35;

    private final ContainerLevelAccess access;
    public final mio_icif_MetsAdvancedSolarGenerator blockEntity;

    public MetsAdvancedSolarGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public MetsAdvancedSolarGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_MetsAdvancedSolarGenerator blockEntity) {
        super(mio_icif_menus.METS_ADVANCED_SOLAR_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT,
            playerInventory,
            blockEntity != null ? blockEntity.getItemHandlerCapability(null) : null,
            null, 4, blockEntity);

        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(BATTERY_SLOT, BATTERY_SLOT_X, BATTERY_SLOT_Y);
    }

    /** Live EU/t (scaled by sky brightness). */
    public int getLiveOutput() {
        return data.getCount() > 3 ? data.get(3) : 0;
    }

    public boolean isGenerating() {
        return data.get(2) == 1;
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return stack.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IBatteryItem;
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
            this.setSyncData(2, blockEntity.isGenerating() ? 1 : 0);
            this.setSyncData(3, (int) Math.min(Integer.MAX_VALUE, blockEntity.getPowerOutput()));
        }
    }
}