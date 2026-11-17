package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.47 (Wingman motion audit M4, decision four A): the touchdown cushion and the take-off push start and end at zero,
 * the hover float and the idle look stay within their ranges and only apply when idle, and the head turns first while the
 * body follows at a bounded rate (the aim follows the head). {@code WingmanPose} is reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11547GameTests {
    private Round11547GameTests() {}

    private static float pose(String name, Object... args) {
        return (Float) Round33GameTests.call(Round33GameTests.common("armory.WingmanPose"), name, args);
    }

    /** Cushion and push: zero outside, a peak near 1, no step between quarter ticks; bob and look bounded and gated. */
    @GameTest(template = "empty", batch = "round11547_curves", timeoutTicks = 20)
    public static void idleAndLandingCurves(GameTestHelper h) {
        for (String curve : new String[]{"landingCrouch", "takeoffPush"}) {
            float peak = 0, last = 0, worst = 0;
            for (int i = -8; i <= 60; i++) {
                float v = pose(curve, i / 4F);
                h.assertTrue(v >= 0 && v <= 1, curve + " out of range at " + i / 4F);
                peak = Math.max(peak, v);
                worst = Math.max(worst, Math.abs(v - last));
                last = v;
            }
            h.assertTrue(pose(curve, 0F) == 0 && pose(curve, 15F) == 0 && peak > 0.95F, curve + " peak " + peak);
            h.assertTrue(worst < 0.5F, curve + " steps by " + worst);
        }
        h.assertTrue(pose("hoverBob", 33F, 0F, 0F) == 0 && pose("hoverBob", 33F, 1F, 1F) == 0, "the bob on the ground or at speed");
        h.assertTrue(Math.abs(pose("hoverBob", 14F, 1F, 0F)) <= 0.035F, "the bob is too large");
        h.assertTrue(pose("idleLook", 400F, 0F) == 0, "the look when busy");
        for (int i = 0; i < 2000; i += 7) h.assertTrue(Math.abs(pose("idleLook", (float) i, 1F)) <= 0.6F, "the look too wide at " + i);
        h.succeed();
    }

    /** The head turns to the aim at once, the body follows by at most 18 degrees per tick; the aim error reads the head. */
    @GameTest(template = "empty", batch = "round11547_turn", timeoutTicks = 20)
    public static void headTurnsFirst(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Entity w = Round1102GameTests.wingman(h, owner, new Vec3(1.5, 4, 1.5));
        LivingEntity living = (LivingEntity) w;
        w.setYRot(0);
        living.yHeadRot = 0;
        living.yBodyRot = 0;
        Vec3 aside = w.getEyePosition().add(-10, 0, 0);   // west: yaw 90
        Round33GameTests.call(w, "face", aside);
        h.assertTrue(Math.abs(living.yHeadRot - 90) < 0.5F, "head " + living.yHeadRot);
        h.assertTrue(Math.abs(w.getYRot() - 18) < 0.5F, "body " + w.getYRot());
        h.assertTrue((Double) Round33GameTests.call(w, "facingError", aside) < 1.0, "the aim error reads the body");
        for (int i = 0; i < 5; i++) Round33GameTests.call(w, "face", aside);
        h.assertTrue(Math.abs(w.getYRot() - 90) < 0.5F, "the body did not follow: " + w.getYRot());
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }
}
