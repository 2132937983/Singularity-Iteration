package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractGeneratorBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("null")
public class mio_icif_unlimit_generator extends AbstractGeneratorBlockEntity {

    public static final long CAPACITY = 100000L;
    public static final long ENERGY_GENERATION_RATE = 8192L;
    public static final long MAX_EXTRACT = 8192L;
    public static final CableTier OUTPUT_TIER = tierForRate(ENERGY_GENERATION_RATE);

    private static CableTier tierForRate(long ratePerTick) {
        for (CableTier tier : CableTier.allTiers()) {
            if (tier.getPowerRating() >= ratePerTick) {
                return tier;
            }
        }
        return CableTier.MAX;
    }

    public mio_icif_unlimit_generator(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.UNLIMIT_GENERATOR_ENTITY_TYPE.get(),
            SlotLayout.builder().extra(1).build(), ENERGY_GENERATION_RATE, CAPACITY, 0, MAX_EXTRACT, OUTPUT_TIER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_unlimit_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        long room = blockEntity.apiGetMaxEnergy() - blockEntity.apiGetStoredEnergy();
        long toGen = Math.min(blockEntity.energyGenerationRate, room);
        if (toGen > 0) {
            blockEntity.apiGenerateEnergy(toGen, false);
        }

        blockEntity.chargeItems();

        blockEntity.distributeEnergy();

        blockEntity.updateActiveState(level, pos, true);
        blockEntity.setChanged();
    }

    @Override
    protected void consumeFuel() {
        burnTime = Integer.MAX_VALUE;
    }

    @Override
    public void generateEnergy() {
        long room = apiGetMaxEnergy() - apiGetStoredEnergy();
        long toGen = Math.min(energyGenerationRate, room);
        if (toGen > 0) {
            apiGenerateEnergy(toGen, false);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void chargeItems() {
        if (itemHandler.getSlots() <= CHARGE_SLOT) {
            return;
        }

        ItemStack chargeStack = itemHandler.getStackInSlot(CHARGE_SLOT);
        if (chargeStack.isEmpty()) {
            return;
        }

        if (chargeStack.getItem() instanceof com.miophas.singularity_iteration.common.item.normal.mio_icif_bat battery) { // NOPMD
            long currentEnergy = battery.getEnergy(chargeStack);
            long batteryMaxEnergy = battery.getMaxEnergy();
            long batteryChargeRate = battery.getChargeRate();

            if (currentEnergy >= batteryMaxEnergy) {
                return;
            }

            long energyToCharge = Math.min(batteryChargeRate, batteryMaxEnergy - currentEnergy);

            if (energyToCharge > 0) {
                battery.addEnergy(chargeStack, energyToCharge);
            }
        }
    }

    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        return 0;
    }

    @Override
    public boolean isBurning() {
        return true;
    }
}