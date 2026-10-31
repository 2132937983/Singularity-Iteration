package com.miophas.singularity_iteration.common.blockentity.generator;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.39: the shape of the Steam Tunnel (the same shape as in IC2 Classic): a hollow box, 3 x 3 across
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

    /**
     * The piece of the big fan that a casing of the front ring shows on its front face while the tunnel is formed (the controller in
     * the middle is the hub): the side the front faces, then the place in the 3 x 3 as seen from outside. {@link #NONE} on every
     * other casing.
     */
    public enum Fan implements StringRepresentable {
        NONE,
        NORTH_TOP_LEFT, NORTH_TOP, NORTH_TOP_RIGHT, NORTH_LEFT, NORTH_RIGHT, NORTH_BOTTOM_LEFT, NORTH_BOTTOM, NORTH_BOTTOM_RIGHT,
        EAST_TOP_LEFT, EAST_TOP, EAST_TOP_RIGHT, EAST_LEFT, EAST_RIGHT, EAST_BOTTOM_LEFT, EAST_BOTTOM, EAST_BOTTOM_RIGHT,
        SOUTH_TOP_LEFT, SOUTH_TOP, SOUTH_TOP_RIGHT, SOUTH_LEFT, SOUTH_RIGHT, SOUTH_BOTTOM_LEFT, SOUTH_BOTTOM, SOUTH_BOTTOM_RIGHT,
        WEST_TOP_LEFT, WEST_TOP, WEST_TOP_RIGHT, WEST_LEFT, WEST_RIGHT, WEST_BOTTOM_LEFT, WEST_BOTTOM, WEST_BOTTOM_RIGHT;

        private static final Fan[] VALUES = values();
        private static final List<Direction> FRONTS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
        /** Column (-1 left, 1 right) and row (1 top, -1 bottom) of the 8 places, in the order of the constants. */
        private static final int[][] PLACES = {{-1, 1}, {0, 1}, {1, 1}, {-1, 0}, {1, 0}, {-1, -1}, {0, -1}, {1, -1}};
        private static final String[] PLACE_NAMES = {"top_left", "top", "top_right", "left", "right", "bottom_left", "bottom", "bottom_right"};
        /** The place that left and right swap into. */
        private static final int[] MIRRORED = {2, 1, 0, 4, 3, 7, 6, 5};

        private final String name = name().toLowerCase(Locale.ROOT);

        @Override
        public String getSerializedName() { return name; }

        /** The side the fan faces (the controller's facing); null for {@link #NONE}. */
        public @Nullable Direction front() { return this == NONE ? null : FRONTS.get((ordinal() - 1) / 8); }

        private int place() { return (ordinal() - 1) % 8; }

        /** The place in the 3 x 3 as seen from outside ({@code top_left} ... {@code bottom_right}); not for {@link #NONE}. */
        public String placeName() { return PLACE_NAMES[place()]; }

        /** Where this piece sits in front of the controller at {@code controller} (not for {@link #NONE}). */
        public BlockPos casing(BlockPos controller) {
            int[] at = PLACES[place()];
            return controller.relative(front().getCounterClockWise(), at[0]).above(at[1]);   // seen from outside, left is clockwise
        }

        /** The 8 pieces of a fan facing {@code front} (horizontal). */
        public static List<Fan> of(Direction front) {
            int first = 1 + 8 * FRONTS.indexOf(front);
            return Arrays.asList(VALUES).subList(first, first + 8);
        }

        private static Fan of(Direction front, int place) { return VALUES[1 + 8 * FRONTS.indexOf(front) + place]; }

        public Fan rotate(Rotation rotation) { return this == NONE ? NONE : of(rotation.rotate(front()), place()); }

        public Fan mirror(Mirror mirror) { return this == NONE ? NONE : of(mirror.mirror(front()), MIRRORED[place()]); }
    }
}
