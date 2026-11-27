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
 * 0.1.11.221 (Wingman cave overhaul G1): the Wingman measures its place with its whole body and steers by context.
 * {@code WingmanSpace} sorts a 3-high tunnel, an 11-high cavern and the open ground; {@code WingmanTactics.avoid} does not
 * send a Wingman in a tunnel up into the rock (before, a blocked way ahead made it climb), still lifts it over a wall in the
 * open, and brakes without drifting up when nothing is free. The scenes of {@link Round111220GameTests} check the flight.
 * The Wingman has no AI here (it stays where it is put); the common classes are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111221GameTests {
    private Round111221GameTests() {}

    private static Class<?> tactics() { return Round33GameTests.common("armory.WingmanTactics"); }

    private static Class<?> space() { return Round33GameTests.common("armory.WingmanSpace"); }

    /** A Wingman without AI at {@code at} (feet) and its owner. */
    private static Mob still(GameTestHelper h, Vec3 at, ServerPlayer[] owner) {
        owner[0] = Round33GameTests.survivalPlayer(h);
        owner[0].moveTo(h.absoluteVec(new Vec3(3.5, 2, 5.5)));
        Mob w = (Mob) Round1102GameTests.wingman(h, owner[0], at);
        w.setNoAi(true);
        return w;
    }

    private static Vec3 avoid(Mob w, Vec3 want) { return (Vec3) Round33GameTests.call(tactics(), "avoid", w, want); }

    private static String kind(Mob w) {
        Object room = Round33GameTests.call(space(), "measure", w.level(), w);
        return ((Enum<?>) Round33GameTests.call(room, "kind")).name();
    }

    private static void done(GameTestHelper h, Mob w, ServerPlayer owner) {
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    @GameTest(template = "cave", batch = "round111221_kinds", timeoutTicks = 40)
    public static void placesAreSortedByTheirCeiling(GameTestHelper h) {
        Round111220GameTests.shell(h, true);
        Round111220GameTests.carve(h, 2, 2, 4, 14, 4, 6);     // 3 high
        Round111220GameTests.carve(h, 20, 2, 2, 41, 12, 8);   // 11 high
        ServerPlayer[] owner = new ServerPlayer[1];
        Mob w = still(h, new Vec3(6.5, 2, 5.5), owner);
        h.assertTrue(kind(w).equals("TUNNEL"), "a 3-high tunnel is " + kind(w));
        w.moveTo(h.absoluteVec(new Vec3(30.5, 4, 5.5)));
        h.assertTrue(kind(w).equals("INDOOR"), "an 11-high cavern is " + kind(w));
        Round111220GameTests.carve(h, 20, 13, 2, 41, 16, 8);  // the roof gone: up to the barrier, 15 high
        h.assertTrue(kind(w).equals("OPEN"), "a place 15 blocks high is " + kind(w));
        done(h, w, owner[0]);
    }

    @GameTest(template = "cave", batch = "round111221_tunnel", timeoutTicks = 40)
    public static void aTunnelDoesNotSendItUp(GameTestHelper h) {
        Round111220GameTests.shell(h, true);
        Round111220GameTests.carve(h, 2, 2, 4, 30, 4, 6);
        ServerPlayer[] owner = new ServerPlayer[1];
        Mob w = still(h, new Vec3(6.5, 2, 5.5), owner);
        // the target is up and ahead through the rock (the cave scenes' upper tunnel): it used to climb into the roof
        Vec3 v = avoid(w, new Vec3(0.8, 0.8, 0));
        h.assertTrue(v.y < 0.05, "the avoidance sends it up in a tunnel: " + v);
        h.assertTrue(v.x > 0.2, "the avoidance does not follow the tunnel: " + v);
        done(h, w, owner[0]);
    }

    @GameTest(template = "cave", batch = "round111221_wall", timeoutTicks = 40)
    public static void aWallInTheOpenIsClimbed(GameTestHelper h) {
        Round111220GameTests.shell(h, false);
        Round111220GameTests.fill(h, 9, 2, 1, 9, 4, 9, Blocks.STONE.defaultBlockState());
        ServerPlayer[] owner = new ServerPlayer[1];
        Mob w = still(h, new Vec3(6.5, 2, 5.5), owner);
        Vec3 v = avoid(w, new Vec3(0.9, 0, 0));
        h.assertTrue(v.y > 0.2, "a 3-high wall in the open is not climbed: " + v);
        h.assertTrue(v.x > 0, "it turns back from a wall in the open: " + v);
        done(h, w, owner[0]);
    }

    @GameTest(template = "cave", batch = "round111221_boxed", timeoutTicks = 40)
    public static void noWayBrakesWithoutDrift(GameTestHelper h) {
        Round111220GameTests.shell(h, true);
        Round111220GameTests.carve(h, 6, 2, 5, 6, 3, 5);      // a 1 x 2 x 1 pocket: the body only
        ServerPlayer[] owner = new ServerPlayer[1];
        Mob w = still(h, new Vec3(6.5, 2, 5.5), owner);
        Vec3 v = avoid(w, new Vec3(0.9, 0.3, 0));
        h.assertTrue(v.y <= 0, "boxed in, it still drifts up: " + v);
        h.assertTrue(v.length() < 0.5, "boxed in, it does not brake: " + v);
        done(h, w, owner[0]);
    }
}
