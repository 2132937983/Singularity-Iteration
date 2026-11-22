package com.miophas.singularity_iteration.common.block.generator;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.energy.ICableBlock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.154: the cells around a solar panel that its model (SCXSYGL's models) reaches into, and how the panels tie into a
 * grid.
 *
 * <p>A panel's wings and tilted panels overhang some of the cells around it. In an {@link Kind#AIR} cell nothing may be
 * placed; in a {@link Kind#CABLE} cell the wing sits in the top few pixels, so a cable fits under it but a block does not.
 * Every other cell is free. A panel is not placed where its cells are taken, and nothing is placed into a panel's cells
 * (players, the blueprint builder and the pump all post the placement event).
 *
 * <p>The cells are in the model's own frame (front to the north, -z) and turn with the panel's facing, like the model.
 *
 * <p>Grid-tie: the photon resonance line (advanced METS, photon resonance, ultimate photon resonance) joins through its
 * front and back (its wings take the sides); the advanced line (advanced, hybrid, ultimate hybrid) through its left and
 * right (its panels take the front and back). The faces that join have a coloured line under the port; the others only
 * the two upright lines. The basic panel (nothing beside it) ties on all four sides, so it lies in arrays (0.1.11.155); the
 * quantum panel (wings on all four sides) does not tie in.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class SolarFootprint {
    private SolarFootprint() {}

    public enum Kind { AIR, CABLE }

    /** How a panel ties into a grid. */
    public enum Tie { NONE, FRONT_BACK, SIDES, ALL }

    /** One cell, in the model's frame: x east, y up, z south (the back); the front is z = -1. */
    public record Cell(int dx, int dy, int dz, Kind kind) {
        BlockPos offset(Direction facing) { return new BlockPos(dx, dy, dz).rotate(rotation(facing)); }
    }

    private record Shape(List<Cell> cells, Tie tie) {}

    private static Cell air(int x, int y, int z) { return new Cell(x, y, z, Kind.AIR); }
    private static Cell cable(int x, int y, int z) { return new Cell(x, y, z, Kind.CABLE); }

    /** The cells of each panel (from its model; the screenshots of the seven panels in a stone frame agree). */
    private static final Map<String, Shape> SHAPES = Map.of(
        // the tilted panel stands into the cell above
        "generator/block_solar_generator", new Shape(List.of(air(0, 1, 0)), Tie.ALL),   // 0.1.11.155: ties on all four sides
        // a flat panel over the front cell, the tilted one up and to the back
        "generator/block_advanced_solar_panel", new Shape(List.of(cable(0, 0, -1), air(0, 1, 0), air(0, 1, 1)), Tie.SIDES),
        "generator/block_hybrid_solar_panel", new Shape(List.of(cable(0, 0, -1), air(0, 1, 0), air(0, 1, 1)), Tie.SIDES),
        // the flat panel lies one cell higher, the tilted one reaches two cells up at the back
        "generator/block_ultimate_hybrid_solar_panel", new Shape(List.of(air(0, 1, -1), air(0, 1, 0), air(0, 1, 1), air(0, 2, 1)), Tie.SIDES),
        // wings on all four sides
        "generator/block_quantum_solar_panel", new Shape(List.of(cable(-1, 0, 0), cable(1, 0, 0), cable(0, 0, -1), cable(0, 0, 1),
            air(0, 1, 0), air(-1, 1, 0), air(1, 1, 0), air(0, 1, -1), air(0, 1, 1)), Tie.NONE),
        // wings to the left and right
        "generator/block_mets_advanced_solar_generator", new Shape(List.of(cable(-1, 0, 0), cable(1, 0, 0),
            air(0, 1, 0), air(-1, 1, 0), air(1, 1, 0)), Tie.FRONT_BACK),
        "generator/block_photon_resonance_solar_generator", new Shape(List.of(cable(-1, 0, 0), cable(1, 0, 0),
            air(0, 1, 0), air(-1, 1, 0), air(1, 1, 0)), Tie.FRONT_BACK),
        // its wings sit higher: the cells beside it are free
        "generator/block_ultimate_photon_resonance_solar_generator", new Shape(List.of(air(0, 1, 0), air(-1, 1, 0), air(1, 1, 0)), Tie.FRONT_BACK));

    /** One way a cell can be taken: a panel of {@code block} facing {@code facing} at {@code -offset} from the cell. */
    private record Probe(Block block, Direction facing, Kind kind) {}

    private static volatile Map<Block, Shape> byBlock;
    /** Offset from a cell to the panel that could take it, with the panels that would. */
    private static volatile Map<BlockPos, List<Probe>> probes;

    private static Map<Block, Shape> byBlock() {
        Map<Block, Shape> known = byBlock;
        if (known != null) return known;
        Map<Block, Shape> map = new IdentityHashMap<>();
        Map<BlockPos, List<Probe>> reach = new HashMap<>();
        for (var e : SHAPES.entrySet()) {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, e.getKey()));
            if (block == null || block.defaultBlockState().isAir()) continue;
            map.put(block, e.getValue());
            for (Direction facing : Direction.Plane.HORIZONTAL)
                for (Cell cell : e.getValue().cells())
                    reach.computeIfAbsent(cell.offset(facing), k -> new ArrayList<>()).add(new Probe(block, facing, cell.kind()));
        }
        probes = Map.copyOf(reach);
        byBlock = map;
        return map;
    }

    private static Rotation rotation(Direction facing) {
        return switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static Direction facing(BlockState state) {
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
    }

    /** Whether the block is one of the solar panels with a footprint. */
    public static boolean isPanel(BlockState state) { return byBlock().containsKey(state.getBlock()); }

    /** The cells a panel in this state at {@code pos} takes, in the world. */
    public static Map<BlockPos, Kind> cells(BlockState state, BlockPos pos) {
        Shape shape = byBlock().get(state.getBlock());
        if (shape == null) return Map.of();
        Map<BlockPos, Kind> out = new HashMap<>();
        Direction facing = facing(state);
        for (Cell cell : shape.cells()) out.merge(pos.offset(cell.offset(facing)), cell.kind(), SolarFootprint::stricter);
        return out;
    }

    private static Kind stricter(Kind a, Kind b) { return a == Kind.AIR || b == Kind.AIR ? Kind.AIR : Kind.CABLE; }

    /** How a cell is taken by the panels around it ({@link Kind#AIR} before {@link Kind#CABLE}), or {@code null} when free. */
    public static @Nullable Kind takenAt(BlockGetter level, BlockPos at) {
        byBlock();
        Kind taken = null;
        for (var e : probes.entrySet()) {
            BlockState there = level.getBlockState(at.subtract(e.getKey()));
            if (!byBlock.containsKey(there.getBlock())) continue;
            Direction facing = facing(there);
            for (Probe probe : e.getValue()) {
                if (probe.block() != there.getBlock() || probe.facing() != facing) continue;
                if (probe.kind() == Kind.AIR) return Kind.AIR;
                taken = Kind.CABLE;
            }
        }
        return taken;
    }

    /** Whether {@code placed} may go into a cell taken this way. */
    public static boolean allows(@Nullable Kind kind, BlockState placed) {
        return kind == null || placed.isAir() || kind == Kind.CABLE && placed.getBlock() instanceof ICableBlock;
    }

    /**
     * Whether a panel in {@code state} fits at {@code pos}: its AIR cells are empty (or replaceable, like grass), its CABLE
     * cells empty or a cable, and its own cell is not taken by another panel.
     */
    public static boolean fits(BlockGetter level, BlockPos pos, BlockState state, @Nullable BlockPlaceContext context) {
        if (!allows(takenAt(level, pos), state)) return false;
        for (var e : cells(state, pos).entrySet()) {
            BlockState there = level.getBlockState(e.getKey());
            boolean empty = there.isAir() || (context != null ? there.canBeReplaced(context) : there.canBeReplaced());
            if (!empty && !(e.getValue() == Kind.CABLE && there.getBlock() instanceof ICableBlock)) return false;
        }
        return true;
    }

    /**
     * The placement state of a panel, or {@code null} when it does not fit (the item stays in the hand). The panel's
     * blocks call this from {@code getStateForPlacement}.
     */
    public static @Nullable BlockState place(BlockPlaceContext context, @Nullable BlockState state) {
        if (state == null || fits(context.getLevel(), context.getClickedPos(), state, context)) return state;
        if (context.getPlayer() != null && context.getLevel().isClientSide())
            context.getPlayer().displayClientMessage(Component.translatable("message.mio_icif.solar.no_room"), true);
        return null;
    }

    // ---- grid-tie ----------------------------------------------------------------------------------------------------

    public static Tie tie(BlockState state) {
        Shape shape = byBlock().get(state.getBlock());
        return shape == null ? Tie.NONE : shape.tie();
    }

    private static final Direction[] NONE = new Direction[0];

    /** The faces through which a panel in this state ties into its neighbours. */
    public static Direction[] gridSides(BlockState state) {
        Direction facing = facing(state);
        return switch (tie(state)) {
            case FRONT_BACK -> new Direction[]{facing, facing.getOpposite()};
            case SIDES -> new Direction[]{facing.getClockWise(), facing.getCounterClockWise()};
            case ALL -> new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            case NONE -> NONE;
        };
    }

    /** Whether panel {@code a} ties to panel {@code b} next to it towards {@code side}: the same panel, both faces joining. */
    public static boolean joins(BlockState a, Direction side, BlockState b) {
        if (a.getBlock() != b.getBlock() || tie(a) == Tie.NONE) return false;
        boolean out = false, in = false;
        for (Direction d : gridSides(a)) out |= d == side;
        for (Direction d : gridSides(b)) in |= d == side.getOpposite();
        return out && in;
    }

    // ---- placing into a panel's cells -------------------------------------------------------------------------------

    /**
     * 0.1.11.156: a player's block is refused before it is placed, on the client and the server alike, so the client does
     * not show it for a moment and take it from the hand (0.1.11.154 only undid the placement on the server: the block
     * flashed and the stack stayed one short on the client). The block's own use (a chest beside a panel) still works.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof net.minecraft.world.item.BlockItem item)) return;
        BlockPos at = new BlockPlaceContext(event.getEntity(), event.getHand(), event.getItemStack(), event.getHitVec()).getClickedPos();
        if (allows(takenAt(event.getLevel(), at), item.getBlock().defaultBlockState())) return;
        event.setUseItem(TriState.FALSE);
        if (event.getLevel().isClientSide())
            event.getEntity().displayClientMessage(Component.translatable("message.mio_icif.solar.cell_taken"), true);
    }

    /** The placement event: blocks placed some other way (the blueprint builder, the pump, other mods). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        List<BlockSnapshot> placed = event instanceof BlockEvent.EntityMultiPlaceEvent multi ? multi.getReplacedBlockSnapshots() : List.of(event.getBlockSnapshot());
        for (BlockSnapshot snapshot : placed) {
            BlockPos at = snapshot.getPos();
            BlockState state = event.getLevel().getBlockState(at);
            // the cell is another panel's, or (a panel placed some other way than by hand) the panel's own cells are taken
            if (!allows(takenAt(event.getLevel(), at), state) || isPanel(state) && !fits(event.getLevel(), at, state, null)) {
                event.setCanceled(true);
                if (event.getEntity() instanceof Player player) player.displayClientMessage(Component.translatable("message.mio_icif.solar.cell_taken"), true);
                // the stack is given back on the server; the client took it already, so send it the inventory again
                if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                    player.inventoryMenu.sendAllDataToRemote();
                    player.containerMenu.sendAllDataToRemote();
                }
                return;
            }
        }
    }
}
