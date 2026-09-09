package com.singularity_iteration.mio_icif.energy.grid;

public class GridInfo {
    public final int uid;
    public final int nodeCount;
    public final int complexNodes;
    public final int minX, minY, minZ;
    public final int maxX, maxY, maxZ;

    public GridInfo(int uid, int nodeCount, int complexNodes,
                    int minX, int minY, int minZ,
                    int maxX, int maxY, int maxZ) {
        this.uid = uid;
        this.nodeCount = nodeCount;
        this.complexNodes = complexNodes;
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
    }
}

