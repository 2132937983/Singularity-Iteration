package example.sicore;

import static example.sicore.Round33GameTests.call;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 0.1.11.188: what the Blueprint Builder places is what the blueprint shows: built bronze fluid pipes keep their joins (their
 * ports were shut), a turned or mirrored blueprint turns this mod's machines and pipes too, states a blueprint caught at work
 * (running, formed) go back to the default; the multiblock structures become blueprints (the tokamak with each port in its
 * slot, the TESU with input and output ports in turn) and a builder takes one; a TESU wireless port that fills a machine is
 * noted for the particle stream to it. Classes by name.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111188GameTests {
    private Round111188GameTests() {}

    private static final BlockPos AT = new BlockPos(1, 2, 1);

    private static Block block(String id) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)); }

    private static BooleanProperty side(BlockState s, Direction d) {
        return (BooleanProperty) s.getBlock().getStateDefinition().getProperty(d.getSerializedName());
    }

    private static void delete(Path dir) {
        try {
            if (Files.isDirectory(dir)) try (Stream<Path> files = Files.walk(dir)) {
                files.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
            }
        } catch (java.io.IOException ignored) { }
    }

    /** Two bronze pipes joined east-west, as a structure file. */
    private static byte[] pipes() throws Exception {
        CompoundTag root = new CompoundTag();
        ListTag size = new ListTag();
        size.add(IntTag.valueOf(2)); size.add(IntTag.valueOf(1)); size.add(IntTag.valueOf(1));
        root.put("size", size);
        ListTag palette = new ListTag();
        for (String joined : new String[]{"east", "west"}) {
            CompoundTag state = new CompoundTag();
            state.put("Name", StringTag.valueOf("mio_icif:pipe/fluid_pipe_bronze"));
            CompoundTag props = new CompoundTag();
            for (String d : new String[]{"down", "up", "north", "south", "west", "east"}) props.putString(d, d.equals(joined) ? "true" : "false");
            props.putString("size", "medium");
            state.put("Properties", props);
            palette.add(state);
        }
        root.put("palette", palette);
        ListTag blocks = new ListTag();
        for (int x = 0; x < 2; x++) {
            CompoundTag b = new CompoundTag();
            b.putInt("state", x);
            ListTag pos = new ListTag();
            pos.add(IntTag.valueOf(x)); pos.add(IntTag.valueOf(0)); pos.add(IntTag.valueOf(0));
            b.put("pos", pos);
            blocks.add(b);
        }
        root.put("blocks", blocks);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NbtIo.writeCompressed(root, out);
        return out.toByteArray();
    }

    /** Built bronze pipes are joined as drawn, and stay joined after their block entities have ticked. */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void builtFluidPipesKeepTheirJoins(GameTestHelper h) throws Exception {
        h.setBlock(AT, block("producer/block_blueprint_builder"));
        h.setBlock(new BlockPos(1, 2, 3), block("producer/block_infinite_material_interface"));
        BlockEntity builder = h.getBlockEntity(AT);
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        Class<?> up = Round33GameTests.common("builder.BlueprintUploads");
        Path dir = (Path) call(up, "folder", p.getUUID());
        List<?> plan;
        try {
            byte[] file = pipes();
            h.assertTrue((int) call(up, "begin", p, builder, "pipes.nbt", file.length, call(up, "sha256", file)) == 1, "the upload is not accepted");
            h.assertTrue((int) call(up, "chunk", p, 0, file) == 2, "the upload is not done");
            plan = (List<?>) call(builder, "plan");
            h.assertTrue(plan != null && plan.size() == 2, "the plan is " + plan);
            call(builder, "apiSetEnergy", 200_000L);
            call(builder, "action", p, 6, 0);   // A_START
        } finally {
            delete(dir);
            p.discard();
        }
        BlockPos west = (BlockPos) call(plan.get(0), "pos"), east = (BlockPos) call(plan.get(1), "pos");
        if (west.getX() > east.getX()) { BlockPos t = west; west = east; east = t; }
        BlockPos w = west, e = east;
        long[] built = {-1};
        h.succeedWhen(() -> {
            h.assertTrue((int) call(builder, "status") == 4, "the builder is not done: status " + call(builder, "status"));
            if (built[0] < 0) built[0] = h.getLevel().getGameTime();
            h.assertTrue(h.getLevel().getGameTime() - built[0] >= 10, "the pipes have not ticked yet");   // their block entities update the state
            BlockState a = h.getLevel().getBlockState(w), b = h.getLevel().getBlockState(e);
            h.assertTrue(a.getValue(side(a, Direction.EAST)) && b.getValue(side(b, Direction.WEST)), "the built pipes are not joined: " + a + " / " + b);
            h.assertTrue(!(boolean) call(h.getLevel().getBlockEntity(w), "isDirectionBlocked", Direction.EAST), "the west pipe's east port is shut");
            h.assertTrue((boolean) call(h.getLevel().getBlockEntity(w), "isDirectionBlocked", Direction.NORTH), "a side the blueprint does not join is open");
        });
    }

    /** A turned or mirrored blueprint turns this mod's machines and pipes (they do not turn themselves); vanilla blocks turn as before. */
    @GameTest(template = "empty")
    public static void turnedBlueprintsTurnOwnBlocks(GameTestHelper h) {
        Class<?> bp = Round33GameTests.common("builder.Blueprint");
        BlockState furnace = block("producer/block_furnace_elc").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        BlockState turned = (BlockState) call(bp, "turn", furnace, Rotation.CLOCKWISE_90, Mirror.NONE);
        h.assertTrue(turned.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST, "the machine faces " + turned.getValue(BlockStateProperties.HORIZONTAL_FACING));
        BlockState mirrored = (BlockState) call(bp, "turn", furnace, Rotation.NONE, Mirror.LEFT_RIGHT);
        h.assertTrue(mirrored.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.SOUTH, "the mirrored machine faces " + mirrored.getValue(BlockStateProperties.HORIZONTAL_FACING));
        BlockState pipe = block("pipe/fluid_pipe_bronze").defaultBlockState();
        pipe = pipe.setValue(side(pipe, Direction.NORTH), true).setValue(side(pipe, Direction.UP), true);
        BlockState p2 = (BlockState) call(bp, "turn", pipe, Rotation.CLOCKWISE_90, Mirror.NONE);
        h.assertTrue(p2.getValue(side(p2, Direction.EAST)) && !p2.getValue(side(p2, Direction.NORTH)) && p2.getValue(side(p2, Direction.UP)),
            "the turned pipe joins " + p2);
        BlockState vanilla = Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        BlockState v2 = (BlockState) call(bp, "turn", vanilla, Rotation.CLOCKWISE_90, Mirror.NONE);
        h.assertTrue(v2.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST, "the vanilla furnace faces " + v2.getValue(BlockStateProperties.HORIZONTAL_FACING));
        // what a blueprint caught at work is placed as the default: running and formed go
        BlockState working = block("fusion/block_fusion_controller").defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
        for (var prop : working.getProperties())
            if (prop instanceof BooleanProperty b && (b.getName().equals("active") || b.getName().equals("formed"))) working = working.setValue(b, true);
        BlockState placed = (BlockState) call(Round33GameTests.common("builder.BlueprintBuilderBlockEntity"), "target", working);
        for (var prop : placed.getProperties())
            if (prop instanceof BooleanProperty b && (b.getName().equals("active") || b.getName().equals("formed")))
                h.assertTrue(!placed.getValue(b), "the built controller is still " + b.getName());
        h.assertTrue(placed.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.WEST, "the facing went: " + placed);
        h.succeed();
    }

    /** Every multiblock structure becomes a blueprint; the tokamak has each port once in its slot, the TESU 4 input and 4 output ports. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void multiblockStructuresBecomeBlueprints(GameTestHelper h) {
        Class<?> st = Round33GameTests.common("builder.BuilderStructures");
        List<?> all = (List<?>) call(st, "all");
        h.assertTrue(all.size() >= 11, "only " + all.size() + " structures");
        for (Object plan : all) {
            Object bp = call(st, "blueprint", call(plan, "id"));
            h.assertTrue(bp != null && !((List<?>) call(bp, "entries")).isEmpty(), "structure " + call(plan, "id") + " gave no blueprint");
        }
        java.util.function.BiFunction<String, String, Long> count = (structure, path) -> {
            Object bp = call(st, "blueprint", ResourceLocation.fromNamespaceAndPath("mio_icif", structure));
            return ((List<?>) call(bp, "entries")).stream().filter(e -> ((BlockState) call(e, "state")).is(block(path))).count();
        };
        h.assertTrue(count.apply("fusion_reactor", "fusion/block_fusion_power_port") == 1, "power ports " + count.apply("fusion_reactor", "fusion/block_fusion_power_port"));
        h.assertTrue(count.apply("fusion_reactor", "fusion/block_fusion_cryo_port") == 1, "cryo ports " + count.apply("fusion_reactor", "fusion/block_fusion_cryo_port"));
        h.assertTrue(count.apply("fusion_reactor", "fusion/block_fusion_nbi_port") == 2, "NBI ports " + count.apply("fusion_reactor", "fusion/block_fusion_nbi_port"));
        h.assertTrue(count.apply("fusion_reactor", "fusion/block_fusion_controller") == 1, "controllers " + count.apply("fusion_reactor", "fusion/block_fusion_controller"));
        h.assertTrue(count.apply("tesu", "wiring/block_tesu_input") == 4 && count.apply("tesu", "wiring/block_tesu_output") == 4,
            "TESU ports " + count.apply("tesu", "wiring/block_tesu_input") + " in, " + count.apply("tesu", "wiring/block_tesu_output") + " out");
        h.assertTrue(count.apply("tesu", "wiring/block_tesu_cell") == 189, "TESU cells " + count.apply("tesu", "wiring/block_tesu_cell"));
        // a builder takes the steam tunnel: ready, its source is a structure, its plan the tunnel's blocks
        h.setBlock(AT, block("producer/block_blueprint_builder"));
        BlockEntity builder = h.getBlockEntity(AT);
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        try {
            h.assertTrue((boolean) call(builder, "selectStructure", p, ResourceLocation.fromNamespaceAndPath("mio_icif", "steam_tunnel")), "the builder refused the steam tunnel");
            h.assertTrue(!(boolean) call(builder, "selectStructure", p, ResourceLocation.fromNamespaceAndPath("mio_icif", "no_such_structure")), "the builder took an unknown structure");
        } finally {
            p.discard();
        }
        h.assertTrue((int) call(builder, "status") == 1, "the builder is not ready: status " + call(builder, "status"));
        h.assertTrue(call(builder, "structure").equals("mio_icif:steam_tunnel"), "the builder's structure is " + call(builder, "structure"));
        List<?> plan = (List<?>) call(builder, "plan");
        h.assertTrue(plan.size() == 42, "the steam tunnel plan has " + plan.size() + " blocks");   // 3 x 3 x 5 less the 3 blocks inside
        h.succeed();
    }

    /** A TESU wireless port that fills an upgraded machine is noted with it (the clients draw the stream from the port to it). */
    @GameTest(template = "reactor_loop", batch = "round111188_beam", timeoutTicks = 200)
    public static void wirelessPortNotesTheStreamToItsMachine(GameTestHelper h) {
        BlockPos controllerAt = Round111157GameTests.controller(h);
        List<BlockPos> ports = Round111157GameTests.build(h, controllerAt, Direction.NORTH, 0, 0, 1);
        BlockEntity controller = Round111157GameTests.be(h, controllerAt);
        BlockPos machineAt = Round111157GameTests.corner(h).offset(11, 1, 1);
        h.getLevel().setBlock(machineAt, block("producer/block_furnace_elc").defaultBlockState(), 3);
        BlockEntity furnace = Round111157GameTests.be(h, machineAt);
        IItemHandler items = (IItemHandler) call(furnace, "getItemHandler");
        ItemStack rest = items.insertItem((Integer) call(furnace, "getUpgradeSlotStart"),
            new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "upgrade/wireless_upgrade"))), false);
        h.assertTrue(rest.isEmpty(), "the wireless upgrade did not fit");
        Class<?> beams = Round33GameTests.common("blockentity.batbox.TesuWirelessBeams");
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue((Boolean) call(controller, "isStructureComplete"), "the TESU did not form"))
            .thenExecute(() -> call(call(controller, "getEnergyStorageInternal"), "setEnergy", 1_000_000_000L))
            .thenWaitUntil(() -> h.assertTrue((boolean) call(beams, "feeds", h.getLevel(), ports.get(0), machineAt),
                "the port is not noted as filling the furnace"))
            .thenSucceed();
    }
}
