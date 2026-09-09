package com.singularity_iteration.mio_icif.api.crop;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 植物类型基类
 * 定义一种可种植作物的基本属性和行为
 */
public abstract class PlantType {

    /**
     * 获取植物类型ID
     */
    public abstract String getTypeId();

    /**
     * 获取植物所属模组ID
     */
    public abstract String getModId();

    /**
     * 获取本地化名称的键
     */
    public String getTranslationKey() {
        return getModId() + ".plant." + getTypeId();
    }

    /**
     * 获取发现者名称（无参版本，用于GUI显示）
     */
    public String getFoundBy() {
        return "unknown";
    }

    /**
     * 获取发现者名称（带玩家名称版本，用于作物分析仪右键）
     * @param playerName 当前玩家的名称
     */
    public String getFoundBy(String playerName) {
        return playerName != null ? playerName : getFoundBy();
    }

    /**
     * 获取植物特性标签
     */
    public abstract String[] getTraits();

    /**
     * 获取植物属性（等级等）
     */
    public abstract PlantStats getStats();

    /**
     * 获取最大成长阶段
     */
    public abstract int getMaxGrowthStage();

    /**
     * 获取可收获阶段（达到此阶段可收获）
     */
    public int getHarvestStage() {
        return getMaxGrowthStage();
    }

    /**
     * 获取最佳收获阶段
     */
    public int getOptimalHarvestStage() {
        return getHarvestStage();
    }

    /**
     * 收获后的大小
     */
    public int getStageAfterHarvest() {
        return 1;
    }

    /**
     * 获取生长所需时间（生长点数）
     * 原版IC2: 每个阶段需要的生长点数
     */
    public int getGrowthTime(IPlanter planter) {
        // 基础值：与calcGrowthRate * CROP_TICK_RATE匹配
        // calcGrowthRate返回2-7，CROP_TICK_RATE=512，所以每次增长1024-3584
        // 基础时间设为 200 * 等级 * 100，这样需要约 5-20 个周期才能升级
        int baseTime = getStats().getLevel() * 200 * 100;

        // 环境越好，所需时间越短（但最少为 10000）
        int have = weightInfluences(planter, planter.getHumidity(), planter.getSoilNutrients(), planter.getAirQuality());
        int need = (getStats().getLevel() - 1) * 4 + planter.getGrowthSpeed() + planter.getYield() + planter.getResilience();

        if (have >= need) {
            // 环境好，减少所需时间
            baseTime = baseTime * 100 / (100 + have - need);
        } else {
            // 环境差，增加所需时间
            baseTime = baseTime * (100 + (need - have) * 2) / 100;
        }

        return Math.max(10000, baseTime);
    }

    /**
     * 是否可以继续生长
     */
    public boolean canGrow(IPlanter planter) {
        return planter.getGrowthStage() < getMaxGrowthStage();
    }

    /**
     * 是否可以参与杂交
     * 基于IC2的canCross逻辑：需要达到一定成长阶段
     */
    public boolean canHybridize(IPlanter planter) {
        return planter.getGrowthStage() + 2 > getMaxGrowthStage();
    }

    /**
     * 是否是杂草
     * 基于IC2的isWeed逻辑
     */
    public boolean isWeed(IPlanter planter) {
        return false;
    }

    /**
     * 每tick执行的逻辑
     * 基于IC2的CropCard.tick()
     * 子类可覆写以实现特殊行为（如毒藤伤害实体等）
     */
    public void tick(IPlanter planter) {
    }

    /**
     * 是否可以被收获
     * 原版IC2: canBeHarvested
     */
    public boolean isHarvestable(IPlanter planter) {
        return planter.getGrowthStage() >= getHarvestStage();
    }

    /**
     * 是否可以被收割（用于收割机）
     * 原版IC2: canBeHarvested - 达到最佳收割阶段即可收割
     */
    public boolean canBeHarvested(IPlanter planter) {
        return planter.getGrowthStage() >= getOptimalHarvestStage(planter);
    }

    /**
     * 获取最佳收割阶段
     * 原版IC2: getOptimalHarvestSize
     */
    public int getOptimalHarvestStage(IPlanter planter) {
        return getOptimalHarvestStage();
    }

    /**
     * 获取收获物（包含产物和种子）
     * 用于手动收割
     */
    public abstract ItemStack[] getHarvest(IPlanter planter);

    /**
     * 获取产物（单个）- 原版IC2: getGain
     * 默认实现：从getHarvest中过滤掉种子
     */
    public ItemStack getGain(IPlanter planter) {
        ItemStack[] harvest = getHarvest(planter);
        // 返回第一个非种子物品，如果没有则返回第一个物品
        for (ItemStack stack : harvest) {
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 获取产物数组 - 原版IC2: getGains
     * 用于收割机，只返回产物，不返回种子
     * 默认实现：从getHarvest中过滤掉种子
     */
    public ItemStack[] getGains(IPlanter planter) {
        ItemStack[] harvest = getHarvest(planter);
        if (harvest == null || harvest.length == 0) {
            return new ItemStack[]{getGain(planter)}; // 如果没有收获数组，返回单个产物
        }

        List<ItemStack> gains = new ArrayList<>();

        // 获取种子物品用于过滤
        ItemStack seed = getSeedItem(planter);

        for (ItemStack stack : harvest) {
            if (!stack.isEmpty() && !ItemStack.isSameItem(stack, seed)) {
                gains.add(stack);
            }
        }

        // 如果没有过滤出产物，返回所有非空物品（兼容旧实现）
        if (gains.isEmpty()) {
            for (ItemStack stack : harvest) {
                if (!stack.isEmpty()) {
                    gains.add(stack);
                }
            }
        }

        // 如果还是没有产物，返回单个产物作为后备
        if (gains.isEmpty()) {
            gains.add(getGain(planter));
        }

        return gains.toArray(new ItemStack[0]);
    }

    /**
     * 获取种子物品
     * 默认实现：返回空，子类可以覆盖
     */
    public ItemStack getSeedItem(IPlanter planter) {
        return ItemStack.EMPTY;
    }

    /**
     * 获取产物掉落概率
     * 原版IC2: dropGainChance
     */
    public double dropGainChance() {
        return Math.pow(0.95D, getStats().getLevel());
    }

    /**
     * 计算实际产物数量
     * 原版IC2: 使用高斯分布计算
     */
    public int calculateDropCount(IPlanter planter) {
        double chance = dropGainChance();
        chance *= Math.pow(1.03D, planter.getYield());
        int count = (int) Math.max(0L, Math.round(planter.getPlanterWorld().getRandom().nextGaussian() * chance * 0.6827D + chance));
        return Math.max(1, count); // 至少返回1个产物
    }

    /**
     * 获取特殊产物（用于作物分析仪功能）
     * 如粘性树脂、毒马铃薯等第二阶段产物
     * @param planter 种植架
     * @return 特殊产物，如果没有则返回空物品栈
     */
    public ItemStack getSpecialDrop(IPlanter planter) {
        // 默认实现：没有特殊产物
        return ItemStack.EMPTY;
    }

    /**
     * 获取根系深度（影响下方方块检测范围）
     */
    public int getRootDepth(IPlanter planter) {
        return 5;
    }

    /**
     * 获取纹理路径（用于每个成长阶段）
     */
    public abstract String getTexture(int stage);

    /**
     * 右键点击时的处理
     */
    public boolean onInteract(IPlanter planter, net.minecraft.world.entity.player.Player player) {
        if (!isHarvestable(planter)) {
            return false;
        }
        return planter.doManualHarvest();
    }

    /**
     * 种子掉落概率
     * 基于IC2的dropSeedChance机制
     * @param planter 种植架
     * @return 基础掉落概率 (0.0 - 1.0)
     */
    public float dropSeedChance(IPlanter planter) {
        if (planter.getGrowthStage() == 1) return 0.0F;
        float base = 0.5F;
        if (planter.getGrowthStage() == 2) base /= 2.0F;
        for (int i = 0; i < getStats().getLevel(); i++) {
            base *= 0.8F;
        }
        return base;
    }

    /**
     * 环境权重影响
     * 基于IC2的weightInfluences机制
     * 返回值越高，作物越适应环境
     * @param planter 种植架
     * @param humidity 湿度
     * @param soilNutrients 土壤养分
     * @param airQuality 空气质量
     * @return 环境适应权重
     */
    public int weightInfluences(IPlanter planter, int humidity, int soilNutrients, int airQuality) {
        return humidity + soilNutrients + airQuality;
    }

    /**
     * 获取额外信息（显示在分析器中）
     */
    public List<String> getExtraInfo() {
        return List.of();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PlantType)) return false;
        PlantType other = (PlantType) obj;
        return getTypeId().equals(other.getTypeId()) && getModId().equals(other.getModId());
    }

    @Override
    public int hashCode() {
        return getTypeId().hashCode() * 31 + getModId().hashCode();
    }
}
