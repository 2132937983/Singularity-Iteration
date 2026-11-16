package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.registry.mio_icif_items;

import com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Water_Kinetic_Generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.core.api.item.IKineticRotor;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class WaterKineticGeneratorMenu extends mio_icif_generator_menu {
    /**
     * 0.1.11.18: 0 KU stored, 1 KU capacity, 2 generating, 3 KU/t, 4 biome, 5 water in front, 6 rotor durability,
     * 7 rotor max, 8 diameter, 9 source (WaterPhysics.Source), 10 mean speed of the water cells x1000, 11 water cells,
     * 12 disc cells, 13 wake factor x1000, 14 moon phase, 15 day time, 16 mean output, 17 the current's speed x1000,
     * 18.. the disc's cells (WaterPhysics.pack).
     */
    public static final int DATA_COUNT = 18 + com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.WaterPhysics.PACKED;
    public static final int SLOT_COUNT = 1;
    public static final int ROTOR_SLOT = 0;

    public WaterKineticGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public WaterKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public WaterKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.WATER_KINETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
        addCustomMoveRule(
            stack -> stack.getItem() instanceof IKineticRotor,
            ROTOR_SLOT, ROTOR_SLOT + 1
        );
    }

    public WaterKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_Water_Kinetic_Generator blockEntity) {
        super(mio_icif_menus.WATER_KINETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
            blockEntity != null ? blockEntity.getItemHandler() : null, null, DATA_COUNT, blockEntity);
        addCustomMoveRule(
            stack -> stack.getItem() instanceof IKineticRotor,
            ROTOR_SLOT, ROTOR_SLOT + 1
        );
    }

    public int getKinetic() { return data.get(0); }
    public int getMaxKinetic() { return data.get(1); }
    public boolean isGenerating() { return data.get(2) == 1; }
    public int getKineticOutput() { return data.get(3); }
    public int getBiomeType() { return data.get(4); }
    public boolean isInNaturalWater() { return data.get(5) == 1; }
    public int getRotorDurability() { return data.get(6); }
    public int getRotorMaxDurability() { return data.get(7); }

    /** @return 转子耐久度百分比(0-100)，无转子时返回 -1 */
    public int getRotorHealthPercent() {
        int maxDurability = getRotorMaxDurability();
        if (maxDurability <= 0) return -1;
        int percent = (int) ((long) getRotorDurability() * 100L / maxDurability);
        return Math.max(0, Math.min(100, percent));
    }

    public String getBiomeTypeName() {
        int biomeType = getBiomeType();
        return switch (biomeType) {
            case 1 -> "gui.mio_icif.water_kinetic.biome_river";
            case 2 -> "gui.mio_icif.water_kinetic.biome_ocean";
            default -> "gui.mio_icif.water_kinetic.biome_other";
        };
    }

    public String getStatusReason() {
        if (!isInNaturalWater()) return "gui.mio_icif.water_kinetic.not_in_water";
        if (getBiomeType() == 0) return "gui.mio_icif.water_kinetic.biome_other";
        return "gui.mio_icif.water_kinetic.unknown";
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity instanceof mio_icif_Water_Kinetic_Generator generator) {
            setSyncData(0, (int) generator.getKineticStorage().getKineticStored());
            setSyncData(1, (int) generator.getKineticStorage().getMaxKineticStored());
            setSyncData(2, generator.isGenerating() ? 1 : 0);
            setSyncData(3, generator.getCurrentKineticOutput());
            setSyncData(4, generator.getCurrentBiomeType().ordinal());
            setSyncData(5, generator.isInNaturalWater() ? 1 : 0);
            setSyncData(6, generator.getRotorDurability());
            setSyncData(7, generator.getRotorMaxDurability());
            var disc = generator.getDisc();
            var level = generator.getLevel();
            setSyncData(8, disc == null ? 0 : disc.diameter());
            setSyncData(9, generator.getSource().ordinal());
            setSyncData(10, disc == null ? 0 : (int) Math.round(disc.meanSpeed() * 1000));
            setSyncData(11, disc == null ? 0 : disc.waterCells());
            setSyncData(12, disc == null ? 0 : disc.discCells());
            setSyncData(13, (int) Math.round(generator.getWakeFactor() * 1000));
            setSyncData(14, level == null ? 0 : level.getMoonPhase());
            setSyncData(15, level == null ? 0 : (int) Math.floorMod(level.getDayTime(), 24000L));
            setSyncData(16, generator.getMeanOutput());
            setSyncData(17, level == null ? 0 : (int) Math.round(1000 * com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.WaterPhysics.biomeSpeed(
                level, generator.getBlockPos(), generator.getSource())));
            for (int i = 0; i < com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.WaterPhysics.PACKED; i++)
                setSyncData(18 + i, disc == null ? 0 : com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.WaterPhysics.pack(disc.cells(), i));
        }
        super.broadcastChanges();
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, ROTOR_SLOT, 10, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (!(stack.getItem() instanceof IKineticRotor)) return false;
                return !stack.is(com.miophas.singularity_iteration.common.registry.mio_icif_items.ROTOR_WOOD.get());
            }
        });
    }

    // 0.1.11.18: the 248 x 204 rotor cross-section screen
    public static final int INVENTORY_X = 44, INVENTORY_Y = 122, HOTBAR_Y = 180;

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

    public int value(int i) { return data.get(i); }
}
