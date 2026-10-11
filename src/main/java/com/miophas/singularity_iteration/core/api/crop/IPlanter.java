package com.miophas.singularity_iteration.core.api.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Block entity interface for a crop planter.
 * It defines all the methods that a planter must implement. */
public interface IPlanter {

    // ==================== Plant type ====================
    PlantType getPlant();
    void setPlant(PlantType plantType);

    // ==================== Growth stage ====================
    int getGrowthStage();
    void setGrowthStage(int stage);

    // ==================== Three stats (range 0-31) ====================
    int getGrowthSpeed();
    void setGrowthSpeed(int speed);

    int getYield();
    void setYield(int yield);

    int getResilience();
    void setResilience(int resilience);

    // ==================== Stored resources ====================
    int getNutrients();
    void setNutrients(int nutrients);

    int getWater();
    void setWater(int water);

    int getWeedControl();
    void setWeedControl(int weedControl);

    // ==================== Growth ====================
    int getProgress();
    void setProgress(int progress);

    // ==================== Scan level ====================
    int getScanLevel();
    void setScanLevel(int level);

    // ==================== Hybrid base flag ====================
    boolean isHybridBase();
    void setHybridBase(boolean hybridBase);

    // ==================== Custom data ====================
    CompoundTag getCustomData();

    // ==================== Environment checks ====================
    int getHumidity();
    int getSoilNutrients();
    int getAirQuality();

    Level getPlanterWorld();
    BlockPos getPlanterPos();

    int getLightLevel();

    // ==================== Operations ====================
    boolean pick();
    boolean doManualHarvest();
    boolean doHarvestWithoutSeeds();
    List<ItemStack> doHarvest();
    void reset();
    void updateState();

    boolean isBlockBelow(Block block);
    boolean isBlockBelow(String oredictName);

    // ==================== Seed creation ====================
    /** Creates seed bags with the planter scan level. The second argument is the stack count. */
    ItemStack makeSeeds(PlantType plantType, int count, int growthSpeed, int yield, int resilience);
}

