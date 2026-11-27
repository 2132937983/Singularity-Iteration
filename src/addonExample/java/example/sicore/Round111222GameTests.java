package example.sicore;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.222 (Wingman cave overhaul G2): grid path finding. {@code WingmanPathing.findNow} finds the way up the shaft of the
 * cave scene (about 45 cells), keeps to the floor under a ceiling when the height costs, fails behind a glass wall, and never
 * leads over lava. The Wingman has no AI here; the common classes are reached by reflection. The flight along the paths is
 * checked by the scenes of {@link Round111220GameTests} (the tunnel: the first hit within the run, the head off the ceiling).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111222GameTests {
    private Round111222GameTests() {}

    private static Class<?> pathing() { return Round33GameTests.common("armory.WingmanPathing"); }

    private static Mob still(GameTestHelper h, Vec3 at, ServerPlayer[] owner) {
        owner[0] = Round33GameTests.survivalPlayer(h);
        owner[0].moveTo(h.absoluteVec(new Vec3(3.5, 2, 5.5)));
        Mob w = (Mob) Round1102GameTests.wingman(h, owner[0], at);
        w.setNoAi(true);
        return w;
    }

    private static Object find(GameTestHelper h, Mob w, Vec3 goal, double heightCost) {
        Object s = Round33GameTests.call(pathing(), "findNow", w.level(), w, h.absoluteVec(goal), heightCost);
        if (s == null) throw new AssertionError("the search could not start");
        return s;
    }

    private static String status(Object search) { return ((Enum<?>) Round33GameTests.call(search, "status")).name(); }

    @SuppressWarnings("unchecked")
    private static List<Vec3> waypoints(Object search) { return (List<Vec3>) Round33GameTests.call(pathing(), "waypoints", search); }

    private static Vec3 rel(GameTestHelper h, Vec3 abs) { return abs.subtract(Vec3.atLowerCornerOf(h.absolutePos(BlockPos.ZERO))); }

    private static void done(GameTestHelper h, Mob w, ServerPlayer owner) {
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    @GameTest(template = "cave", batch = "round111222_shaft", timeoutTicks = 40)
    public static void theWayUpTheShaft(GameTestHelper h) {
        Round111220GameTests.shell(h, true);
        Round111220GameTests.carve(h, 2, 2, 4, 28, 4, 6);
        Round111220GameTests.carve(h, 29, 3, 4, 36, 5, 6);
        Round111220GameTests.carve(h, 33, 3, 4, 35, 12, 6);
        Round111220GameTests.carve(h, 14, 10, 4, 35, 12, 6);
        ServerPlayer[] owner = new ServerPlayer[1];
        Mob w = still(h, new Vec3(6.5, 2, 5.5), owner);
        Object s = find(h, w, new Vec3(18.5, 10, 5.5), WingmanNavCost.CONFINED);
        h.assertTrue(status(s).equals("FOUND"), "no way to the upper tunnel: " + status(s));
        List<Vec3> path = waypoints(s);
        h.assertTrue(path.size() >= 40, "the way is too short to go round (" + path.size() + " cells): it cuts through rock");
        boolean shaft = path.stream().map(p -> rel(h, p)).anyMatch(p -> p.x >= 33 && p.y >= 6 && p.y <= 9);
        h.assertTrue(shaft, "the way does not climb the shaft");
        done(h, w, owner[0]);
    }

    @GameTest(template = "cave", batch = "round111222_floor", timeoutTicks = 40)
    public static void underACeilingThePathKeepsToTheFloor(GameTestHelper h) {
        Round111220GameTests.shell(h, true);
        Round111220GameTests.carve(h, 2, 2, 3, 40, 7, 7);    // a 6-high hall
        ServerPlayer[] owner = new ServerPlayer[1];
        Mob w = still(h, new Vec3(5.5, 5, 5.5), owner);       // 3 above the floor
        Vec3 goal = new Vec3(35.5, 2, 5.5);
        double withCost = meanHeight(h, waypoints(find(h, w, goal, WingmanNavCost.CONFINED)));
        double without = meanHeight(h, waypoints(find(h, w, goal, 0.0)));
        h.assertTrue(withCost < 0.5, "with the height cost the path flies " + withCost + " above the floor on average");
        h.assertTrue(withCost < without, "the height cost does not lower the path (" + withCost + " vs " + without + ")");
        done(h, w, owner[0]);
    }

    private static double meanHeight(GameTestHelper h, List<Vec3> path) {
        return path.stream().mapToDouble(p -> rel(h, p).y - 2).average().orElse(99);
    }

    @GameTest(template = "cave", batch = "round111222_glass", timeoutTicks = 40)
    public static void noWayThroughGlass(GameTestHelper h) {
        Round111220GameTests.shell(h, true);
        Round111220GameTests.carve(h, 2, 2, 4, 20, 4, 6);
        Round111220GameTests.fill(h, 21, 2, 4, 21, 4, 6, Blocks.GLASS.defaultBlockState());
        Round111220GameTests.carve(h, 22, 2, 4, 26, 4, 6);
        ServerPlayer[] owner = new ServerPlayer[1];
        Mob w = still(h, new Vec3(6.5, 2, 5.5), owner);
        Object s = find(h, w, new Vec3(24.5, 2, 5.5), WingmanNavCost.CONFINED);
        h.assertTrue(status(s).equals("FAILED"), "a way through the glass: " + status(s));
        done(h, w, owner[0]);
    }

    @GameTest(template = "cave", batch = "round111222_lava", timeoutTicks = 40)
    public static void neverOverLava(GameTestHelper h) {
        Round111220GameTests.shell(h, true);
        Round111220GameTests.carve(h, 2, 2, 4, 30, 4, 6);
        Round111220GameTests.fill(h, 12, 1, 4, 14, 1, 6, Blocks.LAVA.defaultBlockState());
        ServerPlayer[] owner = new ServerPlayer[1];
        Mob w = still(h, new Vec3(6.5, 2, 5.5), owner);
        Object s = find(h, w, new Vec3(25.5, 2, 5.5), WingmanNavCost.CONFINED);
        h.assertTrue(status(s).equals("FOUND"), "no way past the lava: " + status(s));
        for (Vec3 p : waypoints(s)) {
            Vec3 r = rel(h, p);
            boolean overLava = r.x >= 12 && r.x < 15 && r.y < 3;
            h.assertTrue(!overLava, "the path stands right over the lava at " + r);
        }
        done(h, w, owner[0]);
    }

    /** The height costs the tests use (the navigator's under a ceiling). */
    static final class WingmanNavCost {
        static final double CONFINED = 0.6;
    }
}
