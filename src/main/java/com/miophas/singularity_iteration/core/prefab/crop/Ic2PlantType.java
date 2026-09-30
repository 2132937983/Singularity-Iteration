// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.prefab.crop;

import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** IC2 Experimental defaults. tick is a special-behavior hook, never the growth engine. */
public abstract class Ic2PlantType extends PlantType {
    @Override public boolean usesIc2CropCycle() { return true; }
    @Override public void tick(IPlanter planter) {}
    @Override public int getGrowthTime(IPlanter planter) { return getStats().getLevel() * 200; }
    @Override public boolean canGrow(IPlanter planter) { return planter.getGrowthStage() < getMaxGrowthStage(); }
    @Override public boolean canHybridize(IPlanter planter) { return planter.getGrowthStage() >= 3; }
    @Override public boolean isWeed(IPlanter planter) {
        return planter.getGrowthStage() >= 2 && (getTypeId().equals("weed") || planter.getGrowthSpeed() >= 24);
    }
    @Override public boolean canBeHarvested(IPlanter planter) { return planter.getGrowthStage() == getMaxGrowthStage(); }
    @Override public boolean isHarvestable(IPlanter planter) { return canBeHarvested(planter); }
    @Override public int getOptimalHarvestStage() { return getMaxGrowthStage(); }
    @Override public int weightInfluences(IPlanter planter, int humidity, int nutrients, int air) { return humidity + nutrients + air; }
    @Override public double dropGainChance() { return Math.pow(0.95, getStats().getLevel()); }
    @Override public int calculateDropCount(IPlanter planter) {
        double chance = dropGainChance() * Math.pow(1.03, planter.getYield());
        return Math.max(0, (int) Math.round(planter.getPlanterWorld().random.nextGaussian() * chance * 0.6827 + chance));
    }
    @Override public float dropSeedChance(IPlanter planter) {
        if (planter.getGrowthStage() == 1) return 0;
        return (float) ((planter.getGrowthStage() == 2 ? 0.25 : 0.5) * Math.pow(0.8, getStats().getLevel()));
    }
    @Override public ItemStack getSeedItem(IPlanter planter) {
        return planter.makeSeeds(this, 1, planter.getGrowthSpeed(), planter.getYield(), planter.getResilience());
    }
    @Override public boolean onInteract(IPlanter planter, Player player) { return planter.doManualHarvest(); }
}
