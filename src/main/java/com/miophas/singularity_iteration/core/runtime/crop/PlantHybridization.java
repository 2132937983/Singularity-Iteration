// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.crop;

import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;

/** IC2 crossing and single-neighbor spreading; never loads adjacent chunks. */
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
            if (target.getPlanterWorld().hasChunkAt(pos)
                    && target.getPlanterWorld().getBlockEntity(pos) instanceof IPlanter crop) result.add(crop);
        }
        return result;
    }
    public static int participationBase(int growth, int resistance) {
        return 4 + (growth >= 16 ? 1 : 0) + (growth >= 30 ? 1 : 0) + (resistance >= 28 ? 27 - resistance : 0);
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
        return Math.clamp(sum / parents + target.getPlanterWorld().random.nextInt(1 + 2 * parents) - parents, 0, 31);
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
