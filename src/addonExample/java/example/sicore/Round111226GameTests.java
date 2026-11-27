package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.226 (Wingman cave overhaul G3, decisions two B and three A): the ground form. Sent onto a ledge 3 blocks up in a
 * low hall, a Wingman on foot gets there with a thruster hop; one block up it just steps (no hop); when its owner flies off
 * fast it takes to the air again; and on foot it plays the gaits of batch AN1 at the speed it moves (walk, run, thruster
 * skate), the take-off of its hop, the air poses and a heavy landing after a fall. The scenes of {@link Round111220GameTests}
 * check that it goes on foot in the tunnels and never in the open. The common classes are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111226GameTests {
    private Round111226GameTests() {}

    private static ServerPlayer owner(GameTestHelper h, Vec3 at) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        p.moveTo(h.absoluteVec(at));
        p.getAbilities().invulnerable = true;
        return p;
    }

    private static Mob wingman(GameTestHelper h, ServerPlayer owner, Vec3 at) {
        Mob w = (Mob) Round1102GameTests.wingman(h, owner, at);
        Round33GameTests.call(w, "setReserve", 1_000_000L);
        return w;
    }

    private static boolean ground(Mob w) { return (Boolean) Round33GameTests.call(w, "ground"); }

    private static int hops(Mob w) { return (Integer) Round33GameTests.call(w, "hops"); }

    private static void move(GameTestHelper h, Mob w, Vec3 to) {
        Round33GameTests.call(w, "order", Round1102GameTests.constant("armory.WingmanOrder", "MOVE"), h.absoluteVec(to), null);
    }

    private static void done(GameTestHelper h, Mob w, ServerPlayer owner) {
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** A 5-high hall (on foot) with a ledge 3 blocks high at its far end; the owner and the Wingman at the near end. */
    private static void hallWithLedge(GameTestHelper h, int ledge) {
        Round111220GameTests.shell(h, true);
        Round111220GameTests.carve(h, 2, 2, 3, 30, 6, 7);
        if (ledge > 0) Round111220GameTests.fill(h, 20, 2, 3, 30, 1 + ledge, 7, Blocks.STONE.defaultBlockState());
    }

    @GameTest(template = "cave", batch = "round111226_hop", timeoutTicks = 260)
    public static void aLedgeIsReachedWithAThrusterHop(GameTestHelper h) {
        hallWithLedge(h, 3);
        ServerPlayer owner = owner(h, new Vec3(3.5, 2, 5.5));
        Mob w = wingman(h, owner, new Vec3(6.5, 2, 5.5));
        h.runAtTickTime(30, () -> {
            h.assertTrue(ground(w), "a Wingman in a 5-high hall is not on foot");
            move(h, w, new Vec3(26.5, 5, 5.5));
        });
        h.runAtTickTime(240, () -> {
            int n = hops(w);
            Vec3 at = w.position().subtract(Vec3.atLowerCornerOf(h.absolutePos(net.minecraft.core.BlockPos.ZERO)));
            done(h, w, owner);
            h.assertTrue((n >> 1) > 0 && (n & 1) == 1, "no thruster hop up the 3-block ledge (hops " + n + ")");
            h.assertTrue(at.y >= 4.9 && at.x > 19.5, "the Wingman is not on the ledge but at " + at);
        });
    }

    @GameTest(template = "cave", batch = "round111226_step", timeoutTicks = 260)
    public static void oneBlockIsStepped(GameTestHelper h) {
        hallWithLedge(h, 1);
        ServerPlayer owner = owner(h, new Vec3(3.5, 2, 5.5));
        Mob w = wingman(h, owner, new Vec3(6.5, 2, 5.5));
        h.runAtTickTime(30, () -> move(h, w, new Vec3(26.5, 3, 5.5)));
        h.runAtTickTime(240, () -> {
            int n = hops(w);
            Vec3 at = w.position().subtract(Vec3.atLowerCornerOf(h.absolutePos(net.minecraft.core.BlockPos.ZERO)));
            done(h, w, owner);
            h.assertTrue(at.y >= 2.9 && at.x > 19.5, "the Wingman is not on the step but at " + at);
            h.assertTrue((n >> 1) == 0, "a one-block step takes a hop (hops " + n + ")");
        });
    }

    @GameTest(template = "cave", batch = "round111226_takeoff", timeoutTicks = 120)
    public static void aFastOwnerTakesItIntoTheAir(GameTestHelper h) {
        hallWithLedge(h, 0);
        ServerPlayer owner = owner(h, new Vec3(3.5, 2, 5.5));
        Mob w = wingman(h, owner, new Vec3(6.5, 2, 5.5));
        h.runAtTickTime(40, () -> h.assertTrue(ground(w), "a Wingman in a 5-high hall is not on foot"));
        // the owner dashes down the hall at a block per tick
        for (int t = 41; t <= 60; t++) {
            final int k = t - 40;
            h.runAtTickTime(t, () -> owner.teleportTo(owner.getX() + 1.0, owner.getY(), owner.getZ()));
            if (k == 20) h.runAtTickTime(t + 1, () -> {
                boolean on = ground(w);
                done(h, w, owner);
                h.assertTrue(!on, "the Wingman stays on foot while its owner rushes on at a block per tick");
            });
        }
    }

    private static Object pick(boolean onFloor, float speed, double vy, float sinceHop, boolean thrust, float sinceTouch, boolean heavy) {
        return Round33GameTests.call(Round33GameTests.common("armory.WingmanKeyframeChoice"), "onFoot", onFloor, speed, vy, sinceHop, thrust,
            sinceTouch, heavy, 7, 500);
    }

    private static String name(Object pick) { return (String) Round33GameTests.call(pick, "name"); }

    private static float speed(Object pick) { return (Float) Round33GameTests.call(pick, "speed"); }

    @GameTest(template = "empty", batch = "round111226_picks", timeoutTicks = 20)
    public static void onFootTheGaitsFollowTheSpeed(GameTestHelper h) {
        h.assertTrue(name(pick(true, 0.215F, 0, 99, false, 99, false)).equals("walk"), "no walk at 4.3 m/s");
        h.assertTrue(Math.abs(speed(pick(true, 0.215F, 0, 99, false, 99, false)) - 1) < 1.0E-3, "the walk at its reference speed is not played at 1x");
        h.assertTrue(speed(pick(true, 0.1F, 0, 99, false, 99, false)) < 0.6F, "a slow walk is not played slower");
        h.assertTrue(name(pick(true, 0.35F, 0, 99, false, 99, false)).equals("run"), "no run at 7 m/s");
        h.assertTrue(name(pick(true, 0.6F, 0, 99, false, 99, false)).equals("skate"), "no thruster skate at 12 m/s");
        h.assertTrue(name(pick(true, 0, 0, 99, false, 99, false)).startsWith("idle_"), "no idle standing still");
        h.assertTrue(name(pick(false, 0, 0.4, 2, true, 99, false)).equals("thrust_hop"), "no thruster hop take-off");
        h.assertTrue(name(pick(false, 0, 0.4, 2, false, 99, false)).equals("jump_takeoff"), "no jump take-off");
        h.assertTrue(name(pick(false, 0, 0.3, 20, true, 99, false)).equals("air_rise"), "no rise in the air");
        h.assertTrue(name(pick(false, 0, -0.3, 20, true, 99, false)).equals("air_fall"), "no fall in the air");
        h.assertTrue(name(pick(true, 0, 0, 99, false, 3, true)).equals("land_heavy"), "no heavy landing after a fall");
        h.assertTrue(name(pick(true, 0, 0, 99, false, 3, false)).equals("land_light"), "no light landing after a step down");
        h.succeed();
    }
}
