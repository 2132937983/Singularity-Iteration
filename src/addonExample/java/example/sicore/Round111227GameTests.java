package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.227 (Wingman cave overhaul G3b, decision three A): the stance of the command wheel. "In the air" keeps a Wingman
 * flying in a low hall where it would go on foot; "On foot" sets it down even in the open; the stance is saved with the
 * Wingman; and the wheel's command sets it. The common classes are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111227GameTests {
    private Round111227GameTests() {}

    static final byte AUTO = 0, AIR = 1, GROUND = 2, A_STANCE = 16;

    private static Mob wingman(GameTestHelper h, ServerPlayer owner) {
        owner.moveTo(h.absoluteVec(new Vec3(3.5, 2, 5.5)));
        owner.getAbilities().invulnerable = true;
        Mob w = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(6.5, 2, 5.5));
        Round33GameTests.call(w, "setReserve", 1_000_000L);
        return w;
    }

    private static boolean ground(Mob w) { return (Boolean) Round33GameTests.call(w, "ground"); }

    private static byte stance(Mob w) { return (Byte) Round33GameTests.call(w, "stance"); }

    private static void lowHall(GameTestHelper h) {
        Round111220GameTests.shell(h, true);
        Round111220GameTests.carve(h, 2, 2, 3, 30, 6, 7);
    }

    private static void done(GameTestHelper h, ServerPlayer owner, net.minecraft.world.entity.Entity... es) {
        for (var e : es) e.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    @GameTest(template = "cave", batch = "round111227_air", timeoutTicks = 80)
    public static void inTheAirKeepsItFlying(GameTestHelper h) {
        lowHall(h);
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Mob w = wingman(h, owner);
        Round33GameTests.call(w, "setStance", AIR);
        h.runAtTickTime(60, () -> {
            boolean on = ground(w);
            done(h, owner, w);
            h.assertTrue(!on, "with the stance \"in the air\" the Wingman goes on foot in a 5-high hall");
        });
    }

    @GameTest(template = "cave", batch = "round111227_ground", timeoutTicks = 80)
    public static void onFootSetsItDownInTheOpen(GameTestHelper h) {
        Round111220GameTests.shell(h, false);
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Mob w = wingman(h, owner);
        Round33GameTests.call(w, "setStance", GROUND);
        h.runAtTickTime(60, () -> {
            boolean on = ground(w);
            done(h, owner, w);
            h.assertTrue(on, "with the stance \"on foot\" the Wingman stays in the air in the open");
        });
    }

    @GameTest(template = "cave", batch = "round111227_saved", timeoutTicks = 40)
    public static void theStanceIsSaved(GameTestHelper h) {
        lowHall(h);
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Mob w = wingman(h, owner);
        Round33GameTests.call(w, "setStance", GROUND);
        CompoundTag tag = w.saveWithoutId(new CompoundTag());
        Mob copy = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(8.5, 2, 5.5));
        copy.load(tag);
        byte got = stance(copy);
        done(h, owner, w, copy);
        h.assertTrue(got == GROUND, "the stance is not saved (read back " + got + ")");
    }

    @GameTest(template = "cave", batch = "round111227_command", timeoutTicks = 40)
    public static void theWheelSetsTheStance(GameTestHelper h) {
        lowHall(h);
        ServerPlayer owner = Round1102GameTests.commander(h);
        Mob w = wingman(h, owner);
        Round33GameTests.call(Round33GameTests.common("armory.WingmanCommands"), "handle", owner,
            Round1102GameTests.command(A_STANCE, w.getUUID(), AIR, Vec3.ZERO, -1));
        byte got = stance(w);
        Round33GameTests.call(Round33GameTests.common("armory.WingmanCommands"), "handle", owner,
            Round1102GameTests.command(A_STANCE, w.getUUID(), (byte) 9, Vec3.ZERO, -1));
        byte afterBad = stance(w);
        done(h, owner, w);
        h.assertTrue(got == AIR, "the wheel's stance command sets " + got);
        h.assertTrue(afterBad == AIR, "a stance out of range is not refused (" + afterBad + ")");
    }
}
