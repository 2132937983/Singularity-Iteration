package com.miophas.singularity_iteration.common.blockentity.build;

import com.miophas.singularity_iteration.core.api.item.IStorageBlock;

import com.miophas.singularity_iteration.common.block.build.mio_icif_storage_box;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.menu.storage.StorageBoxMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_storage_box_entity extends BlockEntity implements Container, MenuProvider, IStorageBlock {

    private MachineItemHandler itemHandler;
    private int slotCount;
    private String storageType = "wood"; // 存储箱类型缓存

    public mio_icif_storage_box_entity(BlockPos pos, BlockState state, int slotCount) {
        super(mio_icif_block_entities.STORAGE_BOX_ENTITY_TYPE.get(), pos, state);
        this.slotCount = slotCount;
        this.storageType = getStorageTypeFromBlockState(state);
        SlotLayout layout = SlotLayout.builder().extra(slotCount).build();
        this.itemHandler = new MachineItemHandler(layout) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return acceptsStack(stack) && super.isItemValid(slot, stack);
            }
        };
    }

    public mio_icif_storage_box_entity(BlockPos pos, BlockState state) {
        this(pos, state, getSlotCountFromBlock(state));
    }

    /**
     * 从方块状态获取存储箱类型（根据方块注册名称）
     */
    private static String getStorageTypeFromBlockState(BlockState state) {
        if (state.getBlock() instanceof mio_icif_storage_box box) {
            // 首先尝试从方块实例获取类型
            String type = box.getStorageType();
            if (!type.isEmpty()) {
                return type;
            }
            
            // 回退：从注册名称解析
            var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(box);
            if (key != null) {
                String path = key.getPath();
                if (path.contains("wood")) return "wood";
                if (path.contains("bronze")) return "bronze";
                if (path.contains("iron")) return "iron";
                if (path.contains("adviron")) return "adviron";
                if (path.contains("iridium")) return "iridium";
                if (path.contains("titanium")) return "titanium";
            }
        }
        return "wood";
    }

    private static int getSlotCountFromBlock(BlockState state) {
        if (state.getBlock() instanceof mio_icif_storage_box box) {
            return box.getSlotCount();
        }
        return 27;
    }

    @Override
    public int getContainerSize() {
        return slotCount;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot >= 0 && slot < itemHandler.getSlots()) {
            return itemHandler.getStackInSlot(slot);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int toRemove = Math.min(amount, stack.getCount());
        ItemStack result = stack.copyWithCount(toRemove);
        stack.shrink(toRemove);
        if (stack.isEmpty()) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < itemHandler.getSlots()) {
            itemHandler.setStackInSlot(slot, stack);
            if (stack.getCount() > getMaxStackSize()) {
                stack.setCount(getMaxStackSize());
            }
            setChanged();
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null) return false;
        if (level.getBlockEntity(worldPosition) != this) return false;
        return player.canInteractWithBlock(worldPosition, 4.0);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    public CompoundTag saveToTag(CompoundTag tag) {
        tag.put("Items", itemHandler.serializeNBT(level != null ? level.registryAccess() : null));
        return tag;
    }

    public void loadFromTag(CompoundTag tag) {
        if (tag.contains("Items")) {
            itemHandler.deserializeNBT(level != null ? level.registryAccess() : null, tag.getCompound("Items"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("SlotCount", slotCount);
        tag.putString("StorageType", storageType);
        tag.put("Items", itemHandler.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("SlotCount")) {
            int savedSlotCount = tag.getInt("SlotCount");
            if (savedSlotCount != slotCount) {
                SlotLayout layout = SlotLayout.builder().extra(savedSlotCount).build();
                this.itemHandler = new MachineItemHandler(layout) {
                    @Override
                    protected void onContentsChanged(int slot) {
                        setChanged();
                    }
                };
                this.slotCount = savedSlotCount;
            }
        }
        if (tag.contains("StorageType")) {
            this.storageType = tag.getString("StorageType");
        }
        // 注意：不根据槽位数量推断类型，因为青铜和铁质都是45槽
        // 旧存档如果没有StorageType，保持默认值"wood"
        if (tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
    }

    @Override
    public Component getDisplayName() {
 // 优先使用存的存储箱类型
        return Component.translatable("container.mio_icif.storage_box." + this.storageType);
    }

    /**
     * 获取存储箱类型标识
     */
    public String getStorageType() {
        return this.storageType;
    }

    /**
     * 从方块状态获取存储箱类型（用于初始化和同步）
     */
    @SuppressWarnings("unused")
    private String getStorageTypeName(mio_icif_storage_box box) {
        if (level != null) {
            var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(box);
            String path = key.getPath();
            if (path.contains("wood")) return "wood";
            if (path.contains("bronze")) return "bronze";
            if (path.contains("iron")) return "iron";
            if (path.contains("adviron")) return "adviron";
            if (path.contains("iridium")) return "iridium";
            if (path.contains("titanium")) return "titanium";
        }
        return this.storageType;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StorageBoxMenu(
                containerId, playerInventory, this);
    }

    public MachineItemHandler getItemHandler() {
        return itemHandler;
    }

    /**
     * A filled storage box (its contents travel in the item) cannot go into another one: boxes
     * nested in boxes grew the item data without bound and could exceed the network packet limit.
     */
    public static boolean acceptsStack(ItemStack stack) {
        return !(stack.getItem() instanceof net.minecraft.world.item.BlockItem block
            && block.getBlock() instanceof com.miophas.singularity_iteration.common.block.build.mio_icif_storage_box
            && stack.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA));
    }
}
