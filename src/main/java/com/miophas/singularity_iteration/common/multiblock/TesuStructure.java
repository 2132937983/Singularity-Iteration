package com.miophas.singularity_iteration.common.multiblock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.168: a box, 7 x 7 x 13 (it was a cylinder); every position of the cylinder keeps its role, the new ones are casing.
 *
 * <p>0.1.11.166: the top electrode is gone; the middle of the cap is casing.
 *
 * <p>0.1.11.158: the inside is all energy cells (189), and a port slot also takes a TESU wireless output port.
 *
 * <p>0.1.11.157: the TESU (太级储电罐) structure, read from {@code data/mio_icif/multiblock/tesu.json} (written by
 * {@code tools/tesu_layout_111157.py}; the sample world is built from the same file). A 7 x 7 x 13 cylinder: a base disc, a
 * ring with the controller and 8 port slots, a shell with reinforced-glass windows round a column of energy cells, a lid and
 * a cap of casing. Positions are kept relative to the controller as laid out (its front to the north); a
 * controller facing another way turns the whole structure with it.
 *
 * <p>A port slot takes a TESU input or output port, or a casing block to close it. The cells listed as air must be empty.
 */
public final class TesuStructure {
    public static final String RESOURCE = "/data/mio_icif/multiblock/tesu.json";

    public enum Part { CASING, GLASS, CELL, CONTROLLER, PORT_SLOT, AIR }

    /** A position relative to the controller (unturned) and what belongs there. */
    public record Cell(int x, int y, int z, Part part) {}

    /**
     * The result of a check: formed, or the first wrong position and what it should hold; the ports found; {@code unloaded}
     * when part of the structure lies in an unloaded chunk (nothing can be said then).
     */
    public record Check(boolean formed, boolean unloaded, @Nullable BlockPos wrongAt, @Nullable Part expected,
                        List<BlockPos> inputs, List<BlockPos> outputs, List<BlockPos> wireless) {}

    private static volatile TesuStructure instance;

    private final List<Cell> cells;

    private final it.unimi.dsi.fastutil.longs.LongOpenHashSet localOffsets;

    private TesuStructure(List<Cell> cells) {
        this.cells = List.copyOf(cells);
        var set = new it.unimi.dsi.fastutil.longs.LongOpenHashSet(this.cells.size());
        for (Cell c : this.cells) set.add(BlockPos.asLong(c.x(), c.y(), c.z()));
        this.localOffsets = set;
    }

    public static TesuStructure get() {
        TesuStructure s = instance;
        if (s == null) {
            synchronized (TesuStructure.class) {
                if (instance == null) instance = load();
                s = instance;
            }
        }
        return s;
    }

    public List<Cell> cells() { return cells; }

    private static Part part(String name) {
        return switch (name) {
            case "casing" -> Part.CASING;
            case "glass" -> Part.GLASS;
            case "cell" -> Part.CELL;
            case "controller" -> Part.CONTROLLER;
            case "port" -> Part.PORT_SLOT;
            default -> throw new IllegalArgumentException("unknown TESU part " + name);
        };
    }

    private static TesuStructure load() {
        try (InputStream in = TesuStructure.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException("missing " + RESOURCE);
            JsonObject layout = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            int cx = 0, cy = 0, cz = 0;
            for (JsonElement e : layout.getAsJsonArray("blocks")) {
                JsonArray b = e.getAsJsonArray();
                if (b.get(3).getAsString().equals("controller")) { cx = b.get(0).getAsInt(); cy = b.get(1).getAsInt(); cz = b.get(2).getAsInt(); }
            }
            List<Cell> cells = new ArrayList<>();
            for (JsonElement e : layout.getAsJsonArray("blocks")) {
                JsonArray b = e.getAsJsonArray();
                cells.add(new Cell(b.get(0).getAsInt() - cx, b.get(1).getAsInt() - cy, b.get(2).getAsInt() - cz, part(b.get(3).getAsString())));
            }
            for (JsonElement e : layout.getAsJsonArray("air")) {
                JsonArray b = e.getAsJsonArray();
                cells.add(new Cell(b.get(0).getAsInt() - cx, b.get(1).getAsInt() - cy, b.get(2).getAsInt() - cz, Part.AIR));
            }
            return new TesuStructure(cells);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Rotation rotation(Direction facing) {
        return switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** The world position of a cell for a controller at {@code controller} facing {@code facing}. */
    public static BlockPos at(BlockPos controller, Direction facing, Cell cell) {
        return controller.offset(new BlockPos(cell.x(), cell.y(), cell.z()).rotate(rotation(facing)));
    }

    /** Whether {@code state} is what {@code part} wants (a port slot: a port or casing). */
    public static boolean fits(Part part, BlockState state) {
        return switch (part) {
            case CASING -> state.is(mio_icif_blocks.TESU_CASING.get());
            case GLASS -> state.is(mio_icif_blocks.BLOCK_ALLOY_GLASS.get());
            case CELL -> state.is(mio_icif_blocks.TESU_CELL.get());
            case CONTROLLER -> state.is(mio_icif_blocks.TESU_CONTROLLER.get());
            case PORT_SLOT -> state.is(mio_icif_blocks.TESU_INPUT.get()) || state.is(mio_icif_blocks.TESU_OUTPUT.get())
                || state.is(mio_icif_blocks.TESU_WIRELESS_PORT.get()) || state.is(mio_icif_blocks.TESU_INDUCTION_PORT.get())   // 0.1.11.206
                || state.is(mio_icif_blocks.TESU_CASING.get());
            case AIR -> state.isAir();
        };
    }

    private static void setRotated(BlockPos.MutableBlockPos out, BlockPos controller, Direction facing, int x, int y, int z) {
        int rx, rz;
        switch (facing) {
            case EAST -> { rx = -z; rz = x; }
            case SOUTH -> { rx = -x; rz = -z; }
            case WEST -> { rx = z; rz = -x; }
            default -> { rx = x; rz = z; }
        }
        out.set(controller.getX() + rx, controller.getY() + y, controller.getZ() + rz);
    }

    /** Checks the structure of a controller at {@code controller} facing {@code facing}; stops at the first wrong cell. */
    public Check check(Level level, BlockPos controller, Direction facing) {
        List<BlockPos> inputs = new ArrayList<>(), outputs = new ArrayList<>(), wireless = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (Cell cell : cells) {
            setRotated(pos, controller, facing, cell.x(), cell.y(), cell.z());
            if (!level.isLoaded(pos)) return new Check(false, true, pos.immutable(), cell.part(), List.of(), List.of(), List.of());
            BlockState state = level.getBlockState(pos);
            if (!fits(cell.part(), state)) return new Check(false, false, pos.immutable(), cell.part(), List.of(), List.of(), List.of());
            if (cell.part() == Part.PORT_SLOT) {
                if (state.is(mio_icif_blocks.TESU_INPUT.get())) inputs.add(pos.immutable());
                else if (state.is(mio_icif_blocks.TESU_OUTPUT.get())) outputs.add(pos.immutable());
                else if (state.is(mio_icif_blocks.TESU_WIRELESS_PORT.get())) wireless.add(pos.immutable());   // 0.1.11.158
                else if (state.is(mio_icif_blocks.TESU_INDUCTION_PORT.get())) wireless.add(pos.immutable());   // 0.1.11.206: a wireless port too
            }
        }
        return new Check(true, false, null, null, List.copyOf(inputs), List.copyOf(outputs), List.copyOf(wireless));
    }

    /** Whether {@code pos} is one of the structure's cells for this controller (a block change there means a check). */
    public boolean contains(BlockPos controller, Direction facing, BlockPos pos) {
        int dx = pos.getX() - controller.getX();
        int dy = pos.getY() - controller.getY();
        int dz = pos.getZ() - controller.getZ();
        int lx, lz;
        switch (facing) {
            case EAST -> { lx = dz; lz = -dx; }
            case SOUTH -> { lx = -dx; lz = -dz; }
            case WEST -> { lx = -dz; lz = dx; }
            default -> { lx = dx; lz = dz; }
        }
        return localOffsets.contains(BlockPos.asLong(lx, dy, lz));
    }

    /** The plan symbol of a part (the JEI and manual plan, {@code BuiltinStructureBlueprints}); air is '.'. */
    public static char symbol(Part part) {
        return switch (part) {
            case CASING -> 'C';
            case GLASS -> 'G';
            case CELL -> 'E';
            case CONTROLLER -> 'K';
            case PORT_SLOT -> 'P';
            case AIR -> '.';
        };
    }

    /** The structure as plan layers, bottom up; each layer's rows run north to south, each row west to east (controller facing north). */
    public List<String[]> layers() {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Cell c : cells) {
            minX = Math.min(minX, c.x()); maxX = Math.max(maxX, c.x());
            minY = Math.min(minY, c.y()); maxY = Math.max(maxY, c.y());
            minZ = Math.min(minZ, c.z()); maxZ = Math.max(maxZ, c.z());
        }
        char[][][] grid = new char[maxY - minY + 1][maxZ - minZ + 1][maxX - minX + 1];
        for (char[][] layer : grid) for (char[] row : layer) java.util.Arrays.fill(row, '.');
        for (Cell c : cells) grid[c.y() - minY][c.z() - minZ][c.x() - minX] = symbol(c.part());
        List<String[]> out = new ArrayList<>();
        for (char[][] layer : grid) {
            String[] rows = new String[layer.length];
            for (int z = 0; z < layer.length; z++) rows[z] = new String(layer[z]);
            out.add(rows);
        }
        return out;
    }
}
