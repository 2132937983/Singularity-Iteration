package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_geomagnetic_generator;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class GeomagneticGeneratorMenu extends mio_icif_generator_menu {

    public static final int BATTERY_SLOT = 0;
    public static final int SLOT_COUNT = 1;

    private static final int BATTERY_SLOT_X = 80;
    private static final int BATTERY_SLOT_Y = 26;

    private final ContainerLevelAccess access;
    public final mio_icif_geomagnetic_generator blockEntity;

    public GeomagneticGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public GeomagneticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_geomagnetic_generator blockEntity) {
        super(mio_icif_menus.GEOMAGNETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT,
            playerInventory,
            blockEntity != null ? blockEntity.getItemHandlerCapability(null) : null,
            null, 3, blockEntity);

        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(BATTERY_SLOT, BATTERY_SLOT_X, BATTERY_SLOT_Y);
    }

    public boolean isGenerating() {
        return data.get(2) == 1;
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat;
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
            this.setSyncData(2, blockEntity.isStructureComplete() ? 1 : 0);
        }
    }
}