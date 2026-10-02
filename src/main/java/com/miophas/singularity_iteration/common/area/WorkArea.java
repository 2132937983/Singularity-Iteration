package com.miophas.singularity_iteration.common.area;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;

/**
 * One world-space region a machine acts on, as shown by the Area Preview.
 *
 * @param box    world-space box (block edges, max exclusive)
 * @param color  ARGB colour; alpha is ignored, the renderer applies its own translucency
 * @param kind   {@link #PRIMARY} for the actual work volume, {@link #MARKER} for a single
 *               highlighted cell (intake, current target), {@link #ENVELOPE} for an outer
 *               search limit drawn as an outline only
 */
public record WorkArea(AABB box, int color, int kind) {
    public static final int PRIMARY = 0, MARKER = 1, ENVELOPE = 2;

    /** Palette shared by all machines so a colour always means the same thing. */
    public static final int MINING = 0xFFE0B020, DEFENCE = 0xFFC8423C, FLUID = 0xFF3FA7D6,
        COLLECT = 0xFF3C9A52, FARM = 0xFF7CB342, TERRAFORM = 0xFF8D6E63, CHUNK = 0xFF8E6CC8, FIELD = 0xFF3A6EA5;

    public static WorkArea cube(BlockPos center, int radius, int color) {
        return new WorkArea(new AABB(center.getX() - radius, center.getY() - radius, center.getZ() - radius,
            center.getX() + radius + 1, center.getY() + radius + 1, center.getZ() + radius + 1), color, PRIMARY);
    }

    public static WorkArea box(BlockPos center, int dx, int down, int up, int dz, int color) {
        return new WorkArea(new AABB(center.getX() - dx, center.getY() - down, center.getZ() - dz,
            center.getX() + dx + 1, center.getY() + up + 1, center.getZ() + dz + 1), color, PRIMARY);
    }

    public static WorkArea cell(BlockPos pos, int color) {
        return new WorkArea(new AABB(pos), color, MARKER);
    }

    public WorkArea asEnvelope() {
        return new WorkArea(box, color, ENVELOPE);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(box.minX); buf.writeDouble(box.minY); buf.writeDouble(box.minZ);
        buf.writeDouble(box.maxX); buf.writeDouble(box.maxY); buf.writeDouble(box.maxZ);
        buf.writeInt(color);
        buf.writeByte(kind);
    }

    public static WorkArea read(FriendlyByteBuf buf) {
        AABB box = new AABB(buf.readDouble(), buf.readDouble(), buf.readDouble(),
            buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new WorkArea(box, buf.readInt(), buf.readUnsignedByte());
    }
}
