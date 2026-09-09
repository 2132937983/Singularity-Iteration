package com.singularity_iteration.mio_icif.energy.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Node {
    final int uid;
    final Tile tile;
    final NodeType nodeType;
    private boolean isExtraNode = false;
    private Grid grid;
    List<NodeLink> links = new ArrayList<>();

    Node(int uid, Tile tile, NodeType nodeType) {
        if (tile == null) throw new NullPointerException("null tile");
        if (nodeType == null) throw new NullPointerException("null node type");

        // Type checks - only if the main tile implements the appropriate interface
        this.uid = uid;
        this.tile = tile;
        this.nodeType = nodeType;
    }

    public Tile getTile() {
        return this.tile;
    }

    public NodeType getType() {
        return this.nodeType;
    }

    boolean isExtraNode() {
        return this.isExtraNode;
    }

    void setExtraNode(boolean isExtraNode) {
        if (this.nodeType == NodeType.Conductor)
            throw new IllegalStateException("A conductor can't be an extra node.");
        this.isExtraNode = isExtraNode;
    }

    public Grid getGrid() {
        return this.grid;
    }

    void setGrid(Grid grid) {
        if (grid == null) throw new NullPointerException("null grid");
        assert this.grid == null;
        this.grid = grid;
    }

    void clearGrid() {
        assert this.grid != null;
        this.grid = null;
    }

    public Collection<NodeLink> getLinks() {
        return this.links;
    }

    public NodeLink getLinkTo(Node node) {
        for (NodeLink link : this.links) {
            if (link.getNeighbor(this) == node)
                return link;
        }
        return null;
    }

    double getInnerLoss() {
        switch (this.nodeType) {
            case Source:
                return 0.002D;
            case Sink:
                return 0.002D;
            case Conductor:
                if (this.tile.getMainTile() instanceof IEnergyConductor) {
                    return ((IEnergyConductor) this.tile.getMainTile()).getConductionLoss();
                }
                return 0.002D;
        }
        throw new RuntimeException("invalid nodetype: " + String.valueOf(this.nodeType));
    }

    @Override
    public String toString() {
        String type = null;
        switch (this.nodeType) {
            case Source: type = "E"; break;
            case Sink: type = "A"; break;
            case Conductor: type = "C"; break;
        }
        return String.valueOf(this.tile) + "|" + String.valueOf(this.tile) + "|" + type;
    }
}

