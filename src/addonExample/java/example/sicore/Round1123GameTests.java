package example.sicore;

import io.netty.buffer.Unpooled;
import java.lang.reflect.Proxy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.23 (behaviour prediction P2 / P4 / P6 / P7): the movement model of the predictor on real blocks
 * (a slab is stepped up, a one-block ledge is jumped onto, a two-block wall ends the path, a knocked creature slides
 * farther on ice than on stone), the path node packet of the data link (round trip, a list cut at a step that does not
 * fit in a byte), the guided shot assignment (a shot goes to the locked zombie it can finish), the steering law choice
 * and the incoming projectile warning. The fcs and suit classes are internal: reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1123GameTests {
    private Round1123GameTests() {}

    private static final String COMMON = "com.miophas.singularity_iteration" + ".common.";

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> common(String name) {
        try {
            return Class.forName(COMMON + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    // ------------------------------------------------------------------ P2 movement model on real blocks

    /** The level as the predictor sees it (the same answers as the client's LevelBlocks), as a proxy of fcs.DeadReckoning.Blocks. */
    private static Object blocks(GameTestHelper h) {
        Class<?> type = common("fcs.DeadReckoning$Blocks");
        var level = h.getLevel();
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            BlockPos pos = new BlockPos((int) args[0], (int) args[1], (int) args[2]);
            BlockState state = level.getBlockState(pos);
            return switch (method.getName()) {
                case "solid" -> !state.getCollisionShape(level, pos).isEmpty();
                case "air" -> state.isAir();
                case "top" -> {
                    var shape = state.getCollisionShape(level, pos);
                    yield shape.isEmpty() ? 0.0 : Math.max(0.0, shape.max(Direction.Axis.Y));
                }
                case "water" -> level.getFluidState(pos).is(FluidTags.WATER);
                case "friction" -> (double) state.getFriction(level, pos, null);
                default -> throw new AssertionError(method.getName());
            };
        });
    }

    /** A walk of 20 ticks from the relative position {@code start} with velocity and drive (vx, 0) along +x. */
    private static double[] walk(GameTestHelper h, BlockPos start, double vx, double drive, boolean packet) throws ReflectiveOperationException {
        Class<?> bodyType = common("fcs.DeadReckoning$Body");
        Object body = bodyType.getDeclaredConstructor().newInstance();
        bodyType.getField("vx").setDouble(body, vx);
        bodyType.getField("driveX").setDouble(body, drive);
        bodyType.getField("packet").setBoolean(body, packet);
        bodyType.getField("onGround").setBoolean(body, true);
        bodyType.getField("height").setDouble(body, 1.95);
        BlockPos a = h.absolutePos(start);
        return (double[]) call(common("fcs.DeadReckoning"), "walk", blocks(h), a.getX() + 0.5, (double) a.getY(), a.getZ() + 0.5, body, 20);
    }

    private static void lane(GameTestHelper h, int z, BlockState floor) {
        for (int x = 0; x < 7; x++) {
            h.setBlock(new BlockPos(x, 1, z), floor);
            for (int y = 2; y < 6; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
    }

    private static double maxY(double[] path) {
        double m = Double.NEGATIVE_INFINITY;
        for (int i = 1; i < path.length; i += 3) m = Math.max(m, path[i]);
        return m;
    }

    private static double endX(double[] path) { return path.length < 3 ? Double.NaN : path[path.length - 3]; }

    /** A slab is stepped up, a one-block ledge is jumped onto, a two-block wall ends the path (before 0.1.10.23 all three ended it). */
    @GameTest(template = "reactor", batch = "round1123_prediction", timeoutTicks = 20)
    public static void pathStepsJumpsAndStops(GameTestHelper h) throws ReflectiveOperationException {
        for (int z : new int[] {1, 3, 5}) lane(h, z, Blocks.STONE.defaultBlockState());
        h.setBlock(new BlockPos(4, 2, 1), Blocks.STONE_SLAB.defaultBlockState());
        h.setBlock(new BlockPos(4, 2, 3), Blocks.STONE.defaultBlockState());
        h.setBlock(new BlockPos(4, 2, 5), Blocks.STONE.defaultBlockState());
        h.setBlock(new BlockPos(4, 3, 5), Blocks.STONE.defaultBlockState());
        double floor = h.absolutePos(new BlockPos(0, 2, 0)).getY();
        double wallX = h.absolutePos(new BlockPos(4, 2, 0)).getX();
        double[] slab = walk(h, new BlockPos(1, 2, 1), 0.25, 0.25, false);
        double[] ledge = walk(h, new BlockPos(1, 2, 3), 0.25, 0.25, false);
        double[] wall = walk(h, new BlockPos(1, 2, 5), 0.25, 0.25, false);
        h.assertTrue(Math.abs(maxY(slab) - (floor + 0.5)) < 0.01 && endX(slab) > wallX + 0.5, "slab not stepped up: top " + maxY(slab) + ", end " + endX(slab));
        h.assertTrue(Math.abs(maxY(ledge) - (floor + 1)) < 0.3 && endX(ledge) > wallX + 0.5, "ledge not jumped onto: top " + maxY(ledge) + ", end " + endX(ledge));
        h.assertTrue(endX(wall) < wallX, "the path went through a two-block wall: end " + endX(wall));
        h.succeed();
    }

    /** A creature pushed at 0.5 blocks per tick with no drive of its own slides about 4 blocks on packed ice and about 1 on stone. */
    @GameTest(template = "reactor", batch = "round1123_prediction", timeoutTicks = 20)
    public static void knockedCreatureSlidesOnIce(GameTestHelper h) throws ReflectiveOperationException {
        lane(h, 1, Blocks.STONE.defaultBlockState());
        lane(h, 3, Blocks.PACKED_ICE.defaultBlockState());
        double[] stone = walk(h, new BlockPos(0, 2, 1), 0.5, 0, true);
        double[] ice = walk(h, new BlockPos(0, 2, 3), 0.5, 0, true);
        double start = h.absolutePos(new BlockPos(0, 2, 0)).getX() + 0.5;
        double onStone = endX(stone) - start, onIce = endX(ice) - start;
        h.assertTrue(onStone > 0.8 && onStone < 1.5, "slide on stone " + onStone);
        h.assertTrue(onIce > 3 && onIce < 5, "slide on ice " + onIce);
        h.succeed();
    }

    // ------------------------------------------------------------------ P2 data link packet

    /** The path node list survives the codec; a step longer than a byte cuts that list there. */
    @GameTest(template = "empty", batch = "round1123_prediction", timeoutTicks = 20)
    public static void pathNodePacketRoundTrip(GameTestHelper h) throws ReflectiveOperationException {
        Class<?> type = common("suit.SuitPackets$PathNodes");
        int[] ids = {41, 42};
        int[][] nodes = {{10, 64, -5, 11, 64, -5, 12, 65, -5}, {0, 70, 0, 1, 70, 0, 2, 70, 0, 300, 70, 0}};
        Object packet = type.getDeclaredConstructor(int[].class, int[][].class).newInstance(ids, nodes);
        @SuppressWarnings("unchecked")
        var codec = (net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, Object>) type.getField("CODEC").get(null);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        codec.encode(buf, packet);
        Object back = codec.decode(buf);
        int[] ids2 = (int[]) call(back, "ids");
        int[][] nodes2 = (int[][]) call(back, "nodes");
        h.assertTrue(java.util.Arrays.equals(ids, ids2), "ids " + java.util.Arrays.toString(ids2));
        h.assertTrue(java.util.Arrays.equals(nodes[0], nodes2[0]), "first list " + java.util.Arrays.toString(nodes2[0]));
        h.assertTrue(nodes2[1].length == 9, "the list with a 298-block step was not cut: " + java.util.Arrays.toString(nodes2[1]));
        h.assertTrue(buf.readableBytes() == 0, "bytes left over: " + buf.readableBytes());
        h.succeed();
    }

    // ------------------------------------------------------------------ P6 shot assignment, P4 law, P7 warning

    /** With two locked zombies, a 6-damage shot goes to the one with 2 health left (before: the locks in turn). */
    @GameTest(template = "reactor", batch = "round1123_guidance", timeoutTicks = 20)
    public static void shotGoesToTheTargetItFinishes(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 3.5)));
        Zombie weak = h.spawn(EntityType.ZOMBIE, new BlockPos(5, 2, 1));
        Zombie strong = h.spawn(EntityType.ZOMBIE, new BlockPos(5, 2, 5));
        weak.setNoAi(true);
        strong.setNoAi(true);
        weak.setHealth(2);
        Object chosen = call(common("suit.SmartGuidance"), "assign", player, new int[] {strong.getId(), weak.getId()}, 6.0);
        h.assertTrue(chosen == weak, "the shot went to " + chosen);
        h.succeed();
    }

    /** The steering law: PN with the gravity term for a full bow shot, APN with Predictive Guidance, pursuit otherwise. */
    @GameTest(template = "empty", batch = "round1123_guidance", timeoutTicks = 20)
    public static void steeringLawChoice(GameTestHelper h) {
        Class<?> g = common("fcs.Guidance");
        h.assertTrue("PN".equals(call(g, "choose", 3.0, 0.05, false, false).toString()), "full bow shot");
        h.assertTrue("APN".equals(call(g, "choose", 3.0, 0.05, true, true).toString()), "Predictive Guidance");
        h.assertTrue("PURSUIT".equals(call(g, "choose", 3.0, 0.05, false, true).toString()), "flying target without Predictive Guidance");
        h.assertTrue("PURSUIT".equals(call(g, "choose", 1.5, 0.05, true, false).toString()), "short draw");
        h.assertTrue("PURSUIT".equals(call(g, "choose", 2.0, 0.0, true, false).toString()), "no gravity");
        h.succeed();
    }

    /** An arrow aimed at the wearer is a hit; one aimed 3 blocks to the side passes, and the evade step is across its path. */
    @GameTest(template = "empty", batch = "round1123_guidance", timeoutTicks = 20)
    public static void incomingArrowWarning(GameTestHelper h) throws ReflectiveOperationException {
        Class<?> w = common("fcs.ProjectileWarning");
        Object arrow = w.getField("ARROW").get(null);
        double[] feet = {0, 64, 0}, still = {0, 0, 0};
        Object hit = call(w, "closest", new double[] {0, 65.5, -15}, new double[] {0, 0.25, 1.6}, arrow, feet, still, 0.3, 1.8);
        Object pass = call(w, "closest", new double[] {3, 65.5, -15}, new double[] {0, 0.25, 1.6}, arrow, feet, still, 0.3, 1.8);
        h.assertTrue(hit != null && (boolean) call(hit, "hit"), "the arrow at the wearer is not a hit: " + hit);
        h.assertTrue(pass == null || !(boolean) call(pass, "hit"), "the arrow 3 blocks aside is a hit: " + pass);
        double ex = (double) call(hit, "evadeX"), ez = (double) call(hit, "evadeZ");
        h.assertTrue(Math.abs(ez) < 1.0E-6 && Math.abs(Math.abs(ex) - 1) < 1.0E-6, "evade step not across the path: " + ex + ", " + ez);
        h.succeed();
    }
}
