package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_QuantumSolarPanel;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class QuantumSolarPanelMenu extends mio_icif_generator_menu {

    public static final int CHARGING_SLOT_1 = 0;
    public static final int CHARGING_SLOT_2 = 1;
    public static final int CHARGING_SLOT_3 = 2;
    public static final int CHARGING_SLOT_4 = 3;
    public static final int SLOT_COUNT = 4;

    private static final int CHARGING_SLOT_1_X = 52;
    private static final int CHARGING_SLOT_1_Y = 26;
    private static final int CHARGING_SLOT_2_X = 70;
    private static final int CHARGING_SLOT_2_Y = 26;
    private static final int CHARGING_SLOT_3_X = 88;
    private static final int CHARGING_SLOT_3_Y = 26;
    private static final int CHARGING_SLOT_4_X = 106;
    private static final int CHARGING_SLOT_4_Y = 26;

    private final ContainerLevelAccess access;
    public final mio_icif_QuantumSolarPanel blockEntity;

    public QuantumSolarPanelMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public QuantumSolarPanelMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_QuantumSolarPanel blockEntity) {
        super(mio_icif_menus.QUANTUM_SOLAR_PANEL_MENU_TYPE.get(), containerId, SLOT_COUNT,
            playerInventory,
            blockEntity != null ? blockEntity.getItemHandlerCapability(null) : null,
            null, 4, blockEntity);

        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;
    }

    @Override
    protected void addMachineSlots() {
        addChargingSlot(CHARGING_SLOT_1, CHARGING_SLOT_1_X, CHARGING_SLOT_1_Y);
        addChargingSlot(CHARGING_SLOT_2, CHARGING_SLOT_2_X, CHARGING_SLOT_2_Y);
        addChargingSlot(CHARGING_SLOT_3, CHARGING_SLOT_3_X, CHARGING_SLOT_3_Y);
        addChargingSlot(CHARGING_SLOT_4, CHARGING_SLOT_4_X, CHARGING_SLOT_4_Y);
    }

    protected void addChargingSlot(int index, int x, int y) {
        this.addSlot(new net.neoforged.neoforge.items.SlotItemHandler(itemHandler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return QuantumSolarPanelMenu.this.isBattery(stack);
            }

            @Override
            public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
                return true;
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                if (stack.getItem() == net.minecraft.world.item.Items.REDSTONE) return 64;
                return 1;
            }
        });
    }

    public boolean isGenerating() {
        return data.get(2) == 1;
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat;
    }

    public boolean isDaytimeGeneration() {
        return data.get(3) == 1;
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getMaxEnergy() {
        return data.get(1);
    }

    public int getEnergyProgressPixels() {
        int energy = getEnergy();
        int maxEnergy = getMaxEnergy();
        if (maxEnergy <= 0) return 0;
        return (int) ((energy * com.singularity_iteration.mio_icif.util.mio_icif_gui_global_variables.KINETIC_ENERGY_BAR2_WIDTH) / maxEnergy);
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
            this.setSyncData(3, blockEntity.getGenerationState() == mio_icif_QuantumSolarPanel.GenerationState.DAY ? 1 : 0);
        }
    }
}