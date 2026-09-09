package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.machine.MultiblockEnergyPart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

@SuppressWarnings("null")
public class mio_icif_large_fabricator_scrap_entity extends MultiblockEnergyPart {

    private static final long BUFFER_CAPACITY = 0L;
    private static final ICableTier MAX_TIER =
        MioIcifAPI.instance().getEnergyNetAPI().getCableTier("max");

    public static final int SCRAP_SLOT = 0;
    public static final int SLOT_COUNT = 1;
    public static final float SCRAP_PROCESS_THRESHOLD = 100000.0f;

    public static final int SCRAP_BONUS = 5000;
    public static final int SCRAPBOX_BONUS = 45000;
    public static final int THORIUM_SCRAP_BONUS = 1200000;

    private float scrapValue = 0.0f;
    protected final ItemStackHandler itemHandler;

    public mio_icif_large_fabricator_scrap_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.LARGE_FABRICATOR_SCRAP.get(), BUFFER_CAPACITY, 0, 0, MAX_TIER);
        this.itemHandler = new ItemStackHandler(SLOT_COUNT) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return isScrapItem(stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_large_fabricator_scrap_entity blockEntity) {
        if (level.isClientSide()) return;

        if (!blockEntity.isStructureCompleted() || blockEntity.getCorePosition() == null) {
            return;
        }

        blockEntity.processScrapSlot();
    }

    private void processScrapSlot() {
        if (scrapValue >= SCRAP_PROCESS_THRESHOLD) return;

        ItemStack scrapStack = itemHandler.getStackInSlot(SCRAP_SLOT);
        if (scrapStack.isEmpty()) return;

        if (scrapStack.is(mio_icif_normal.SCRAP.get())) {
            scrapValue += SCRAP_BONUS;
            scrapStack.shrink(1);
            setChanged();
        } else if (scrapStack.is(mio_icif_normal.SCRAPBOX.get())) {
            scrapValue += SCRAPBOX_BONUS;
            scrapStack.shrink(1);
            setChanged();
        } else if (scrapStack.is(mio_icif_resources.THORIUM_SCRAP.get())) {
            scrapValue += THORIUM_SCRAP_BONUS;
            scrapStack.shrink(1);
            setChanged();
        }
    }

    public float consumeScrapValue(float value) {
        if (scrapValue - value >= 0) {
            scrapValue -= value;
            setChanged();
            return value;
        } else if (scrapValue > 0) {
            float returnValue = scrapValue;
            scrapValue = 0;
            setChanged();
            return returnValue;
        }
        return 0;
    }

    private boolean isScrapItem(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(mio_icif_normal.SCRAP.get())
            || stack.is(mio_icif_normal.SCRAPBOX.get())
            || stack.is(mio_icif_resources.THORIUM_SCRAP.get()));
    }

    public float getScrapValue() { return scrapValue; }
    public IItemHandler getItemHandler() { return itemHandler; }

    @Override
    @org.jetbrains.annotations.Nullable
    public BlockEntity getJadeDisplayTarget() {
        return null;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.large_fabricator_scrap");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("scrapValue", scrapValue);
        tag.put("Items", itemHandler.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("scrapValue")) {
            scrapValue = tag.getFloat("scrapValue");
        }
        if (tag.contains("Items")) {
            CompoundTag itemsTag = tag.getCompound("Items");
            itemHandler.deserializeNBT(registries, itemsTag);
        }
    }
}