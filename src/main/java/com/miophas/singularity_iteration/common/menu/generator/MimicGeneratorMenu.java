// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.energy.StirlingPhysics;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_generator_menu;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.17: base of the generator menus of the plant mimic screens (248 x 204): the player inventory at (44, 122), the
 * hotbar at y 180, and the synced values filled from {@link #values()} on the server.
 */
@SuppressWarnings("null")
public abstract class MimicGeneratorMenu extends mio_icif_generator_menu {
    public static final int INVENTORY_X = 44, INVENTORY_Y = 162, HOTBAR_Y = 220;

    protected MimicGeneratorMenu(MenuType<?> type, int containerId, int slots, Inventory inv, @Nullable IItemHandler items, int dataCount,
                                 @Nullable Object blockEntity) {
        super(type, containerId, slots, inv, items, null, dataCount, blockEntity);
    }

    public int value(int i) { return data.get(i); }

    /** The values to sync (server side, with the block entity). */
    protected abstract int[] values();

    @Override
    public void broadcastChanges() {
        if (blockEntity != null) {
            int[] v = values();
            for (int i = 0; i < v.length && i < data.getCount(); i++) data.set(i, v[i]);
        }
        super.broadcastChanges();
    }

    @Override protected int getPlayerInventoryY() { return INVENTORY_Y; }
    @Override protected int getPlayerHotbarY() { return HOTBAR_Y; }

    @Override
    protected void addPlayerInventory(Inventory playerInventory, int startY, int hotbarY) {
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j)
            this.addSlot(new Slot(playerInventory, j + i * 9 + 9, INVENTORY_X + j * 18, startY + i * 18));
        for (int i = 0; i < 9; ++i) this.addSlot(new Slot(playerInventory, i, INVENTORY_X + i * 18, hotbarY));
    }

    // ---- shared value blocks ------------------------------------------------------------------------------------------

    /** Heater °C x10, cooler °C x10, factor x1000, heat flow HU/t x10, water cooled, ambient °C x10. */
    public static int[] head(StirlingPhysics.Head h) {
        return new int[]{(int) Math.round(h.hot() * 10), (int) Math.round(h.cold() * 10), (int) Math.round(h.factor() * 1000),
            (int) Math.round(h.flow() * 10), h.water() ? 1 : 0, (int) Math.round(h.ambientTemperature() * 10)};
    }

    /** What drives a kinetic generator: 0 nothing, 1 wind or water, 2 another KU source; and its shaft speed. */
    public static int[] source(@Nullable Level level, BlockPos pos, Direction facing) {
        if (level == null) return new int[]{0, 0};
        var at = pos.relative(facing);
        if (!level.isLoaded(at)) return new int[]{0, 0};
        var be = level.getBlockEntity(at);
        if (be == null) return new int[]{0, 0};
        int kind = be instanceof com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Wind_Kinetic_Generator
            || be instanceof com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Water_Kinetic_Generator ? 1 : 2;
        IMioIcifCapabilities.IKineticStorage ku = level.getCapability(IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, at, facing.getOpposite());
        if (ku == null) return new int[]{0, 0};
        return new int[]{kind, Math.max(0, ku.getRPM())};
    }

    public static Direction facing(Object blockEntity) {
        if (blockEntity instanceof net.minecraft.world.level.block.entity.BlockEntity be && be.getBlockState().hasProperty(BlockStateProperties.FACING))
            return be.getBlockState().getValue(BlockStateProperties.FACING);
        return Direction.NORTH;
    }
}
