// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.crop;

import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.Direction;

/** Bounded cross-crop hybridization using only the public planter and plant contracts. */
public class PlantHybridization {
    public static boolean tryHybridize(IPlanter planter) {
        if (planter == null || planter.getPlanterWorld() == null) return false;
        return planter.getPlanterWorld().random.nextInt(4) == 0 && forceHybridize(planter);
    }

    public static boolean forceHybridize(IPlanter planter) {
        if (planter == null || planter.getPlanterWorld() == null || planter.getPlant() != null
                || !planter.isHybridBase()) return false;
        List<IPlanter> neighbors = new ArrayList<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            var neighborPos = planter.getPlanterPos().relative(direction);
            // Hybridization must be a local operation.  Do not force-load a
            // neighboring chunk just because a crop stick is at its edge.
            if (!planter.getPlanterWorld().hasChunkAt(neighborPos)) continue;
            var blockEntity = planter.getPlanterWorld().getBlockEntity(neighborPos);
            if (blockEntity instanceof IPlanter neighbor && neighbor.getPlant() != null
                    && neighbor.getPlant().canHybridize(neighbor)) {
                neighbors.add(neighbor);
            }
        }
        if (neighbors.size() < 2) return false;

        IPlanter first = neighbors.get(0);
        IPlanter second = neighbors.get(1);
        PlantType child = chooseChild(planter, first.getPlant(), second.getPlant());
        if (child == null) return false;
        planter.setPlant(child);
        planter.setGrowthStage(1);
        planter.setGrowthSpeed(inherit(planter, first.getGrowthSpeed(), second.getGrowthSpeed()));
        planter.setYield(inherit(planter, first.getYield(), second.getYield()));
        planter.setResilience(inherit(planter, first.getResilience(), second.getResilience()));
        planter.setScanLevel(0);
        planter.setProgress(0);
        planter.setHybridBase(false);
        planter.updateState();
        return true;
    }

    private static int inherit(IPlanter target, int left, int right) {
        int mutation = target.getPlanterWorld().random.nextInt(3) - 1;
        return Math.clamp((left + right + 1) / 2 + mutation, 0, 31);
    }

    private static PlantType chooseChild(IPlanter planter, PlantType first, PlantType second) {
        if (first.equals(second)) return first;
        List<PlantType> compatible = PlantRegistry.instance.getAllPlants().stream()
            .filter(candidate -> sharesTrait(candidate, first) && sharesTrait(candidate, second))
            .toList();
        if (compatible.isEmpty()) return planter.getPlanterWorld().random.nextBoolean() ? first : second;
        return compatible.get(planter.getPlanterWorld().random.nextInt(compatible.size()));
    }

    private static boolean sharesTrait(PlantType left, PlantType right) {
        List<String> traits = Arrays.asList(left.getTraits());
        return Arrays.stream(right.getTraits()).anyMatch(traits::contains);
    }
}
