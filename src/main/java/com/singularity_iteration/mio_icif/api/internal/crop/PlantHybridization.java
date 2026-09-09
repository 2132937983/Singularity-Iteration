package com.singularity_iteration.mio_icif.api.internal.crop;

import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * 植物杂交管理器（内部实现）
 * 处理植物之间的杂交逻辑
 * 基于IC2的加权随机杂交算法，相似属性和特性的作物更容易杂交出相似结果
 *
 * <p><strong>这是内部实现类，不应被 Addon 直接访问。</strong>
 * 请通过 ICropAPI 提供的公共接口进行植物杂交操作。
 */
public class PlantHybridization {

    private static final Random RANDOM = new Random();

    /**
     * 尝试进行杂交
     * 基于IC2的attemptCrossing逻辑
     * @param planter 目标种植架（必须为空且有高级作物架）
     * @return 是否成功杂交
     */
    public static boolean tryHybridize(IPlanter planter) {
        if (RANDOM.nextInt(3) != 0) return false;
        return tryHybridizeInternal(planter, false);
    }

    /**
     * 强制进行杂交（用于创造模式测试）
     * @param planter 目标种植架
     * @return 是否成功杂交
     */
    public static boolean forceHybridize(IPlanter planter) {
        return tryHybridizeInternal(planter, true);
    }

    /**
     * 内部杂交逻辑
     * @param planter 目标种植架
     * @param force 是否强制（跳过随机检查）
     * @return 是否成功杂交
     */
    private static boolean tryHybridizeInternal(IPlanter planter, boolean force) {
        Level level = planter.getPlanterWorld();
        BlockPos pos = planter.getPlanterPos();

        List<IPlanter> participants = new ArrayList<>();

        askCropJoinCross(level, planter, pos.north(), participants, force);
        askCropJoinCross(level, planter, pos.south(), participants, force);
        askCropJoinCross(level, planter, pos.east(), participants, force);
        askCropJoinCross(level, planter, pos.west(), participants, force);

        if (participants.size() < 2) return false;

        return performHybridization(planter, participants);
    }

    /**
     * 询问相邻作物是否参与杂交
     * 基于IC2的askCropJoinCross逻辑
     * canGrow检查的是目标位置（target），不是邻居自己
     * 生长值和抗性值越高的作物越容易参与杂交
     */
    private static void askCropJoinCross(Level level, IPlanter target, BlockPos neighborPos, List<IPlanter> participants, boolean force) {
        if (!(level.getBlockEntity(neighborPos) instanceof IPlanter neighbor)) return;

        PlantType neighborPlant = neighbor.getPlant();
        if (neighborPlant == null) return;
        if (!neighborPlant.canGrow(target) || !neighborPlant.canHybridize(neighbor)) return;

        // 强制模式：跳过随机检查，直接添加
        if (force) {
            participants.add(neighbor);
            return;
        }

        int base = 4;
        if (neighbor.getGrowthSpeed() >= 16) base++;
        if (neighbor.getGrowthSpeed() >= 30) base++;
        // 修复：高抗性作物应更容易杂交，使用 resilience - 27 而非 27 - resilience
        if (neighbor.getResilience() >= 28) base += neighbor.getResilience() - 27;

        if (base >= RANDOM.nextInt(16)) {
            participants.add(neighbor);
        }
    }

    /**
     * 执行杂交
     * 基于IC2的加权随机选择算法，属性继承取所有参与者的平均值
     */
    private static boolean performHybridization(IPlanter target, List<IPlanter> participants) {
        PlantType result = selectHybridResult(target, participants);
        if (result == null) return false;

        int count = participants.size();

        int growthSpeed = 0;
        int yield = 0;
        int resilience = 0;

        for (IPlanter p : participants) {
            growthSpeed += p.getGrowthSpeed();
            yield += p.getYield();
            resilience += p.getResilience();
        }

        growthSpeed /= count;
        yield /= count;
        resilience /= count;

        growthSpeed += RANDOM.nextInt(1 + 2 * count) - count;
        yield += RANDOM.nextInt(1 + 2 * count) - count;
        resilience += RANDOM.nextInt(1 + 2 * count) - count;

        growthSpeed = clampStat(growthSpeed);
        yield = clampStat(yield);
        resilience = clampStat(resilience);

        target.setPlant(result);
        target.setGrowthStage(1);
        target.setGrowthSpeed(growthSpeed);
        target.setYield(yield);
        target.setResilience(resilience);
        target.setHybridBase(true);
        target.updateState();

        return true;
    }

    /**
     * 基于IC2的加权随机选择算法选择杂交结果
     * 为每个注册的作物计算与相邻作物的相似度权重，
     * 然后通过累积分布进行二分查找随机选择
     */
    private static PlantType selectHybridResult(IPlanter target, List<IPlanter> participants) {
        Collection<PlantType> allPlants = PlantRegistry.instance.getAllPlants();
        if (allPlants.isEmpty()) return null;

        PlantType[] crops = allPlants.toArray(new PlantType[0]);
        int[] ratios = new int[crops.length];
        int total = 0;

        for (int i = 0; i < ratios.length; i++) {
            PlantType candidate = crops[i];
            if (candidate.canGrow(target)) {
                for (IPlanter participant : participants) {
                    PlantType parentPlant = participant.getPlant();
                    if (parentPlant != null) {
                        total += calculateRatioFor(candidate, parentPlant);
                    }
                }
            }
            ratios[i] = total;
        }

        if (total <= 0) return null;

        int search = RANDOM.nextInt(total);

        int min = 0;
        int max = ratios.length - 1;

        while (min < max) {
            int cur = (min + max) / 2;
            int value = ratios[cur];
            if (search < value) {
                max = cur;
            } else {
                min = cur + 1;
            }
        }

        return crops[min];
    }

    /**
     * 计算杂交比率权重
     * 基于IC2的calculateRatioFor逻辑
     * 属性越相似、特性越匹配、等级越接近，权重越高
     * 完全相同的作物权重为500（极高）
     */
    private static int calculateRatioFor(PlantType candidate, PlantType parent) {
        if (candidate.equals(parent)) return 500;

        int value = 0;

        PlantStats candidateStats = candidate.getStats();
        PlantStats parentStats = parent.getStats();

        for (int i = 0; i < 5; i++) {
            value += statSimilarity(candidateStats.stat(i), parentStats.stat(i));
        }

        for (String candidateTrait : candidate.getTraits()) {
            for (String parentTrait : parent.getTraits()) {
                if (candidateTrait.equalsIgnoreCase(parentTrait)) {
                    value += 5;
                }
            }
        }

        int diff = candidateStats.getLevel() - parentStats.getLevel();
        if (diff > 1) {
            value -= 2 * diff;
        }
        if (diff < -3) {
            value -= -diff;
        }

        // 特殊杂交加成：甘蔗(reed)容易杂交出粘性甘蔗(stickreed)
        if (candidate.getTypeId().equals("stickreed") && parent.getTypeId().equals("reed")) {
            value += 30; // 大幅增加粘性甘蔗的杂交权重
        }

        return Math.max(value, 0);
    }

    /**
     * 计算单个属性的相似度贡献
     * 差值为0得2分，差值越大扣越多
     */
    private static int statSimilarity(int a, int b) {
        int delta = Math.abs(a - b);
        return -delta + 2;
    }

    private static int clampStat(int stat) {
        return Math.max(0, Math.min(31, stat));
    }
}