package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.51 (escort and open Armory phase F2, decision four A): the formation is the old one at rest, follows the flight
 * direction at speed (a look back does not swing it) and opens into a V without a jump; all four slots hold their places
 * on a circle at 4 blocks per tick (offline); the high-speed pose comes in between 1.5 and 2.5 blocks per tick, lies the
 * suit down to 80 degrees, and leaves the arms free in a fight. {@code WingmanFlight} and {@code WingmanPose} are reached
 * by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11551GameTests {
    private Round11551GameTests() {}

    private static Object flight(String name, Object... args) { return Round33GameTests.call(Round33GameTests.common("armory.WingmanFlight"), name, args); }

    private static Object pose(String name, Object... args) { return Round33GameTests.call(Round33GameTests.common("armory.WingmanPose"), name, args); }

    private static Vec3 place(float yaw, Vec3 velocity, int slot) { return (Vec3) flight("place", Vec3.ZERO, yaw, velocity, slot, 1.0); }

    /** At rest: the old offsets around the view; at speed: behind the flight whatever the view, the V open; no jumps. */
    @GameTest(template = "empty", batch = "round11551_formation", timeoutTicks = 20)
    public static void formationFollowsTheFlightAndOpens(GameTestHelper h) {
        double[][] old = {{-1.6, 1.2, -1.8}, {1.6, 1.2, -1.8}, {-2.9, 1.7, -0.6}, {2.9, 1.7, -0.6}};
        for (int slot = 0; slot < 4; slot++) {
            for (float yaw : new float[]{0, 90, -135}) {
                double y = Math.toRadians(yaw), fx = -Math.sin(y), fz = Math.cos(y), rx = -Math.cos(y), rz = -Math.sin(y);
                Vec3 want = new Vec3(rx * old[slot][0] + fx * old[slot][2], old[slot][1], rz * old[slot][0] + fz * old[slot][2]);
                h.assertTrue(place(yaw, Vec3.ZERO, slot).distanceTo(want) < 1e-9, "slot " + slot + " at rest, yaw " + yaw);
            }
        }
        Vec3 south = new Vec3(0, 0, 4);   // flying south (+z) at 4 blocks per tick
        for (int slot = 0; slot < 4; slot++) {
            Vec3 ahead = place(0, south, slot), lookingBack = place(180, south, slot);
            h.assertTrue(ahead.distanceTo(lookingBack) < 1e-9, "slot " + slot + ": a look back swings the formation");
            h.assertTrue(ahead.z < -2.5, "slot " + slot + " is not behind the flight: " + ahead);
        }
        h.assertTrue(Math.abs(Math.abs(place(0, south, 2).x) - 4.4) < 1e-9 && Math.abs(Math.abs(place(0, south, 0).x) - 2.2) < 1e-9,
            "the V is open at full speed");
        for (float yaw : new float[]{0, 90}) {
            for (int slot = 0; slot < 4; slot++) {
                double worst = 0;
                Vec3 last = null;
                for (int i = 0; i <= 400; i++) {
                    Vec3 p = place(yaw, new Vec3(0, 0, i * 0.01), slot);
                    if (last != null) worst = Math.max(worst, p.distanceTo(last));
                    last = p;
                }
                h.assertTrue(worst < 0.15, "slot " + slot + ", yaw " + yaw + ": the place jumps by " + worst + " per 0.01 b/t");
            }
        }
        h.succeed();
    }

    /** All four slots hold their places on a circle of 64 blocks at 4 blocks per tick (as the capture scene flies). */
    @GameTest(template = "empty", batch = "round11551_circle", timeoutTicks = 20)
    public static void squadHoldsTheVOnACircle(GameTestHelper h) {
        double radius = 64;
        Vec3[] pos = new Vec3[4], vel = new Vec3[4];
        Vec3 owner = new Vec3(radius, 0, 0), ownerVel = Vec3.ZERO;
        for (int s = 0; s < 4; s++) { pos[s] = place(0, Vec3.ZERO, s).add(owner); vel[s] = Vec3.ZERO; }
        double angle = 0, worst = 0;
        for (int t = 0; t < 500; t++) {
            double speed = Math.min(4.0, (t + 1) * 4.0 / 60);
            angle += speed / radius;
            Vec3 next = new Vec3(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            ownerVel = (Vec3) flight("ownerVelocity", ownerVel, next.subtract(owner));
            owner = next;
            float yaw = (float) Math.toDegrees(angle);
            double os = ownerVel.length();
            for (int s = 0; s < 4; s++) {
                Vec3 place = place(yaw, ownerVel, s).add(owner);
                if (t > 120) worst = Math.max(worst, place.distanceTo(pos[s]));
                Vec3 want = (Vec3) flight("want", place.subtract(pos[s]), ownerVel, (Double) flight("cap", os, 0.9));
                vel[s] = (Vec3) flight("accelerate", vel[s], want, (Double) flight("accel", os, 0.14));
                pos[s] = pos[s].add(vel[s]);
            }
        }
        h.assertTrue(worst < 1.5, "a Wingman strays " + worst + " blocks from its place on the circle");
        h.succeed();
    }

    /** The high-speed pose: its weight, the pitch, the arms (free in a fight) and the legs. */
    @GameTest(template = "empty", batch = "round11551_pose", timeoutTicks = 20)
    public static void highSpeedPose(GameTestHelper h) {
        h.assertTrue((Float) pose("proneTarget", 1.4F, 1F) == 0 && (Float) pose("proneTarget", 2.6F, 1F) == 1, "the weight 1.5 .. 2.5");
        h.assertTrue((Float) pose("proneTarget", 4F, 0F) == 0, "none on the ground");
        float[] att = {30F, 20F};
        float[] full = (float[]) pose("prone", att, 1F), none = (float[]) pose("prone", att, 0F);
        h.assertTrue(full[0] == 80F && Math.abs(full[1] - 10F) < 1e-5, "full pose " + full[0] + "/" + full[1]);
        h.assertTrue(none[0] == 30F && none[1] == 20F, "no pose at weight 0");
        float[] arms = {-1.0F, 0.2F, 0.1F, -0.5F, -0.1F, -0.1F, 0F}, fight = arms.clone();
        pose("proneArms", arms, 1F, 0F);
        pose("proneArms", fight, 1F, 1F);
        h.assertTrue(Math.abs(arms[0] - 0.12F) < 1e-5 && Math.abs(arms[3] - 0.12F) < 1e-5, "the arms lie along the body");
        h.assertTrue(java.util.Arrays.equals(fight, new float[]{-1.0F, 0.2F, 0.1F, -0.5F, -0.1F, -0.1F, 0F}), "a fight keeps the arms free");
        float[] legs = {0.3F, 0, 0.1F, 0.2F, 0, -0.1F};
        h.assertTrue(java.util.Arrays.equals((float[]) pose("proneLegs", legs, 0F), legs), "no leg change at weight 0");
        h.assertTrue(Math.abs(((float[]) pose("proneLegs", legs, 1F))[0] - 0.04F) < 1e-5, "legs stretched at full weight");
        h.succeed();
    }
}
