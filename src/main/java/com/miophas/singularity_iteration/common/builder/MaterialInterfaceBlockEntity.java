// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * Armory Material Interface (0.1.8.0): the material source of the builder drones.
 *
 * <p>It has 27 slots of its own (pipes, hoppers and players can fill them; it exposes them as
 * an item handler) and it reads the item handlers of the six adjacent blocks. Thus any storage
 * mod that exposes the NeoForge item handler capability (chests, drawers, AE2 or Refined
 * Storage interfaces, ...) supplies the drones without a dedicated integration. Adjacent
 * Material Interfaces and Blueprint Builders are skipped (no loops).
 */
@SuppressWarnings("null")
public class MaterialInterfaceBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOTS = 27;

    private final SimpleContainer items = new SimpleContainer(SLOTS) {
        @Override public void setChanged() { super.setChanged(); MaterialInterfaceBlockEntity.this.setChanged(); }
    };
    private final IItemHandler handler = new InvWrapper(items);

    public MaterialInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(BuilderRegistry.MATERIAL_INTERFACE_ENTITY.get(), pos, state);
    }

    public IItemHandler handler() { return handler; }
    public SimpleContainer container() { return items; }

    /** Number of {@code item} in the own slots and the adjacent storages. */
    public long count(Item item) {
        long n = 0;
        for (int i = 0; i < items.getContainerSize(); i++) if (plain(items.getItem(i), item)) n += items.getItem(i).getCount();
        for (IItemHandler h : neighbours()) {
            for (int s = 0; s < h.getSlots(); s++) {
                ItemStack st = h.getStackInSlot(s);
                if (plain(st, item)) n += st.getCount();
            }
        }
        return n;
    }

    /** Takes up to {@code max} of {@code item}: own slots first, then the adjacent storages. */
    public ItemStack extract(Item item, int max) {
        ItemStack out = ItemStack.EMPTY;
        int need = max;
        for (int i = 0; i < items.getContainerSize() && need > 0; i++) {
            ItemStack st = items.getItem(i);
            if (!plain(st, item)) continue;
            ItemStack taken = items.removeItem(i, need);
            out = merge(out, taken);
            need = max - out.getCount();
        }
        for (IItemHandler h : neighbours()) {
            for (int s = 0; s < h.getSlots() && need > 0; s++) {
                ItemStack st = h.getStackInSlot(s);
                if (!plain(st, item)) continue;
                ItemStack taken = h.extractItem(s, need, false);
                out = merge(out, taken);
                need = max - out.getCount();
            }
            if (need <= 0) break;
        }
        return out;
    }

    /**
     * Only plain stacks are building material: no changed components (no contents, name,
     * enchantment or custom data). A placed block would lose them.
     */
    static boolean plain(ItemStack stack, Item item) {
        return stack.is(item) && stack.isComponentsPatchEmpty();
    }

    private static ItemStack merge(ItemStack into, ItemStack add) {
        if (add.isEmpty()) return into;
        if (into.isEmpty()) return add.copy();
        into.grow(add.getCount());
        return into;
    }

    /** Puts {@code stack} back: own slots first, then the adjacent storages. Returns the rest. */
    public ItemStack insert(ItemStack stack) {
        ItemStack rest = ItemHandlerHelper.insertItemStacked(handler, stack, false);
        for (IItemHandler h : neighbours()) {
            if (rest.isEmpty()) break;
            rest = ItemHandlerHelper.insertItemStacked(h, rest, false);
        }
        return rest;
    }

    private java.util.List<IItemHandler> neighbours() {
        java.util.List<IItemHandler> out = new java.util.ArrayList<>(6);
        if (level == null) return out;
        for (Direction d : Direction.values()) {
            BlockPos at = worldPosition.relative(d);
            if (!level.isLoaded(at)) continue;
            BlockEntity be = level.getBlockEntity(at);
            if (be instanceof MaterialInterfaceBlockEntity || be instanceof BlueprintBuilderBlockEntity) continue;
            IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, at, d.getOpposite());
            if (h != null) out.add(h);
        }
        return out;
    }

    // ------------------------------------------------------------------ menu, save
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mio_icif.producer.block_material_interface");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return ChestMenu.threeRows(id, inventory, items);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.createTag(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.fromTag(tag.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND), registries);
    }
}
