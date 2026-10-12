// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.crop;

import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;

/**
 * <p><b>EN:</b> IC2 crossing and single-neighbour spreading on crossing sticks. The code reads only
 * loaded neighbours, so it never loads a chunk.
 * <ul>
 *   <li>Crossing: one try in three cycles. Each horizontal neighbour joins when it can cross
 *       and passes the participation roll ({@link CropGrowthMath#participationBase}). Two or
 *       more parents are necessary. The child species is a weighted random choice
 *       ({@link #calculateRatio}). Each trait is the parent average with a mutation of
 *       ±(number of parents) ({@link CropGrowthMath#inherit}).</li>
 *   <li>Spreading: when exactly one neighbour is a planter and it passes the participation
 *       roll, the stick gets a copy of that plant with the same traits.</li>
 * </ul>
 * <p><b>ZH:</b> 杂交作物架上的 IC2 杂交与单邻居扩散。代码只读取已加载的邻居，因此不会加载区块。
 * <ul>
 *   <li>杂交：每 3 个周期尝试 1 次。每个水平邻居在可杂交且通过参与检定（{@link CropGrowthMath#participationBase}）时加入。
 *       至少需要 2 个亲本。后代物种按权重随机选择（{@link #calculateRatio}）。每项属性为亲本平均值加 ±（亲本数）的变异
 *       （{@link CropGrowthMath#inherit}）。</li>
 *   <li>扩散：恰好只有 1 个邻居是作物架且其通过参与检定时，作物架获得该植物的副本，属性相同。</li>
 * </ul>
 * <p><b>JA:</b> 交配用作物棒での IC2 交配と単一隣接拡散。読み込み済みの隣接ブロックだけを読むので、チャンクを読み込まない。
 * <ul>
 *   <li>交配：3 サイクルに 1 回試行する。水平方向の各隣接作物は、交配可能で参加判定（{@link CropGrowthMath#participationBase}）に
 *       通ると参加する。親は 2 本以上必要。子の種は重み付きランダム（{@link #calculateRatio}）。各特性値は親の平均に
 *       ±（親の数）の変異を加えた値（{@link CropGrowthMath#inherit}）。</li>
 *   <li>拡散：隣接する作物棒がちょうど 1 本で、それが参加判定に通ると、同じ特性値のままその植物が複製される。</li>
 * </ul>
 */
public class PlantHybridization {
    private static boolean available(IPlanter target) {
        return target != null && target.getPlanterWorld() != null && target.getPlant() == null && target.isHybridBase();
    }
    public static boolean tryHybridize(IPlanter target) {
        return available(target) && target.getPlanterWorld().random.nextInt(3) == 0 && forceHybridize(target);
    }
    private static List<IPlanter> neighbors(IPlanter target) {
        List<IPlanter> result = new ArrayList<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            var pos = target.getPlanterPos().relative(direction);
            if (target.getPlanterWorld().isLoaded(pos)
                    && target.getPlanterWorld().getBlockEntity(pos) instanceof IPlanter crop) result.add(crop);
        }
        return result;
    }
    /** EN: Kept for callers of 0.1.8.0; see {@link CropGrowthMath#participationBase}. ZH: 为兼容 0.1.8.0 保留。JA: 0.1.8.0 との互換のため残す。 */
    public static int participationBase(int growth, int resistance) {
        return CropGrowthMath.participationBase(growth, resistance);
    }
    private static boolean participates(IPlanter parent, IPlanter target) {
        PlantType crop = parent.getPlant();
        return crop != null && crop.canGrow(target) && crop.canHybridize(parent)
            && participationBase(parent.getGrowthSpeed(), parent.getResilience()) >= target.getPlanterWorld().random.nextInt(16);
    }
    public static boolean forceHybridize(IPlanter target) {
        if (!available(target)) return false;
        List<IPlanter> parents = neighbors(target).stream().filter(parent -> participates(parent, target)).toList();
        if (parents.size() < 2) return false;
        List<PlantType> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int total = 0;
        for (PlantType candidate : PlantRegistry.instance.getAllPlants()) {
            if (!candidate.canGrow(target)) continue;
            int weight = parents.stream().mapToInt(parent -> calculateRatio(candidate, parent.getPlant())).sum();
            if (weight == 0) continue;
            candidates.add(candidate);
            weights.add(weight);
            total += weight;
        }
        if (total == 0) return false;
        int roll = target.getPlanterWorld().random.nextInt(total);
        int index = 0;
        while (roll >= weights.get(index)) roll -= weights.get(index++);
        int n = parents.size();
        int growth = inherit(target, parents.stream().mapToInt(IPlanter::getGrowthSpeed).sum(), n);
        int gain = inherit(target, parents.stream().mapToInt(IPlanter::getYield).sum(), n);
        int resistance = inherit(target, parents.stream().mapToInt(IPlanter::getResilience).sum(), n);
        plant(target, candidates.get(index), growth, gain, resistance);
        return true;
    }
    public static boolean trySpread(IPlanter target) {
        if (!available(target)) return false;
        List<IPlanter> neighbors = neighbors(target);
        if (neighbors.size() != 1 || !participates(neighbors.getFirst(), target)) return false;
        IPlanter parent = neighbors.getFirst();
        plant(target, parent.getPlant(), parent.getGrowthSpeed(), parent.getYield(), parent.getResilience());
        return true;
    }
    private static void plant(IPlanter target, PlantType plant, int growth, int gain, int resistance) {
        target.setPlant(plant);
        target.setGrowthStage(1);
        target.setGrowthSpeed(growth);
        target.setYield(gain);
        target.setResilience(resistance);
        target.setScanLevel(0);
        target.setProgress(0);
        target.setHybridBase(false);
        target.updateState();
    }
    private static int inherit(IPlanter target, int sum, int parents) {
        return CropGrowthMath.inherit(sum, parents, target.getPlanterWorld().random.nextInt(1 + 2 * parents));
    }
    public static int calculateRatio(PlantType candidate, PlantType parent) {
        if (candidate.equals(parent)) return 500;
        int value = 0;
        for (int i = 0; i < 5; i++) value += 2 - Math.abs(candidate.getStats().stat(i) - parent.getStats().stat(i));
        for (String a : candidate.getTraits()) for (String b : parent.getTraits()) if (a.equalsIgnoreCase(b)) value += 5;
        int difference = candidate.getStats().getLevel() - parent.getStats().getLevel();
        if (difference > 1) value -= 2 * difference;
        else if (difference < -3) value += difference;
        return Math.max(0, value);
    }
}
