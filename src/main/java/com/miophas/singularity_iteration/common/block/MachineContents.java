package com.miophas.singularity_iteration.common.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Items stored inside a machine item (0.1.9.1). The SI wrench keeps the inventory of a machine,
 * upgrade plugins included, in the item's block-entity data ({@link MachineItemData}). This class
 * reads those items back, so that a machine used as a crafting material returns them
 * ({@link MachineBlockItem}) instead of losing them.
 *
 * <p>Inventory formats in the data: {@code MachineItemHandler} ({@code {Items:[{Slot, Item:{...}}]}}),
 * the NeoForge item handler ({@code {Items:[{Slot, id, count}]}}) and {@code ContainerHelper}
 * ({@code Items:[{Slot, id, count}]}), under the keys the machines use for their inventory.
 * Filters and ghost items are stored under other keys and never count.
 */
public final class MachineContents {
    private MachineContents() {}

    /**
     * Inventory keys of the SI machines (the keys {@link MachineItemData} strips for a settings-only copy).
     * 0.1.10.16: with {@code rci_reserve}, the coolant reserve of a reactor coolant injector (a list of
     * item stacks, 0.1.10.14), which was stripped but not returned, so a wrenched injector used as a
     * crafting material lost it.
     */
    static final String[] KEYS = {"inventory", "Inventory", "Items", "items", "ItemHandler", "rci_reserve"};

    /** Registry lookup on the client (set by the client setup; the server uses the running server). */
    public static volatile Supplier<HolderLookup.Provider> clientLookup = () -> null;

    /** True when the stack's block-entity data holds at least one inventory entry (no parsing). */
    public static boolean has(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null || data.isEmpty()) return false;
        @SuppressWarnings("deprecation") CompoundTag tag = data.getUnsafe();
        for (String key : KEYS) {
            ListTag list = list(tag.get(key));
            if (list != null && !list.isEmpty()) return true;
        }
        return false;
    }

    /** The stored items, merged into full stacks; empty when there are none or no registry is at hand. */
    public static List<ItemStack> of(ItemStack stack, @Nullable HolderLookup.Provider lookup) {
        if (!has(stack)) return List.of();
        if (lookup == null) lookup = lookup();
        if (lookup == null) return List.of();
        @SuppressWarnings("deprecation") CompoundTag tag = stack.get(DataComponents.BLOCK_ENTITY_DATA).getUnsafe();
        List<ItemStack> out = new ArrayList<>();
        for (String key : KEYS) {
            ListTag list = list(tag.get(key));
            if (list == null) continue;
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                CompoundTag item = entry.contains("Item", Tag.TAG_COMPOUND) ? entry.getCompound("Item") : entry;
                if (!item.contains("id", Tag.TAG_STRING)) continue;
                ItemStack parsed = ItemStack.parseOptional(lookup, item);
                if (!parsed.isEmpty()) merge(out, parsed);
            }
        }
        return out;
    }

    @Nullable
    private static ListTag list(@Nullable Tag tag) {
        if (tag instanceof CompoundTag compound && compound.contains("Items", Tag.TAG_LIST)) return compound.getList("Items", Tag.TAG_COMPOUND);
        if (tag instanceof ListTag list && (list.isEmpty() || list.getElementType() == Tag.TAG_COMPOUND)) return list;
        return null;
    }

    private static void merge(List<ItemStack> out, ItemStack stack) {
        for (ItemStack have : out) {
            if (stack.isEmpty()) return;
            if (!ItemStack.isSameItemSameComponents(have, stack) || have.getCount() >= have.getMaxStackSize()) continue;
            int move = Math.min(stack.getCount(), have.getMaxStackSize() - have.getCount());
            have.grow(move);
            stack.shrink(move);
        }
        while (!stack.isEmpty()) out.add(stack.split(stack.getMaxStackSize()));
    }

    @Nullable
    private static HolderLookup.Provider lookup() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) return server.registryAccess();
        return clientLookup.get();
    }
}
