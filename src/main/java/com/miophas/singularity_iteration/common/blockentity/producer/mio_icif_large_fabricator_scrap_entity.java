package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.MultiblockEnergyPart;

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

    /**
     * 0.1.10.15: an item adds its matter-amplifier value times this as EU of scrap value (METS
     * {@code TileEntityFabricatorScrap}: the IC2 amplifier output × 10). The amplifier value is the Mass
     * Fabricator's ({@link mio_icif_matter_elc#amplifierCredit(Level, ItemStack)}): an amplifier recipe, else
     * IC2's scrap 5,000 and scrap box 45,000 (and SI's thorium scrap 360,000). Before, three items were
     * hardcoded at 5,000, 45,000 and 1,200,000 and amplifier recipes were ignored.
     */
    public static final int AMPLIFIER_MULTIPLIER = 10;

    /** EU of scrap value one {@code stack} item adds; 0 when it is no amplifier. */
    public static long scrapValueOf(@org.jetbrains.annotations.Nullable Level level, ItemStack stack) {
        return (long) mio_icif_matter_elc.amplifierCredit(level, stack) * AMPLIFIER_MULTIPLIER;
    }

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

        long value = scrapValueOf(level, scrapStack);
        if (value <= 0) return;
        scrapValue += value;
        scrapStack.shrink(1);
        setChanged();
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

    /** Any matter amplifier: the Mass Fabricator's amplifier recipes and built-in values. */
    private boolean isScrapItem(ItemStack stack) {
        return scrapValueOf(level, stack) > 0;
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