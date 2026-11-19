// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.reactor;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator;
import com.miophas.singularity_iteration.common.item.component.ReactorLayout;
import com.miophas.singularity_iteration.common.item.reactor.ReactorConfigCardItem;
import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.76: the reactor config card on the reactor screens (generator and fluid / steam). Each screen has a card slot and
 * three buttons:
 * <ul>
 *   <li>import ({@link #IMPORT}): the layout of the reactor is written on the card (the old layout on it is replaced);</li>
 *   <li>export ({@link #EXPORT}): the layout of the card becomes the reactor's layout guide (with Shift: {@link #CLEAR});
 *       the screen shows each guided part as a faded item in its empty slot and tints a slot that holds another part;</li>
 *   <li>fill ({@link #FILL}): each empty guided slot takes one matching part from the player (main inventory, hotbar,
 *       offhand; matched by item id only, so used rods and hot vents count).</li>
 * </ul>
 * The guide is saved in the reactor, so one card can guide any number of reactors. The card slot belongs to the menu:
 * the card goes back to the player when the screen closes.
 *
 * <p>The screen learns the guide and the result of the last button through {@link Data} (container data, two 16-bit
 * halves per value).
 */
public final class ReactorLayoutGuide {
    private ReactorLayoutGuide() {}

    /** Menu button ids (the generator screen uses 0..4 and 10..13). */
    public static final int IMPORT = 20, EXPORT = 21, CLEAR = 22, FILL = 23;

    /** Result codes of the last button (0 = none yet). */
    public static final int IMPORTED = 1, EXPORTED = 2, CLEARED = 3, FILLED = 4,
        NO_CARD = 5, BLANK_CARD = 6, EMPTY_REACTOR = 7, NO_GUIDE = 8;

    /** Inventory slots searched by the fill: main inventory and hotbar (0..35), then the offhand (40). */
    private static final int MAIN_SLOTS = 36, OFFHAND_SLOT = 40;

    public static boolean isButton(int id) { return id >= IMPORT && id <= FILL; }

    /** Runs a button on the server. Returns false for an id that is not a guide button. */
    public static boolean click(int id, Player player, mio_icif_nuclear_reactor_generator reactor, Container cardSlot, Data data) {
        if (!isButton(id)) return false;
        ItemStack card = cardSlot.getItem(0);
        int[] result = switch (id) {
            case IMPORT -> importLayout(reactor, card);
            case EXPORT -> exportLayout(reactor, card);
            case CLEAR -> reactor.hasLayoutGuide() ? clear(reactor) : new int[] {NO_GUIDE, 0, 0, 0};
            default -> reactor.hasLayoutGuide() ? fillReport(reactor, player.getInventory()) : new int[] {NO_GUIDE, 0, 0, 0};
        };
        if (result[0] == IMPORTED) cardSlot.setChanged();
        data.report(result);
        if (reactor.getLevel() != null) {
            boolean ok = result[0] <= FILLED;
            reactor.getLevel().playSound(null, reactor.getBlockPos(), mio_icif_sounds.UI_CLICK.get(), SoundSource.BLOCKS, 0.6F, ok ? 1.25F : 0.6F);
        }
        return true;
    }

    private static int[] importLayout(mio_icif_nuclear_reactor_generator reactor, ItemStack card) {
        if (!ReactorConfigCardItem.isCard(card)) return new int[] {NO_CARD, 0, 0, 0};
        ReactorLayout layout = ReactorLayout.capture(reactor.getReactorItemHandler());
        if (layout.isEmpty()) return new int[] {EMPTY_REACTOR, 0, 0, 0};
        card.set(mio_icif_data_components.REACTOR_LAYOUT.get(), layout);
        return new int[] {IMPORTED, layout.size(), 0, 0};
    }

    private static int[] exportLayout(mio_icif_nuclear_reactor_generator reactor, ItemStack card) {
        if (!ReactorConfigCardItem.isCard(card)) return new int[] {NO_CARD, 0, 0, 0};
        ReactorLayout layout = ReactorConfigCardItem.layout(card);
        if (layout == null) return new int[] {BLANK_CARD, 0, 0, 0};
        reactor.setLayoutGuide(layout);
        int need = layout.columnsNeeded();
        return new int[] {EXPORTED, layout.size(), need > reactor.getAvailableColumns() ? need : 0, 0};
    }

    private static int[] clear(mio_icif_nuclear_reactor_generator reactor) {
        reactor.setLayoutGuide(null);
        return new int[] {CLEARED, 0, 0, 0};
    }

    private static int[] fillReport(mio_icif_nuclear_reactor_generator reactor, Inventory inventory) {
        ResourceLocation[] guide = reactor.layoutGuide().toArray();
        Fill fill = fill(reactor.getReactorItemHandler(), reactor.getAvailableColumns(), guide, inventory);
        return new int[] {FILLED, fill.placed, fill.missing, fill.blocked};
    }

    /**
     * Result of {@link #fill}: parts placed, guided slots still empty for want of a part (or because the reactor refused
     * it), and guided slots in columns the reactor does not have (it needs more chambers).
     */
    public record Fill(int placed, int missing, int blocked) {}

    /**
     * Puts one matching part from {@code inventory} into each empty guided slot. A slot that holds any item is left as
     * it is. Parts are matched by item id only; the stack moved keeps its own data (durability, heat, fuel left).
     */
    public static Fill fill(IItemHandler reactor, int columns, ResourceLocation[] guide, Inventory inventory) {
        int placed = 0, missing = 0, blocked = 0;
        for (int slot = 0; slot < Math.min(guide.length, Math.min(ReactorLayout.SLOTS, reactor.getSlots())); slot++) {
            ResourceLocation id = guide[slot];
            if (id == null) continue;
            if (slot % ReactorLayout.COLUMNS >= columns) { blocked++; continue; }
            if (!reactor.getStackInSlot(slot).isEmpty()) continue;
            Item item = ReactorLayout.item(id);
            int from = item == Items.AIR ? -1 : find(inventory, item);
            if (from < 0) { missing++; continue; }
            ItemStack source = inventory.getItem(from);
            ItemStack one = source.copyWithCount(1);
            if (!reactor.insertItem(slot, one, true).isEmpty()) { missing++; continue; }
            reactor.insertItem(slot, one, false);
            source.shrink(1);
            placed++;
        }
        if (placed > 0) inventory.setChanged();
        return new Fill(placed, missing, blocked);
    }

    /** The first inventory slot (hotbar and main inventory, then offhand) with this item, or -1. */
    private static int find(Inventory inventory, Item item) {
        for (int i = 0; i < MAIN_SLOTS; i++) if (inventory.getItem(i).is(item)) return i;
        return inventory.getItem(OFFHAND_SLOT).is(item) ? OFFHAND_SLOT : -1;
    }

    /** The card slot of a reactor screen: one reactor config card. */
    public static final class CardSlot extends Slot {
        public CardSlot(Container container, int x, int y) { super(container, 0, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return ReactorConfigCardItem.isCard(stack); }
        @Override public int getMaxStackSize() { return 1; }
    }

    /**
     * Container data of the guide: per reactor slot the guided item (raw item id + 1, 0 = none), the reactor's columns,
     * and the last button result (sequence, code, three numbers). On the server it reads the reactor; on the client it
     * holds what the server sent.
     */
    public static final class Data implements ContainerData {
        public static final int COLUMNS = ReactorLayout.SLOTS, SEQ = COLUMNS + 1, CODE = SEQ + 1, A = CODE + 1, B = A + 1, C = B + 1,
            COUNT = C + 1;
        private final @Nullable mio_icif_nuclear_reactor_generator reactor;
        private final int[] values = new int[COUNT];

        public Data(@Nullable mio_icif_nuclear_reactor_generator reactor) { this.reactor = reactor; }

        @Override public int get(int index) {
            if (index < 0 || index >= COUNT) return 0;
            if (reactor != null && index < ReactorLayout.SLOTS) {
                ResourceLocation id = reactor.layoutGuideAt(index);
                return id == null ? 0 : BuiltInRegistries.ITEM.getOptional(id).map(item -> BuiltInRegistries.ITEM.getId(item) + 1).orElse(0);
            }
            if (reactor != null && index == COLUMNS) return reactor.getAvailableColumns();
            return values[index];
        }

        @Override public void set(int index, int value) { if (index >= 0 && index < COUNT) values[index] = value; }

        @Override public int getCount() { return COUNT; }

        void report(int[] result) {
            values[SEQ]++;
            values[CODE] = result[0];
            values[A] = result[1];
            values[B] = result[2];
            values[C] = result[3];
        }

        /** The guided item of a reactor slot, or null (client: from the synced raw id). */
        public @Nullable Item guided(int slot) {
            int raw = get(slot);
            if (raw <= 0) return null;
            Item item = BuiltInRegistries.ITEM.byId(raw - 1);
            return item == Items.AIR ? null : item;
        }

        public boolean hasGuide() {
            for (int i = 0; i < ReactorLayout.SLOTS; i++) if (get(i) > 0) return true;
            return false;
        }

        public int columns() { return Math.max(3, Math.min(ReactorLayout.COLUMNS, get(COLUMNS))); }
    }
}
