package com.singularity_iteration.mio_icif.api.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * 种植架方块实体接口
 * 定义了种植架需要实现的所有方法 */
public interface IPlanter {

    // ==================== 植物类型 ====================
    PlantType getPlant();
    void setPlant(PlantType plantType);

    // ==================== 成长阶段 ====================
    int getGrowthStage();
    void setGrowthStage(int stage);

    // ==================== 三属性(0-31) ====================
    int getGrowthSpeed();
    void setGrowthSpeed(int speed);

    int getYield();
    void setYield(int yield);

    int getResilience();
    void setResilience(int resilience);

    // ==================== 存储资源 ====================
    int getNutrients();
    void setNutrients(int nutrients);

    int getWater();
    void setWater(int water);

    int getWeedControl();
    void setWeedControl(int weedControl);

    // ==================== 成长 ====================
    int getProgress();
    void setProgress(int progress);

    // ==================== 扫描等级 ====================
    int getScanLevel();
    void setScanLevel(int level);

    // ==================== 杂交基地标记 ====================
    boolean isHybridBase();
    void setHybridBase(boolean hybridBase);

    // ==================== 自定义数据 ====================
    CompoundTag getCustomData();

    // ==================== 环境检测 ====================
    int getHumidity();
    int getSoilNutrients();
    int getAirQuality();

    Level getPlanterWorld();
    BlockPos getPlanterPos();

    int getLightLevel();

    // ==================== 操作 ====================
    boolean pick();
    boolean doManualHarvest();
    boolean doHarvestWithoutSeeds();
    List<ItemStack> doHarvest();
    void reset();
    void updateState();

    boolean isBlockBelow(Block block);
    boolean isBlockBelow(String oredictName);

    // ==================== 种子生成 ====================
    ItemStack makeSeeds(PlantType plantType, int stage, int growthSpeed, int yield, int resilience);
}

