package com.miophas.singularity_iteration.common.blockentity.generator;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * 0.1.10.39: the shape of the Steam Tunnel (IC2 Classic's {@code SteamTunnelTileEntity.testStructure}): a hollow box, 3 x 3 across
 * and 5 long, behind the controller, which sits in the centre of the front end and faces out of it.
 * <ul>
 *   <li>{@link Role#EDGE}: the 28 positions on two or more sides of the box: casing.</li>
 *   <li>{@link Role#FACE}: the 13 other shell positions (the middles of the four long sides and the centre of the far end):
 *   casing, a fluid port or a power port.</li>
 *   <li>{@link Role#INTERIOR}: the 3 positions inside: air.</li>
 * </ul>
 */
public final class SteamTunnelStructure {
    private SteamTunnelStructure() {}

    public static final int LENGTH = 5;

    public enum Role { CONTROLLER, EDGE, FACE, INTERIOR }

    /** The 45 positions of the tunnel whose controller is at {@code controller} and faces {@code facing} (horizontal). */
    public static Map<BlockPos, Role> layout(BlockPos controller, Direction facing) {
        Direction back = facing.getOpposite(), right = facing.getClockWise();
        Map<BlockPos, Role> out = new LinkedHashMap<>();
        for (int depth = 0; depth < LENGTH; depth++)
            for (int up = -1; up <= 1; up++)
                for (int side = -1; side <= 1; side++) {
                    BlockPos pos = controller.relative(back, depth).relative(right, side).above(up);
                    int sides = (depth == 0 || depth == LENGTH - 1 ? 1 : 0) + (side != 0 ? 1 : 0) + (up != 0 ? 1 : 0);
                    Role role = depth == 0 && side == 0 && up == 0 ? Role.CONTROLLER
                        : sides == 0 ? Role.INTERIOR : sides == 1 ? Role.FACE : Role.EDGE;
                    out.put(pos, role);
                }
        return out;
    }

    /** True when {@code pos} may belong to a tunnel controlled from {@code controller} (the reach of a part's removal). */
    public static boolean inReach(BlockPos controller, BlockPos pos) {
        return Math.abs(controller.getX() - pos.getX()) < LENGTH && Math.abs(controller.getY() - pos.getY()) <= 1
            && Math.abs(controller.getZ() - pos.getZ()) < LENGTH;
    }
}
