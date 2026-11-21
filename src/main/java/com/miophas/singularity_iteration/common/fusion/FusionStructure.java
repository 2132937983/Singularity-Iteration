package com.miophas.singularity_iteration.common.fusion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.92: the fusion reactor's structure, read from {@code data/mio_icif/multiblock/fusion_tokamak.json} (written by
 * {@code tools/fusion_layout_11589.py}; the sample world is built from the same file). Positions are kept relative to the
 * controller as laid out (facing east); a controller facing another way turns the whole structure with it.
 *
 * <p>Every block must be there: the central solenoid, the toroidal and poloidal field coils, the vacuum vessel (with its port
 * ducts), the blanket, the divertor, and air in the plasma channel. A port slot (a duct end, the cryogenic slot on a coil)
 * takes any fusion port, or a vacuum vessel block to leave it closed; which ports the reactor needs to run is the controller's
 * business, not the structure's.
 */
public final class FusionStructure {
    public static final String RESOURCE = "/data/mio_icif/multiblock/fusion_tokamak.json";

    public enum Part { CENTRAL_SOLENOID, TOROIDAL_COIL, POLOIDAL_COIL, VACUUM_VESSEL, BLANKET, DIVERTOR, PORT_SLOT, CONTROLLER, PLASMA }

    /** A position relative to the controller (unturned) and what belongs there. */
    public record Cell(int x, int y, int z, Part part) {}

    /** 0.1.11.93: a block of the layout as laid out (which port in which slot, and its facing), relative to the controller. */
    public record Laid(int x, int y, int z, String name, String facing) {}

    /** The result of a check: the ports found (in port slots), or the first position that is wrong and what it should hold. */
    public record Check(boolean formed, @Nullable BlockPos wrongAt, @Nullable Part expected, List<BlockPos> ports, BlockPos centre) {}

    private static volatile FusionStructure tokamak;

    private final List<Cell> cells;
    private final List<Laid> laid;
    private final int centreX, centreY, centreZ;
    private final int blanketCells, divertorCells, plasmaCells;

    private FusionStructure(List<Cell> cells, List<Laid> laid, int cx, int cy, int cz) {
        this.cells = List.copyOf(cells);
        this.laid = List.copyOf(laid);
        this.centreX = cx;
        this.centreY = cy;
        this.centreZ = cz;
        this.blanketCells = (int) cells.stream().filter(c -> c.part == Part.BLANKET).count();
        this.divertorCells = (int) cells.stream().filter(c -> c.part == Part.DIVERTOR).count();
        this.plasmaCells = (int) cells.stream().filter(c -> c.part == Part.PLASMA).count();
    }

    public static FusionStructure tokamak() {
        FusionStructure s = tokamak;
        if (s == null) {
            synchronized (FusionStructure.class) {
                if (tokamak == null) tokamak = load();
                s = tokamak;
            }
        }
        return s;
    }

    private static Part part(String name) {
        return switch (name) {
            case "central_solenoid" -> Part.CENTRAL_SOLENOID;
            case "toroidal_coil" -> Part.TOROIDAL_COIL;
            case "poloidal_coil" -> Part.POLOIDAL_COIL;
            case "vacuum_vessel" -> Part.VACUUM_VESSEL;
            case "blanket" -> Part.BLANKET;
            case "divertor" -> Part.DIVERTOR;
            case "controller" -> Part.CONTROLLER;
            default -> name.endsWith("_port") ? Part.PORT_SLOT : null;
        };
    }

    private static FusionStructure load() {
        try (InputStream in = FusionStructure.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException("missing " + RESOURCE);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            List<int[]> raw = new ArrayList<>();
            List<Part> parts = new ArrayList<>();
            List<String[]> names = new ArrayList<>();
            int[] controller = null;
            for (JsonElement e : root.getAsJsonArray("blocks")) {
                JsonArray b = e.getAsJsonArray();
                Part p = part(b.get(3).getAsString());
                if (p == null) throw new IllegalStateException("unknown fusion part " + b.get(3));
                int[] xyz = {b.get(0).getAsInt(), b.get(1).getAsInt(), b.get(2).getAsInt()};
                if (p == Part.CONTROLLER) {
                    if (!"east".equals(b.get(4).getAsString())) throw new IllegalStateException("the layout's controller must face east");
                    controller = xyz;
                }
                raw.add(xyz);
                parts.add(p);
                names.add(new String[]{b.get(3).getAsString(), b.get(4).getAsString()});
            }
            for (JsonElement e : root.getAsJsonArray("plasma")) {
                JsonArray b = e.getAsJsonArray();
                raw.add(new int[]{b.get(0).getAsInt(), b.get(1).getAsInt(), b.get(2).getAsInt()});
                parts.add(Part.PLASMA);
            }
            if (controller == null) throw new IllegalStateException("the layout has no controller");
            List<Cell> cells = new ArrayList<>(raw.size());
            List<Laid> laid = new ArrayList<>(names.size());
            for (int i = 0; i < raw.size(); i++) {
                int[] p = raw.get(i);
                cells.add(new Cell(p[0] - controller[0], p[1] - controller[1], p[2] - controller[2], parts.get(i)));
                if (i < names.size()) laid.add(new Laid(p[0] - controller[0], p[1] - controller[1], p[2] - controller[2], names.get(i)[0], names.get(i)[1]));
            }
            return new FusionStructure(cells, laid, -controller[0], -controller[1], -controller[2]);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + RESOURCE, e);
        }
    }

    public List<Cell> cells() { return cells; }
    /** The blocks as laid out (not the plasma), relative to the controller facing east. */
    public List<Laid> laid() { return laid; }
    public int blanketCells() { return blanketCells; }
    public int divertorCells() { return divertorCells; }
    public int plasmaCells() { return plasmaCells; }

    /** Clockwise quarter turns (seen from above) from the layout's east-facing controller to {@code facing}. */
    public static int turns(Direction facing) {
        return switch (facing) {
            case SOUTH -> 1;
            case WEST -> 2;
            case NORTH -> 3;
            default -> 0;
        };
    }

    /** A layout offset turned clockwise by {@code turns} quarter turns: east becomes south. */
    public static BlockPos turn(int x, int y, int z, int turns) {
        for (int i = 0; i < (turns & 3); i++) {
            int t = x;
            x = -z;
            z = t;
        }
        return new BlockPos(x, y, z);
    }

    /** The centre of the torus (on its midplane) for a controller at {@code controller} facing {@code facing}. */
    public BlockPos centre(BlockPos controller, Direction facing) {
        return controller.offset(turn(centreX, centreY, centreZ, turns(facing)));
    }

    public static boolean isPort(BlockState state) {
        return ports().contains(state.getBlock());
    }

    /** Built on first use: the layout is also read at bootstrap (the multiblock plans), before the blocks exist. */
    private static volatile Set<Block> ports;

    private static Set<Block> ports() {
        Set<Block> p = ports;
        if (p == null) {
            p = Set.of(mio_icif_blocks.FUSION_NBI_PORT.get(), mio_icif_blocks.FUSION_ECRH_PORT.get(), mio_icif_blocks.FUSION_FUEL_PORT.get(),
                mio_icif_blocks.FUSION_EXHAUST_PORT.get(), mio_icif_blocks.FUSION_CRYO_PORT.get(), mio_icif_blocks.FUSION_POWER_PORT.get(),
                mio_icif_blocks.FUSION_MITIGATION_PORT.get());
            ports = p;
        }
        return p;
    }

    /** The symbol of each part in the multiblock plan ('.' = unused: the plasma channel, which must be air). */
    public static char symbol(Part part) {
        return switch (part) {
            case CENTRAL_SOLENOID -> 'S';
            case TOROIDAL_COIL -> 'T';
            case POLOIDAL_COIL -> 'P';
            case VACUUM_VESSEL -> 'V';
            case BLANKET -> 'B';
            case DIVERTOR -> 'D';
            case PORT_SLOT -> 'O';
            case CONTROLLER -> 'K';
            case PLASMA -> '.';
        };
    }

    /** 0.1.11.94: the plan symbol of a toroidal coil block facing north or south ({@link #symbol} gives 'T': east or west). */
    public static final char COIL_NORTH_SOUTH = 'U';

    /** The structure as plan layers, bottom up; each layer's rows run north to south, each row west to east (controller facing east). */
    public List<String[]> layers() {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Cell c : cells) {
            minX = Math.min(minX, c.x); maxX = Math.max(maxX, c.x);
            minY = Math.min(minY, c.y); maxY = Math.max(maxY, c.y);
            minZ = Math.min(minZ, c.z); maxZ = Math.max(maxZ, c.z);
        }
        char[][][] grid = new char[maxY - minY + 1][maxZ - minZ + 1][maxX - minX + 1];
        for (char[][] layer : grid) for (char[] row : layer) java.util.Arrays.fill(row, '.');
        for (Cell c : cells) grid[c.y - minY][c.z - minZ][c.x - minX] = symbol(c.part);
        // 0.1.11.94: a toroidal coil block facing north or south (its coil runs north-south) is 'U', so the plan shows its facing
        for (Laid b : laid)
            if (b.name().equals("toroidal_coil") && (b.facing().equals("north") || b.facing().equals("south")))
                grid[b.y() - minY][b.z() - minZ][b.x() - minX] = COIL_NORTH_SOUTH;
        List<String[]> out = new ArrayList<>();
        for (char[][] layer : grid) {
            String[] rows = new String[layer.length];
            for (int z = 0; z < layer.length; z++) rows[z] = new String(layer[z]);
            out.add(rows);
        }
        return out;
    }

    private static Block block(Part part) {
        return switch (part) {
            case CENTRAL_SOLENOID -> mio_icif_blocks.FUSION_CENTRAL_SOLENOID.get();
            case TOROIDAL_COIL -> mio_icif_blocks.FUSION_TOROIDAL_COIL.get();
            case POLOIDAL_COIL -> mio_icif_blocks.FUSION_POLOIDAL_COIL.get();
            case VACUUM_VESSEL -> mio_icif_blocks.FUSION_VACUUM_VESSEL.get();
            case BLANKET -> mio_icif_blocks.FUSION_BLANKET.get();
            case DIVERTOR -> mio_icif_blocks.FUSION_DIVERTOR.get();
            case CONTROLLER -> mio_icif_blocks.FUSION_CONTROLLER.get();
            default -> null;
        };
    }

    /** Checks the structure around a controller; stops at the first wrong position (or one in an unloaded chunk). */
    public Check check(Level level, BlockPos controller, Direction facing) {
        int t = turns(facing);
        List<BlockPos> ports = new ArrayList<>();
        BlockPos centre = centre(controller, facing);
        for (Cell c : cells) {
            if (c.part == Part.CONTROLLER) continue;
            BlockPos at = controller.offset(turn(c.x, c.y, c.z, t));
            if (!level.isLoaded(at)) return new Check(false, at, c.part, ports, centre);
            BlockState s = level.getBlockState(at);
            boolean ok;
            switch (c.part) {
                case PLASMA -> ok = s.isAir();
                case PORT_SLOT -> {
                    ok = isPort(s) || s.is(mio_icif_blocks.FUSION_VACUUM_VESSEL.get());
                    if (ok && isPort(s)) ports.add(at.immutable());
                }
                // 0.1.11.139: a diagnostic viewport stands in for a blanket module or a vacuum vessel wall
                default -> ok = s.is(block(c.part))
                    || (c.part == Part.BLANKET || c.part == Part.VACUUM_VESSEL) && s.is(mio_icif_blocks.FUSION_VIEWPORT.get());
            }
            if (!ok) return new Check(false, at.immutable(), c.part, ports, centre);
        }
        return new Check(true, null, null, List.copyOf(ports), centre);
    }

    /** The world positions of every cell of a part (for the builder tests and the blanket's heat model). */
    public List<BlockPos> positions(BlockPos controller, Direction facing, Part part) {
        int t = turns(facing);
        List<BlockPos> out = new ArrayList<>();
        for (Cell c : cells) if (c.part == part) out.add(controller.offset(turn(c.x, c.y, c.z, t)));
        return out;
    }

    /** The block a cell needs (null for the plasma and for a port slot, which take more than one). */
    public static @Nullable Block required(Part part) {
        return block(part);
    }
}
