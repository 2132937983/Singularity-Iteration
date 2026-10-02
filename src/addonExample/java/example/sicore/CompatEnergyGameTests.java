package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Cross-mod energy: SI must keep powering Modern Industrialization and GregTech Modern
 * machines, through its cables and straight out of its storage blocks, and accept their
 * energy back. The tests only exist when the foreign mod is loaded (compat GameTest runs);
 * everything is reached by capability name and reflection, there is no compile-time link.
 */
@GameTestHolder(CoreExampleMod.ID)
public final class CompatEnergyGameTests {
    private CompatEnergyGameTests() {}

    static final String MI = "modern_industrialization";
    static final String GT = "gtceu";

    @GameTestGenerator
    public static Collection<TestFunction> compatTests() {
        List<TestFunction> tests = new ArrayList<>();
        if (ModList.get().isLoaded(MI)) {
            tests.add(test("mi_cable_feed", "siCableFeedsMiMachine", 200, CompatEnergyGameTests::siCableFeedsMiMachine));
            tests.add(test("mi_direct_feed", "siStorageFeedsAdjacentMiStorage", 200, CompatEnergyGameTests::siStorageFeedsAdjacentMiStorage));
            tests.add(test("mi_reverse_feed", "miStorageFeedsSiStorage", 200, CompatEnergyGameTests::miStorageFeedsSiStorage));
            tests.add(test("mi_machine_feed", "siCableFeedsMiElectricFurnace", 200, CompatEnergyGameTests::siCableFeedsMiElectricFurnace));
        }
        if (ModList.get().isLoaded(GT)) {
            tests.add(test("gt_cable_feed", "siCableFeedsGtMachine", 200, CompatEnergyGameTests::siCableFeedsGtMachine));
            tests.add(test("gt_direct_feed", "siStorageFeedsAdjacentGtMachine", 200, CompatEnergyGameTests::siStorageFeedsAdjacentGtMachine));
            tests.add(test("gt_safe_voltage", "siNeverOvervoltsGtMachine", 200, CompatEnergyGameTests::siNeverOvervoltsGtMachine));
            tests.add(test("gt_reverse_feed", "gtNetworkFeedsSiStorage", 100, CompatEnergyGameTests::gtNetworkFeedsSiStorage));
        }
        return tests;
    }

    private static TestFunction test(String batch, String name, int ticks, Consumer<GameTestHelper> body) {
        return new TestFunction(batch, "compatenergygametests." + name.toLowerCase(java.util.Locale.ROOT),
            CoreExampleMod.ID + ":empty", ticks, 0, true, body);
    }

    // ------------------------------------------------------------------ helpers
    static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
    }

    static BlockState facing(String id, Direction side) {
        BlockState s = block(id).defaultBlockState();
        var p = s.getBlock().getStateDefinition().getProperty("facing");
        return p instanceof DirectionProperty d && d.getPossibleValues().contains(side) ? s.setValue(d, side) : s;
    }

    @SuppressWarnings("unchecked")
    static BlockCapability<Object, Direction> capability(String name) {
        for (var cap : BlockCapability.getAll()) if (cap.name().toString().equals(name)) return (BlockCapability<Object, Direction>) cap;
        throw new IllegalStateException("capability " + name + " is not registered");
    }

    static Object call(Object target, String method, Object... args) {
        try {
            for (Method m : target.getClass().getMethods()) {
                if (m.getName().equals(method) && m.getParameterCount() == args.length) {
                    m.setAccessible(true);
                    return m.invoke(target, args);
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(method + " failed on " + target.getClass().getName(), e);
        }
        throw new IllegalStateException(target.getClass().getName() + " has no " + method);
    }

    static long siStored(GameTestHelper h, BlockPos rel) {
        var fe = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(rel), null);
        if (fe == null) for (Direction d : Direction.values()) {
            fe = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(rel), d);
            if (fe != null) break;
        }
        h.assertTrue(fe != null, "SI block at " + rel + " exposes no FE view");
        return fe.getEnergyStored() / 4L;
    }

    static void fillSi(GameTestHelper h, BlockPos rel, long eu) {
        com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity storage = h.getBlockEntity(rel);
        storage.getEnergyStorageInternal().setStored(Math.min(eu, storage.getEnergyStorageInternal().getCapacity()));
    }

    // ------------------------------------------------------------------ Modern Industrialization
    static final String MI_SIDED = "modern_industrialization:sided_mi_energy_storage";

    static long miAmount(GameTestHelper h, BlockPos rel) {
        var cap = capability(MI_SIDED);
        for (Direction d : Direction.values()) {
            Object storage = h.getLevel().getCapability(cap, h.absolutePos(rel), d);
            if (storage != null) return (long) call(storage, "getAmount");
        }
        throw new IllegalStateException("MI block at " + rel + " exposes no energy storage");
    }

    static long miFill(GameTestHelper h, BlockPos rel, long eu) {
        var cap = capability(MI_SIDED);
        long moved = 0;
        for (Direction d : Direction.values()) {
            Object storage = h.getLevel().getCapability(cap, h.absolutePos(rel), d);
            if (storage != null && (boolean) call(storage, "canReceive"))
                moved += (long) call(storage, "receive", eu - moved, false);
            if (moved >= eu) break;
        }
        return moved;
    }

    /** BatBox (output facing east) -> SI tin cable -> MI LV storage unit. */
    static void siCableFeedsMiMachine(GameTestHelper h) {
        BlockPos box = new BlockPos(1, 2, 2), cable = box.east(), cable2 = cable.east(), mi = cable2.east();
        h.setBlock(box, facing("mio_icif:wiring/block_bat_box", Direction.EAST));
        h.setBlock(cable, block("mio_icif:wiring/cable/block_tin_cable"));
        h.setBlock(cable2, block("mio_icif:wiring/cable/block_tin_cable"));
        h.setBlock(mi, block(MI + ":lv_storage_unit"));
        expectMiGain(h, box, mi, "SI cable -> MI storage unit");
    }

    /** BatBox -> SI cable -> MI electric furnace (a machine, not a storage block). */
    static void siCableFeedsMiElectricFurnace(GameTestHelper h) {
        BlockPos box = new BlockPos(1, 2, 2), cable = box.east(), mi = cable.east();
        h.setBlock(box, facing("mio_icif:wiring/block_bat_box", Direction.EAST));
        h.setBlock(cable, block("mio_icif:wiring/cable/block_tin_cable"));
        h.setBlock(mi, block(MI + ":electric_furnace"));
        int[] tick = {0};
        long[] start = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now == 5) { fillSi(h, box, 40_000); start[0] = miAmount(h, mi); }
            if (now == 120) {
                h.assertTrue(miAmount(h, mi) > start[0], "MI electric furnace received nothing through the SI cable");
                h.succeed();
            }
        });
    }

    /** BatBox output face directly against an MI LV storage unit. */
    static void siStorageFeedsAdjacentMiStorage(GameTestHelper h) {
        BlockPos box = new BlockPos(1, 2, 2), mi = box.east();
        h.setBlock(box, facing("mio_icif:wiring/block_bat_box", Direction.EAST));
        h.setBlock(mi, block(MI + ":lv_storage_unit"));
        expectMiGain(h, box, mi, "SI BatBox -> adjacent MI storage unit");
    }

    private static void expectMiGain(GameTestHelper h, BlockPos box, BlockPos mi, String what) {
        int[] tick = {0};
        long[] start = {0, 0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now == 5) {
                fillSi(h, box, 40_000);
                start[0] = miAmount(h, mi);
                start[1] = siStored(h, box);
                h.assertTrue(start[1] > 10_000, what + ": could not charge the BatBox (" + start[1] + " EU)");
            }
            if (now == 150) {
                long got = miAmount(h, mi) - start[0];
                long spent = start[1] - siStored(h, box);
                h.assertTrue(got > 0, what + ": MI received nothing (SI spent " + spent + " EU)");
                h.assertTrue(spent >= got, what + ": energy created from nothing (" + spent + " < " + got + ")");
                h.assertTrue(got >= 32L * 60, what + ": too slow, " + got + " EU in ~145 ticks");
                h.succeed();
            }
        });
    }

    /** A charged MI storage unit pushes into an SI BatBox placed against each of its faces. */
    static void miStorageFeedsSiStorage(GameTestHelper h) {
        BlockPos mi = new BlockPos(2, 2, 2);
        h.setBlock(mi, block(MI + ":lv_storage_unit"));
        BlockPos[] boxes = new BlockPos[4];
        Direction[] around = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (int i = 0; i < 4; i++) {
            boxes[i] = mi.relative(around[i]);
            h.setBlock(boxes[i], facing("mio_icif:wiring/block_bat_box", around[i]));
        }
        BlockPos box = boxes[0];
        int[] tick = {0};
        long[] start = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now == 5) {
                long filled = miFill(h, mi, 50_000);
                h.assertTrue(filled > 0, "could not charge the MI storage unit");
                start[0] = 0;
                for (BlockPos b : boxes) start[0] += siStored(h, b);
            }
            if (now == 150) {
                long sum = 0;
                for (BlockPos b : boxes) sum += siStored(h, b);
                long got = sum - start[0];
                h.assertTrue(got > 0, "MI -> SI: the BatBox received nothing");
                h.succeed();
            }
        });
    }

    // ------------------------------------------------------------------ GregTech Modern
    static final String GT_CONTAINER = "gtceu:energy_container";

    static Object gtContainer(GameTestHelper h, BlockPos rel) {
        var cap = capability(GT_CONTAINER);
        for (Direction d : Direction.values()) {
            Object c = h.getLevel().getCapability(cap, h.absolutePos(rel), d);
            if (c != null) return c;
        }
        throw new IllegalStateException("GT block at " + rel + " exposes no energy container");
    }

    static long gtStored(GameTestHelper h, BlockPos rel) {
        return (long) call(gtContainer(h, rel), "getEnergyStored");
    }

    static void expectGtGain(GameTestHelper h, BlockPos box, BlockPos gt, String what) {
        int[] tick = {0};
        long[] start = {0, 0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now == 5) {
                fillSi(h, box, 40_000);
                start[0] = gtStored(h, gt);
                start[1] = siStored(h, box);
            }
            if (now == 150) {
                h.assertTrue(h.getBlockState(gt).getBlock() != Blocks.AIR, what + ": the GT machine exploded");
                long got = gtStored(h, gt) - start[0];
                long spent = start[1] - siStored(h, box);
                h.assertTrue(got > 0, what + ": GT received nothing (SI spent " + spent + " EU)");
                h.assertTrue(spent >= got, what + ": energy created from nothing (" + spent + " < " + got + ")");
                h.succeed();
            }
        });
    }

    /** BatBox -> SI cable -> GT LV battery buffer. */
    static void siCableFeedsGtMachine(GameTestHelper h) {
        BlockPos box = new BlockPos(1, 2, 2), cable = box.east(), gt = cable.east();
        h.setBlock(box, facing("mio_icif:wiring/block_bat_box", Direction.EAST));
        h.setBlock(cable, block("mio_icif:wiring/cable/block_tin_cable"));
        h.setBlock(gt, block(GT + ":lv_electric_furnace"));
        expectGtGain(h, box, gt, "SI cable -> GT furnace");
    }

    static void siStorageFeedsAdjacentGtMachine(GameTestHelper h) {
        BlockPos box = new BlockPos(1, 2, 2), gt = box.east();
        h.setBlock(box, facing("mio_icif:wiring/block_bat_box", Direction.EAST));
        h.setBlock(gt, block(GT + ":lv_electric_furnace"));
        expectGtGain(h, box, gt, "SI BatBox -> adjacent GT furnace");
    }

    /** An MFE (512 EU packets) must not blow up an LV (32 V) GT machine. */
    static void siNeverOvervoltsGtMachine(GameTestHelper h) {
        BlockPos box = new BlockPos(1, 2, 2), gt = box.east();
        h.setBlock(box, facing("mio_icif:wiring/block_mfe", Direction.EAST));
        h.setBlock(gt, block(GT + ":lv_electric_furnace"));
        expectGtGain(h, box, gt, "SI MFE -> LV GT furnace");
    }

    /**
     * GT -> SI: GregTech's own FE adapter presents an SI BatBox as a GT energy container, so GT
     * cables and battery buffers feed SI without extra glue. This guards that path.
     */
    static void gtNetworkFeedsSiStorage(GameTestHelper h) {
        BlockPos box = new BlockPos(1, 2, 2);
        h.setBlock(box, facing("mio_icif:wiring/block_bat_box", Direction.EAST));
        h.runAfterDelay(5, () -> {
            Object container = h.getLevel().getCapability(capability(GT_CONTAINER), h.absolutePos(box), Direction.WEST);
            h.assertTrue(container != null, "SI BatBox exposes no GregTech energy container on its input face");
            h.assertTrue((boolean) call(container, "inputsEnergy", Direction.WEST), "BatBox input face refuses GT energy");
            long before = siStored(h, box);
            long amps = (long) call(container, "acceptEnergyFromNetwork", Direction.WEST, 32L, 1L);
            long after = siStored(h, box);
            h.assertTrue(amps == 1 && after - before == 32, "GT packet 32 V x 1 A booked " + amps + " A, stored +" + (after - before));
            Object output = h.getLevel().getCapability(capability(GT_CONTAINER), h.absolutePos(box), Direction.EAST);
            h.assertTrue(output == null || !(boolean) call(output, "inputsEnergy", Direction.EAST), "BatBox output face accepts GT energy");
            h.succeed();
        });
    }
}
