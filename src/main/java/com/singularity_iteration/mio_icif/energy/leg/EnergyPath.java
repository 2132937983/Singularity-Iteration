package com.singularity_iteration.mio_icif.energy.leg;

import com.singularity_iteration.mio_icif.energy.grid.*;
import net.minecraft.core.BlockPos;

/**
 * A path from a source node to a sink node through zero or more conductor nodes.
 * Precomputed and cached between topology changes.
 */
class EnergyPath {
    final Node source;
    final Node target;
    final java.util.List<Node> conductors;
    final double loss;
    final net.minecraft.core.Direction targetDirection;
    final double minEffectEnergy;
    final double minInsulationEnergyAbsorption;
    final double minInsulationBreakdownEnergy;
    final double minConductorBreakdownEnergy;
    final int minX, minY, minZ, maxX, maxY, maxZ;
    int lastCalcId = -1;
    double energySupplied;
    double maxPacketConducted;
    double maxPacketOffered;

    EnergyPath(Node source, Node target, java.util.List<Node> conductors, double loss) {
        this(source, target, conductors, loss, null);
    }

    EnergyPath(Node source, Node target, java.util.List<Node> conductors, double loss, net.minecraft.core.Direction knownTargetDirection) {
        this.source = source;
        this.target = target;
        this.conductors = conductors;
        this.loss = loss;

        if (knownTargetDirection != null) {
            this.targetDirection = knownTargetDirection;
        } else {
            NodeLink lastLink = conductors.isEmpty()
                    ? source.getLinkTo(target)
                    : target.getLinkTo(conductors.get(conductors.size() - 1));
            this.targetDirection = (lastLink != null) ? lastLink.getDirFrom(target) : null;
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        double minInsulationEnergyAbsorption = Double.POSITIVE_INFINITY;
        double minInsulationBreakdownEnergy = Double.POSITIVE_INFINITY;
        double minConductorBreakdownEnergy = Double.POSITIVE_INFINITY;

        for (Node node : conductors) {
            IEnergyConductor conductor = (IEnergyConductor) node.getTile().getMainTile();
            minInsulationEnergyAbsorption = Math.min(minInsulationEnergyAbsorption, conductor.getInsulationEnergyAbsorption());
            minInsulationBreakdownEnergy = Math.min(minInsulationBreakdownEnergy, conductor.getInsulationBreakdownEnergy());
            minConductorBreakdownEnergy = Math.min(minConductorBreakdownEnergy, conductor.getConductorBreakdownEnergy());
            for (IEnergyTile tile : node.getTile().getSubTiles()) {
                BlockPos pos = EnergyNetGlobal.getPos(tile);
                minX = Math.min(minX, pos.getX()); minY = Math.min(minY, pos.getY()); minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX()); maxY = Math.max(maxY, pos.getY()); maxZ = Math.max(maxZ, pos.getZ());
            }
        }
        this.minEffectEnergy = Math.min(Math.min(minInsulationEnergyAbsorption, minInsulationBreakdownEnergy), minConductorBreakdownEnergy);
        this.minInsulationEnergyAbsorption = minInsulationEnergyAbsorption;
        this.minInsulationBreakdownEnergy = minInsulationBreakdownEnergy;
        this.minConductorBreakdownEnergy = minConductorBreakdownEnergy;
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
    }
}

