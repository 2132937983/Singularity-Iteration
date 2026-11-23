package com.miophas.singularity_iteration.common.menu.storage;

import com.miophas.singularity_iteration.common.blockentity.batbox.TesuControllerBlockEntity;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.core.runtime.energy.DirectItemCharging;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.157: the TESU controller's screen data, laid out as the GESU core's (charge and discharge slots, armour, inventory).
 * Data: 0-1 energy, 2-3 capacity, 4-5 output rate (each a long as high and low int), 6 input ports, 7 output ports, 8 formed,
 * 9-11 the first wrong position, 12 what belongs there (the part's ordinal + 1; 0 none), 13 wireless ports (0.1.11.158).
 */
@SuppressWarnings("null")
public class TesuControllerMenu extends mio_icif_base_menu {
    private final ContainerData data = new SimpleContainerData(14);
    private final Container container;
    @Nullable private final TesuControllerBlockEntity blockEntity;

    public TesuControllerMenu(int containerId, Inventory playerInventory) { this(containerId, playerInventory, null); }

    public TesuControllerMenu(int containerId, Inventory playerInventory, @Nullable TesuControllerBlockEntity blockEntity) {
        super(mio_icif_menus.TESU_CONTROLLER_MENU_TYPE.get(), containerId);
        this.blockEntity = blockEntity;
        this.container = blockEntity != null ? blockEntity : new SimpleContainer(2);
        this.addDataSlots(data);
        this.addSlot(new Slot(container, TesuControllerBlockEntity.CHARGE_SLOT, 56, 17) {
            @Override public boolean mayPlace(ItemStack stack) { return isChargeable(stack); }
            @Override public int getMaxStackSize(ItemStack stack) { return 1; }
        });
        this.addSlot(new Slot(container, TesuControllerBlockEntity.DISCHARGE_SLOT, 56, 53) {
            @Override public boolean mayPlace(ItemStack stack) { return isBattery(stack); }
            @Override public int getMaxStackSize(ItemStack stack) { return stack.is(net.minecraft.world.item.Items.REDSTONE) ? 64 : 1; }
        });
        markMachineSlots(2);
        EquipmentSlot[] armor = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        for (int i = 0; i < 4; i++) {
            EquipmentSlot type = armor[i];
            this.addSlot(new Slot(playerInventory, 36 + i, 8 + i * 18, 84) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof ArmorItem a && a.getEquipmentSlot() == type; }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        markPlayerSlots(4);
        addPlayerInventory(playerInventory, 114, 172);
        markPlayerSlots(36);
    }

    private long wide(int i) { return ((long) data.get(i) << 32) | (data.get(i + 1) & 0xFFFFFFFFL); }

    public long getEnergy() { return wide(0); }
    public long getMaxEnergy() { return wide(2); }
    public long getOutputRate() { return wide(4); }
    public int getInputPorts() { return data.get(6); }
    public int getOutputPorts() { return data.get(7); }
    public int getWirelessPorts() { return data.get(13); }
    public boolean isFormed() { return data.get(8) != 0; }
    /** The first wrong position, or {@code null}. */
    @Nullable public BlockPos getWrongAt() { return data.get(12) == 0 ? null : new BlockPos(data.get(9), data.get(10), data.get(11)); }
    /** The ordinal of the part that belongs at the wrong position, or -1. */
    public int getExpectedPart() { return data.get(12) - 1; }

    public int getEnergyProgressPixels() {
        long max = getMaxEnergy();
        return max <= 0 ? 0 : (int) (getEnergy() * (double) ENERGY_BAR_WIDTH / max);
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override protected boolean isBattery(ItemStack stack) { return DirectItemCharging.canDischarge(stack); }
    @Override protected boolean isChargeable(ItemStack stack) { return DirectItemCharging.canCharge(stack); }
    @Override protected int getChargeSlotIndex() { return 0; }
    @Override protected int getBatterySlotIndex() { return 1; }

    @Override
    public void broadcastChanges() {
        if (blockEntity != null) {
            long[] wide = {blockEntity.getStoredEnergy(), blockEntity.getEnergyCapacity(), blockEntity.outputRate()};
            for (int k = 0; k < 3; k++) {
                data.set(2 * k, (int) (wide[k] >> 32));
                data.set(2 * k + 1, (int) wide[k]);
            }
            data.set(6, blockEntity.inputCount());
            data.set(7, blockEntity.outputCount());
            data.set(8, blockEntity.isStructureComplete() ? 1 : 0);
            BlockPos wrong = blockEntity.isStructureComplete() ? null : blockEntity.wrongAt();
            data.set(9, wrong == null ? 0 : wrong.getX());
            data.set(10, wrong == null ? 0 : wrong.getY());
            data.set(11, wrong == null ? 0 : wrong.getZ());
            data.set(12, wrong == null || blockEntity.expected() == null ? 0 : blockEntity.expected().ordinal() + 1);
            data.set(13, blockEntity.wirelessCount());
        }
        super.broadcastChanges();
    }
}
