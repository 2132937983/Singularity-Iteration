package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.block.producer.ResinCollectorBlock;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.common.menu.producer.ResinCollectorMenu;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.block.IRubberWood;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Unpowered one-slot collector. Full storage never consumes a resin spot. */
public class ResinCollectorBlockEntity extends BlockEntity implements MenuProvider {
    private final MachineItemHandler items = createInventory(this::inventoryChanged);

    public ResinCollectorBlockEntity(BlockPos pos, BlockState state) { super(mio_icif_block_entities.RESIN_COLLECTOR.get(), pos, state); }
    public static MachineItemHandler createInventory(Runnable changed) {
        return new MachineItemHandler(SlotLayout.builder().extra(1).build()) {
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return slot == 0 && (stack.isEmpty() || stack.is(mio_icif_resources.HARZ.get()));
            }
            @Override public void setStackInSlot(int slot, ItemStack stack) {
                if (isItemValid(slot, stack) && stack.getCount() <= getSlotLimit(slot)) super.setStackInSlot(slot, stack);
            }
            @Override protected void onContentsChanged(int slot) { changed.run(); }
        };
    }
    public MachineItemHandler getItemHandler() { return items; }
    private void inventoryChanged() {
        setChanged();
        if (level != null && !level.isClientSide && !isRemoved() && level.getBlockState(worldPosition).is(getBlockState().getBlock()))
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, ResinCollectorBlockEntity collector) {
        // Covers chunk loads and neighbor changes that external blocks do not notify.
        if ((level.getGameTime() + pos.asLong()) % 20 == 0) collector.collectResin();
    }
    public boolean collectResin() {
        if (level == null || level.isClientSide || isRemoved()) return false;
        BlockState own = level.getBlockState(worldPosition);
        if (!(own.getBlock() instanceof ResinCollectorBlock)) return false;
        Direction front = own.getValue(ResinCollectorBlock.FACING);
        BlockPos logPos = worldPosition.relative(front.getOpposite());
        if (!level.hasChunkAt(logPos)) return false;
        BlockState log = level.getBlockState(logPos);
        if (!(log.getBlock() instanceof IRubberWood wood) || !log.hasProperty(ResinCollectorBlock.FACING)
                || log.getValue(ResinCollectorBlock.FACING) != front || !wood.isTappable(log) || !wood.hasResin(log)) return false;
        ItemStack resin = new ItemStack(mio_icif_resources.HARZ.get());
        if (!items.insertItem(0, resin, true).isEmpty()) return false;
        // Server-thread operation: clear the wet state once before notifying storage observers.
        if (!level.setBlock(logPos, wood.withResin(log, false), 3)) return false;
        ItemStack remaining = items.insertItem(0, resin, false);
        if (!remaining.isEmpty()) {
            level.setBlock(logPos, log, 3);
            return false;
        }
        return true;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("Inventory", items.serializeNBT(provider));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("Inventory")) items.deserializeNBT(provider, tag.getCompound("Inventory"));
    }
    @Override public Component getDisplayName() { return Component.translatable("block.mio_icif.producer.block_resin_collector"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ResinCollectorMenu(id, inventory, this); }
}
