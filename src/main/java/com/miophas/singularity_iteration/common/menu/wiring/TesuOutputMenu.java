// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.menu.wiring;

import com.miophas.singularity_iteration.common.blockentity.batbox.TesuOutputPortBlockEntity;
import com.miophas.singularity_iteration.common.menu.generator.CreativeGeneratorMenu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.191: screen of the TESU output port (no slots), after the Creative Generator's: the output (EU/t), the switch, the
 * EU sent in the last tick, the voltage, whether the port is on a formed TESU and the TESU's energy travel as data slots
 * ({@link TesuOutputPortBlockEntity#DATA_COUNT}); a typed output goes through {@code TesuOutputPacket}, the buttons through
 * {@link #clickMenuButton}. Any player within reach may set it (it is not a creative block).
 */
@SuppressWarnings("null")
public class TesuOutputMenu extends AbstractContainerMenu {
    /** Button ids: the voltages' full packets 0..9 (IV..MAX), then x10, /10, switch. */
    public static final int VOLTAGES = TesuOutputPortBlockEntity.TIERS.length;
    public static final int BUTTON_TIMES_TEN = VOLTAGES, BUTTON_DIVIDE_TEN = VOLTAGES + 1, BUTTON_SWITCH = VOLTAGES + 2;

    @Nullable private final TesuOutputPortBlockEntity port;
    private final ContainerData data;
    @Nullable private BlockPos pos;

    public TesuOutputMenu(int containerId, Inventory inventory, @Nullable RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, null, new SimpleContainerData(TesuOutputPortBlockEntity.DATA_COUNT));
        if (buf != null) pos = buf.readBlockPos();
    }

    public TesuOutputMenu(int containerId, Inventory inventory, @Nullable TesuOutputPortBlockEntity port, ContainerData data) {
        super(mio_icif_menus.TESU_OUTPUT_MENU_TYPE.get(), containerId);
        checkContainerDataCount(data, TesuOutputPortBlockEntity.DATA_COUNT);
        this.port = port;
        this.data = data;
        if (port != null) pos = port.getBlockPos();
        addDataSlots(data);
    }

    /** A typed output (digits, k/M/G suffixes as on the Creative Generator), or -1 when it is no number or over the MAX packet. */
    public static long parseRate(String text) {
        long v = CreativeGeneratorMenu.parseRate(text);
        return v < 0 || v > TesuOutputPortBlockEntity.MAX_RATE ? -1 : v;
    }

    @Nullable public BlockPos pos() { return pos; }
    public long rate() { return TesuOutputPortBlockEntity.join(data, 0); }
    public boolean enabled() { return data.get(4) != 0; }
    public long lastSent() { return TesuOutputPortBlockEntity.join(data, 5); }
    public int tierIndex() { return data.get(9); }
    public boolean formed() { return data.get(10) != 0; }
    public long stored() { return TesuOutputPortBlockEntity.join(data, 11); }
    public long capacity() { return TesuOutputPortBlockEntity.join(data, 15); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (port == null) return false;
        long rate = port.rate();
        if (id >= 0 && id < VOLTAGES) port.configure(TesuOutputPortBlockEntity.tierOf(id).getPowerRating(), port.enabled());
        else if (id == BUTTON_TIMES_TEN) port.configure(rate > TesuOutputPortBlockEntity.MAX_RATE / 10 ? TesuOutputPortBlockEntity.MAX_RATE : Math.max(1, rate * 10), port.enabled());
        else if (id == BUTTON_DIVIDE_TEN) port.configure(rate / 10, port.enabled());
        else if (id == BUTTON_SWITCH) port.configure(rate, !port.enabled());
        else return false;
        return true;
    }

    /** Server side: a validated typed output. */
    public void applyRate(long rate) {
        if (port != null && rate >= 0) port.configure(rate, port.enabled());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        if (port == null) return true;
        if (port.isRemoved() || port.getLevel() != player.level()) return false;
        BlockPos at = port.getBlockPos();
        return player.distanceToSqr(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5) <= 64.0;
    }
}
