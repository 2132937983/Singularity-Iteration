package com.singularity_iteration.mio_icif.energy.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Tile {
    private final IEnergyTile mainTile;
    final List<IEnergyTile> subTiles;
    final List<Node> nodes = new ArrayList<>();

    private boolean disabled;
    private double amount;
    private int packetCount;

    Tile(EnergyNetLocal enet, IEnergyTile mainTile, List<IEnergyTile> subTiles) {
        this.mainTile = mainTile;
        this.subTiles = subTiles;
        if (!(mainTile instanceof IEnergyConductor) && mainTile instanceof IEnergySource)
            this.nodes.add(new Node(enet.allocateNodeId(), this, NodeType.Source));
        if (!(mainTile instanceof IEnergyConductor) && mainTile instanceof IEnergySink)
            this.nodes.add(new Node(enet.allocateNodeId(), this, NodeType.Sink));
        if (mainTile instanceof IEnergyConductor)
            this.nodes.add(new Node(enet.allocateNodeId(), this, NodeType.Conductor));
    }

    public IEnergyTile getMainTile() {
        return this.mainTile;
    }

    public Collection<Node> getNodes() {
        return this.nodes;
    }

    void addExtraNode(Node node) {
        node.setExtraNode(true);
        this.nodes.add(node);
    }

    boolean removeExtraNode(Node node) {
        boolean canBeRemoved = false;
        if (node.isExtraNode()) {
            canBeRemoved = true;
        } else {
            for (Node otherNode : this.nodes) {
                if (otherNode != node && otherNode.nodeType == node.nodeType && otherNode.isExtraNode()) {
                    otherNode.setExtraNode(false);
                    canBeRemoved = true;
                    break;
                }
            }
        }
        if (canBeRemoved)
            this.nodes.remove(node);
        return canBeRemoved;
    }

    public Collection<IEnergyTile> getSubTiles() {
        return this.subTiles;
    }

    IEnergyTile getSubTileAt(net.minecraft.core.BlockPos pos) {
        for (IEnergyTile subTile : this.subTiles) {
            if (EnergyNetGlobal.getPos(subTile).equals(pos))
                return subTile;
        }
        return null;
    }

    void setDisabled() {
        this.disabled = true;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getPacketCount() {
        return this.packetCount;
    }

    public void setSourceData(double amount, int packetCount) {
        this.amount = amount;
        this.packetCount = packetCount;
    }

    @Override
    public String toString() {
        String ret = getTeClassName(this.mainTile);
        return ret;
    }

    private static String getTeClassName(Object o) {
        return o.getClass().getSimpleName().replace("TileEntity", "");
    }
}

