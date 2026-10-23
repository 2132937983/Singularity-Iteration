package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@SuppressWarnings("null")
public class PlantBaseSapling extends com.miophas.singularity_iteration.core.prefab.crop.Ic2PlantType {

    protected final String cropName;
    protected final String[] traits;
    protected final ItemStack cropDrop;
    protected final ItemStack saplingDrop;
    protected final ItemStack logDrop; // 主产物：原木（IC2 CropBaseSapling 的 cropDrop）
    protected final boolean isOak;

    public PlantBaseSapling(String cropName, String[] traits, ItemStack cropDrop, ItemStack saplingDrop, boolean isOak) {
        this(cropName, traits, cropDrop, saplingDrop, null, isOak);
    }

    public PlantBaseSapling(String cropName, String[] traits, ItemStack cropDrop, ItemStack saplingDrop, ItemStack logDrop, boolean isOak) {
        this.cropName = cropName;
        this.traits = traits;
        this.cropDrop = cropDrop;
        this.saplingDrop = saplingDrop;
        this.logDrop = logDrop;
        this.isOak = isOak;
    }

    @Override
    public String getTypeId() {
        return cropName;
    }

    @Override
    public String getModId() {
        return Singularity_Iteration.MOD_ID;
    }

    @Override
    public String getFoundBy() {
        return "Speiger";
    }

    @Override
    public String[] getTraits() {
        return traits;
    }

    /** EN: Shared, immutable stats (the crossing loop reads them often). ZH: 共享的不可变数值（杂交循环频繁读取）。JA: 共有の不変値（交配ループが頻繁に読む）。 */
    private static final PlantStats STATS = new PlantStats(3, 1, 0, 4, 4, 0);

    @Override
    public PlantStats getStats() {
        return STATS;
    }

    @Override
    public int getMaxGrowthStage() {
        return 5;
    }

    @Override
    public int getHarvestStage() {
        return 5;
    }

    @Override
    public int getOptimalHarvestStage() {
        return 5;
    }

    @Override
    public int getStageAfterHarvest() {
        return 4;
    }

    /**
     * IC2 {@code CropBaseSapling.getGains}: one log, a sapling 25% of the time, and for oak an apple 25% of
     * the time. 0.1.10.15: the log was registered but never dropped (the leaves came instead), so trees on
     * crop sticks gave no wood. A tree registered without a log (the five-argument constructor) keeps its drop.
     */
    @Override
    public ItemStack[] getHarvest(IPlanter planter) {
        java.util.List<ItemStack> drops = new java.util.ArrayList<>();
        drops.add((logDrop != null && !logDrop.isEmpty() ? logDrop : cropDrop).copy());
        if (planter.getPlanterWorld().random.nextInt(100) >= 75) {
            drops.add(saplingDrop.copy());
        }
        if (isOak && planter.getPlanterWorld().random.nextInt(100) >= 75) {
            drops.add(new ItemStack(Items.APPLE, 1));
        }
        return drops.toArray(new ItemStack[0]);
    }

    @Override
    public int getGrowthTime(IPlanter planter) {
        if (planter.getGrowthStage() >= 4) return 150;
        return 600;
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/" + cropName + "_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        return planter.getGrowthStage() < getMaxGrowthStage() && planter.getLightLevel() >= 9;
    }
}