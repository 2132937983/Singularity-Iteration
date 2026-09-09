package com.singularity_iteration.mio_icif.energy.grid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public class NodeLink {
    Node nodeA;
    Node nodeB;
    Direction dirFromA;
    Direction dirFromB;
    double loss;
    List<Node> skippedNodes = new ArrayList<>();

    NodeLink(Node nodeA, Node nodeB, double loss) {
        this(nodeA, nodeB, loss, null, null);
        calculateDirections();
    }

    NodeLink(NodeLink link) {
        this(link.nodeA, link.nodeB, link.loss, link.dirFromA, link.dirFromB);
        this.skippedNodes.addAll(link.skippedNodes);
    }

    private NodeLink(Node nodeA1, Node nodeB1, double loss1, Direction dirFromA, Direction dirFromB) {
        assert nodeA1 != nodeB1;
        this.nodeA = nodeA1;
        this.nodeB = nodeB1;
        this.loss = loss1;
        this.dirFromA = dirFromA;
        this.dirFromB = dirFromB;
    }

    public Node getNeighbor(Node node) {
        return (this.nodeA == node) ? this.nodeB : this.nodeA;
    }

    Node getNeighbor(int uid) {
        return (this.nodeA.uid == uid) ? this.nodeB : this.nodeA;
    }

    public Node getNodeA() {
        return this.nodeA;
    }

    public Node getNodeB() {
        return this.nodeB;
    }

    public double getLoss() {
        return this.loss;
    }

    void replaceNode(Node oldNode, Node newNode) {
        if (this.nodeA == oldNode) {
            this.nodeA = newNode;
        } else {
            if (this.nodeB != oldNode)
                throw new IllegalArgumentException("Node " + String.valueOf(oldNode) + " isn't in " + String.valueOf(this) + ".");
            this.nodeB = newNode;
        }
    }

    public Direction getDirFrom(Node node) {
        if (this.nodeA == node)
            return this.dirFromA;
        return (this.nodeB == node) ? this.dirFromB : null;
    }

    @Override
    public String toString() {
        return "NodeLink:" + String.valueOf(this.nodeA) + "@" + String.valueOf(this.dirFromA) + "->" + String.valueOf(this.nodeB) + "@" + String.valueOf(this.dirFromB);
    }

    private void calculateDirections() {
        for (IEnergyTile tileA : this.nodeA.tile.subTiles) {
            for (IEnergyTile tileB : this.nodeB.tile.subTiles) {
                BlockPos delta = EnergyNetGlobal.getPos(tileB).subtract(
                        java.util.Objects.requireNonNull(EnergyNetGlobal.getPos(tileA)));
                for (Direction dir : Direction.values()) {
                    if (dir.getStepX() == delta.getX() && dir.getStepY() == delta.getY() && dir.getStepZ() == delta.getZ()) {
                        this.dirFromA = dir;
                        this.dirFromB = dir.getOpposite();
                        return;
                    }
                }
            }
        }
        BlockPos posA = EnergyNetGlobal.getPos(this.nodeA.tile.getMainTile());
        BlockPos posB = EnergyNetGlobal.getPos(this.nodeB.tile.getMainTile());
        BlockPos delta = posB.subtract(posA);
        int absDist = Math.abs(delta.getX()) + Math.abs(delta.getY()) + Math.abs(delta.getZ());
        if (absDist == 1) {
            for (Direction dir : Direction.values()) {
                if (dir.getStepX() == delta.getX() && dir.getStepY() == delta.getY() && dir.getStepZ() == delta.getZ()) {
                    this.dirFromA = dir;
                    this.dirFromB = dir.getOpposite();
                    return;
                }
            }
        }
        this.dirFromA = Direction.NORTH;
        this.dirFromB = Direction.SOUTH;
    }
}