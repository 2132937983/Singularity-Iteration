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
 * 0.1.11.224 (Wingman cave overhaul G5): the stall monitor. {@code WingmanStuck} takes its steps in order (search again,
 * back off, give up, the jump back to the owner) after 30 ticks each without a new best, the way back of a back-off does not
 * reset it, and a new best or arriving does. In the dead end a move order to a point behind the glass is given up (before,
 * the Wingman pressed against the glass for as long as the order lasted). The common classes are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111224GameTests {
    private Round111224GameTests() {}

    private static Object monitor() {
        try {
            return Round33GameTests.common("armory.WingmanStuck").getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("WingmanStuck", e);
        }
    }

    private static String track(Object m, double left) {
        return ((Enum<?>) Round33GameTests.call(m, "track", Vec3.ZERO, new Vec3(20, 0, 0), left)).name();
    }

    /** {@code n} ticks at {@code left}; the action of the last one. */
    private static String hold(Object m, double left, int n) {
        String last = "NONE";
        for (int i = 0; i < n; i++) {
            String a = track(m, left);
            if (!a.equals("NONE") && i < n - 1) throw new AssertionError("a step (" + a + ") after " + (i + 1) + " of " + n + " ticks");
            last = a;
        }
        return last;
    }

    @GameTest(template = "empty", batch = "round111224_steps", timeoutTicks = 20)
    public static void theStepsComeInOrder(GameTestHelper h) {
        Object m = monitor();
        track(m, 10);   // the first tick sets the best
        h.assertTrue(hold(m, 10, 30).equals("REPLAN"), "the first stall does not search again");
        h.assertTrue(hold(m, 10, 30).equals("BACK_OFF"), "the second stall does not back off");
        h.assertTrue(Round33GameTests.call(m, "backOff") != null, "no place to back off to");
        // the back-off runs 20 ticks; on the way back the Wingman closes in to 9.8 (not a new best: 10 - 0.5)
        h.assertTrue(hold(m, 9.8, 20 + 30).equals("GIVE_UP"), "the third stall does not give up");
        h.assertTrue(hold(m, 9.8, 30).equals("RECALL"), "the fourth stall does not jump back");
        h.assertTrue(hold(m, 9.8, 30).equals("RECALL"), "the jump back is not tried again");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111224_reset", timeoutTicks = 20)
    public static void aNewBestOrArrivingStartsAgain(GameTestHelper h) {
        Object m = monitor();
        track(m, 10);   // the first tick sets the best
        h.assertTrue(hold(m, 10, 30).equals("REPLAN"), "no first step");
        track(m, 9.4);   // a new best
        h.assertTrue((Integer) Round33GameTests.call(m, "level") == 0, "a new best does not start again");
        h.assertTrue(hold(m, 9.4, 30).equals("REPLAN"), "after a new best the first step is not the search again");
        track(m, 2.0);   // arrived
        h.assertTrue((Integer) Round33GameTests.call(m, "level") == 0, "arriving does not start again");
        h.assertTrue(hold(m, 2.0, 60).equals("NONE"), "a Wingman at its goal counts as stalled");
        h.succeed();
    }

    @GameTest(template = "cave", batch = "round111224_order", timeoutTicks = 320)
    public static void aMoveOrderBehindGlassIsGivenUp(GameTestHelper h) {
        Round111220GameTests.shell(h, true);
        Round111220GameTests.carve(h, 2, 2, 4, 20, 4, 6);
        Round111220GameTests.fill(h, 21, 2, 4, 21, 4, 6, Blocks.GLASS.defaultBlockState());
        Round111220GameTests.carve(h, 22, 2, 4, 26, 4, 6);
        Round111220GameTests.carve(h, 3, 2, 8, 3, 3, 8);   // the owner's closed cell: it does not see the Wingman
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        owner.moveTo(h.absoluteVec(new Vec3(3.5, 2, 8.5)));
        owner.getAbilities().invulnerable = true;
        Mob w = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(6.5, 2.2, 5.5));
        Round33GameTests.call(w, "setReserve", 1_000_000L);
        Round33GameTests.call(w, "order", Round1102GameTests.constant("armory.WingmanOrder", "MOVE"), h.absoluteVec(new Vec3(24.5, 2, 5.5)), null);
        h.runAtTickTime(300, () -> {
            String order = ((Enum<?>) Round33GameTests.call(w, "order")).name();
            w.discard();
            Round33GameTests.remove(owner);
            h.assertTrue(order.equals("NONE"), "the move order behind the glass is still on after 300 ticks (" + order + ")");
            h.succeed();
        });
    }
}
