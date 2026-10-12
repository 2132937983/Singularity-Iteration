// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/** Blueprint Builder GUI (0.1.8.0): schematic slot, battery slot and the synced builder state. */
@SuppressWarnings("null")
public class BlueprintBuilderMenu extends mio_icif_machine_menu {
    public static final int GUI_HEIGHT = 234;
    public static final int SCHEMATIC_X = 8, SCHEMATIC_Y = 20, BATTERY_X = 8, BATTERY_Y = 78;

    @Nullable private final BlockPos pos;

    public BlueprintBuilderMenu(int containerId, Inventory inventory, @Nullable RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, null, null, buf != null ? buf.readBlockPos() : null);
    }

    public BlueprintBuilderMenu(int containerId, Inventory inventory, BlueprintBuilderBlockEntity builder, ContainerData data) {
        this(containerId, inventory, builder, data, builder.getBlockPos());
    }

    private BlueprintBuilderMenu(int containerId, Inventory inventory, @Nullable BlueprintBuilderBlockEntity builder,
                                 @Nullable ContainerData data, @Nullable BlockPos pos) {
        super(BuilderRegistry.BUILDER_MENU.get(), containerId, BlueprintBuilderBlockEntity.SLOT_COUNT, inventory,
            builder != null ? builder.getItemHandler() : null, data, BlueprintBuilderBlockEntity.DATA_COUNT, builder);
        this.pos = pos;
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new SlotItemHandler(itemHandler, BlueprintBuilderBlockEntity.SCHEMATIC_SLOT, SCHEMATIC_X, SCHEMATIC_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return BlueprintReader.isCreateSchematic(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        addBatterySlot(BlueprintBuilderBlockEntity.BATTERY_SLOT, BATTERY_X, BATTERY_Y);
    }

    @Override protected int getPlayerInventoryY() { return 152; }
    @Override protected int getPlayerHotbarY() { return 210; }
    @Override protected int getBatterySlotIndex() { return BlueprintBuilderBlockEntity.BATTERY_SLOT; }
    @Override protected int getDataSlotCount() { return BlueprintBuilderBlockEntity.DATA_COUNT; }

    @Nullable public BlockPos pos() { return pos; }

    private int u16(int i) { return data.get(i) & 0xFFFF; }
    private int u32(int lo) { return u16(lo) | (u16(lo + 1) << 16); }

    public long energy() { return u16(0) | ((long) u16(1) << 16) | ((long) u16(2) << 32); }
    public int status() { return data.get(3); }
    public int placed() { return u32(4); }
    public int total() { return u32(6); }
    public int obstructed() { return u32(8); }
    public int missing() { return u32(10); }
    public int rotation() { return data.get(12); }
    public int mirror() { return data.get(13); }
    public int offset(int axis) { return (short) data.get(14 + axis); }
    public int fileIndex() { return data.get(17); }
    public int source() { return data.get(18); }
    public boolean preview() { return data.get(19) != 0; }
    public int drones() { return data.get(20); }
    public int doneCount() { return u32(21); }
}
