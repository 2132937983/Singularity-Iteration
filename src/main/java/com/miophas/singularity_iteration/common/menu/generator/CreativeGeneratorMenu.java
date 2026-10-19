// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.block.generator.mio_icif_block_creative_generator;
import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_creative_generator;
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
 * 0.1.10.11: screen of the Creative Generator (no slots). The output, the switch and the EU sent in
 * the last tick travel as nine 16-bit data slots; a typed output goes through
 * {@code CreativeGeneratorPacket}, the buttons through {@link #clickMenuButton}. Every change is
 * checked on the server: only a creative player or an operator may make it.
 */
@SuppressWarnings("null")
public class CreativeGeneratorMenu extends AbstractContainerMenu {
    /** Button ids: presets 0..PRESETS.length-1, then x10, /10, switch. */
    public static final long[] PRESETS = {32L, 128L, 512L, 2048L, 8192L, 32768L, 131072L, 1_000_000L};
    public static final int BUTTON_TIMES_TEN = PRESETS.length;
    public static final int BUTTON_DIVIDE_TEN = PRESETS.length + 1;
    public static final int BUTTON_SWITCH = PRESETS.length + 2;

    @Nullable private final mio_icif_creative_generator generator;
    private final ContainerData data;
    @Nullable private BlockPos pos;

    public CreativeGeneratorMenu(int containerId, Inventory inventory, @Nullable RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, null, new SimpleContainerData(mio_icif_creative_generator.DATA_COUNT));
        if (buf != null) pos = buf.readBlockPos();
    }

    public CreativeGeneratorMenu(int containerId, Inventory inventory, @Nullable mio_icif_creative_generator generator, ContainerData data) {
        super(mio_icif_menus.CREATIVE_GENERATOR_MENU_TYPE.get(), containerId);
        checkContainerDataCount(data, mio_icif_creative_generator.DATA_COUNT);
        this.generator = generator;
        this.data = data;
        if (generator != null) pos = generator.getBlockPos();
        addDataSlots(data);
    }

    /**
     * Parses a typed output: digits with optional {@code ,}, {@code _} or spaces and an optional
     * suffix k (10^3), M (10^6), G (10^9) or T (10^12), also with one decimal part ("2.5M").
     * Returns -1 for text that is not a number or exceeds {@link mio_icif_creative_generator#MAX_RATE}.
     */
    public static long parseRate(String text) {
        if (text == null) return -1;
        String s = text.replace(",", "").replace("_", "").replace(" ", "").trim();
        if (s.isEmpty()) return -1;
        long scale = 1;
        char last = s.charAt(s.length() - 1);
        switch (Character.toLowerCase(last)) {
            case 'k' -> scale = 1_000L;
            case 'm' -> scale = 1_000_000L;
            case 'g' -> scale = 1_000_000_000L;
            case 't' -> scale = 1_000_000_000_000L;
            default -> { }
        }
        if (scale != 1) s = s.substring(0, s.length() - 1);
        if (s.isEmpty() || !s.matches("\\d{1,13}(\\.\\d{1,12})?")) return -1;
        try {
            var value = new java.math.BigDecimal(s).multiply(java.math.BigDecimal.valueOf(scale));
            if (value.compareTo(java.math.BigDecimal.valueOf(mio_icif_creative_generator.MAX_RATE)) > 0) return -1;
            return value.setScale(0, java.math.RoundingMode.DOWN).longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            return -1;
        }
    }

    @Nullable public BlockPos pos() { return pos; }
    public long rate() { return mio_icif_creative_generator.join(data, 0); }
    public boolean enabled() { return data.get(4) != 0; }
    public long lastSent() { return mio_icif_creative_generator.join(data, 5); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (generator == null || !mio_icif_block_creative_generator.allowed(player)) return false;
        long rate = generator.rate();
        if (id >= 0 && id < PRESETS.length) generator.configure(PRESETS[id], true);
        else if (id == BUTTON_TIMES_TEN) generator.configure(rate > mio_icif_creative_generator.MAX_RATE / 10 ? mio_icif_creative_generator.MAX_RATE : Math.max(1, rate * 10), generator.enabled());
        else if (id == BUTTON_DIVIDE_TEN) generator.configure(rate / 10, generator.enabled());
        else if (id == BUTTON_SWITCH) generator.configure(rate, !generator.enabled());
        else return false;
        return true;
    }

    /** Server side: a validated typed output. */
    public void applyRate(Player player, long rate) {
        if (generator != null && mio_icif_block_creative_generator.allowed(player)) generator.configure(rate, generator.enabled());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (generator == null) return true;
        if (generator.isRemoved() || generator.getLevel() != player.level() || !mio_icif_block_creative_generator.allowed(player)) return false;
        BlockPos at = generator.getBlockPos();
        return player.distanceToSqr(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5) <= 64.0;
    }
}
