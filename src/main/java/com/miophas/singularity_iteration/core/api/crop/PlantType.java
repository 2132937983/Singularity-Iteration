// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.crop;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Independent common crop policy used by SI plant implementations. */
public abstract class PlantType {
    public abstract String getTypeId();
    public abstract String getModId();

    public String getTranslationKey() {
        return getModId() + ".plant." + getTypeId();
    }

    public String getFoundBy() {
        return "Unknown";
    }

    public String getFoundBy(String fallback) {
        String foundBy = getFoundBy();
        return foundBy == null || foundBy.isBlank() || "Unknown".equals(foundBy) ? fallback : foundBy;
    }

    public abstract String[] getTraits();
    public abstract PlantStats getStats();
    public abstract int getMaxGrowthStage();

    public int getHarvestStage() { return getMaxGrowthStage(); }
    public int getOptimalHarvestStage() { return getHarvestStage(); }
    public int getStageAfterHarvest() { return 1; }
    /** Opt in to the IC2 256-tick growth-point lifecycle. Legacy addons retain per-tick callbacks. */
    public boolean usesIc2CropCycle() { return false; }
    public int getStageAfterHarvest(IPlanter planter) { return getStageAfterHarvest(); }
    public boolean onLeftClick(IPlanter planter, Player player) { return planter.pick(); }
    public boolean onCollision(IPlanter planter, net.minecraft.world.entity.Entity entity) {
        return entity instanceof net.minecraft.world.entity.LivingEntity && entity.isSprinting();
    }
    public int getRedstoneSignal(IPlanter planter) { return 0; }
    public int getLightEmission(IPlanter planter) { return 0; }

    public int getGrowthTime(IPlanter planter) {
        int level = Math.max(0, getStats().getLevel());
        // Frozen R153 public observations for the ordinary SI plant catalog.
        // Level-specific classes (generic crops, saplings and weeds) retain
        // their own overrides.
        return switch (level) {
            case 1 -> 17_391;
            case 2 -> 36_036;
            case 3 -> 56_074;
            case 4 -> 77_669;
            case 5 -> 102_000;
            default -> Math.max(200, (level + 1) * 24_000);
        };
    }

    public boolean canGrow(IPlanter planter) {
        return planter != null
            && planter.getGrowthStage() < getMaxGrowthStage()
            && planter.getLightLevel() >= 9
            && planter.getWater() > 0
            && planter.getNutrients() > 0;
    }

    public boolean canHybridize(IPlanter planter) {
        return planter != null && !isWeed(planter) && planter.getGrowthStage() >= getHarvestStage();
    }

    public boolean isWeed(IPlanter planter) { return false; }

    public void tick(IPlanter planter) {
        if (!canGrow(planter)) return;
        int increment = Math.max(1, 1 + planter.getGrowthSpeed());
        int progress = Math.max(0, planter.getProgress()) + increment;
        int required = Math.max(1, getGrowthTime(planter));
        if (progress >= required) {
            planter.setProgress(progress - required);
            planter.setGrowthStage(Math.min(getMaxGrowthStage(), planter.getGrowthStage() + 1));
            planter.updateState();
        } else {
            planter.setProgress(progress);
        }
    }

    public boolean isHarvestable(IPlanter planter) {
        return planter != null && planter.getGrowthStage() >= getHarvestStage() && canBeHarvested(planter);
    }

    public boolean canBeHarvested(IPlanter planter) {
        return getHarvest(planter).length > 0;
    }

    public int getOptimalHarvestStage(IPlanter planter) { return getOptimalHarvestStage(); }
    public abstract ItemStack[] getHarvest(IPlanter planter);

    public ItemStack getGain(IPlanter planter) {
        ItemStack[] gains = getGains(planter);
        return gains.length == 0 ? ItemStack.EMPTY : gains[0].copy();
    }

    public ItemStack[] getGains(IPlanter planter) {
        ItemStack[] harvest = getHarvest(planter);
        ItemStack[] copy = new ItemStack[harvest.length];
        for (int i = 0; i < harvest.length; i++) copy[i] = harvest[i].copy();
        return copy;
    }

    public ItemStack getSeedItem(IPlanter planter) {
        return ItemStack.EMPTY;
    }

    public double dropGainChance() { return 0.95D; }

    public int calculateDropCount(IPlanter planter) {
        if (planter == null) return 1;
        int bound = Math.max(1, 3 + planter.getYield());
        return 1 + planter.getPlanterWorld().random.nextInt(bound) / 3;
    }

    public ItemStack getSpecialDrop(IPlanter planter) { return ItemStack.EMPTY; }
    public int getRootDepth(IPlanter planter) { return 1; }
    public abstract String getTexture(int stage);
    public boolean onInteract(IPlanter planter, Player player) { return false; }

    public float dropSeedChance(IPlanter planter) {
        int resistance = planter == null ? 0 : Math.max(0, planter.getResilience());
        return Mth.clamp(0.2F - resistance * 0.01F, 0.02F, 0.25F);
    }

    public int weightInfluences(IPlanter planter, int humidity, int nutrients, int airQuality) {
        int growth = planter == null ? 0 : planter.getGrowthSpeed();
        return humidity + nutrients + airQuality + growth - getStats().getLevel();
    }

    public List<String> getExtraInfo() { return Collections.emptyList(); }

    @Override
    public boolean equals(Object value) {
        return value instanceof PlantType other
            && Objects.equals(getModId(), other.getModId())
            && Objects.equals(getTypeId(), other.getTypeId());
    }

    @Override
    public int hashCode() { return Objects.hash(getModId(), getTypeId()); }
}
