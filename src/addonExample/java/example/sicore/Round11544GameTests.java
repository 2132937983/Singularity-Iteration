package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.44 (Wingman motion audit M0): the suit tilts along its flight (forward, back, to the side), and an offline run of
 * the pose math over a synthetic flight (start, circle strafe, reversal, stop, landing, weapon changes, combat on and off)
 * stays finite and moves no angle faster than its bound: the regression check of the motion batches. {@code WingmanPose}
 * is pure math in the common classes and is reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11544GameTests {
    private Round11544GameTests() {}

    private static final String POSE = "armory.WingmanPose";

    private static Object call(String name, Object... args) { return Round33GameTests.call(Round33GameTests.common(POSE), name, args); }

    private static float[] lean(float forward, float right, float air) { return (float[]) call("lean", forward, right, air); }

    /** The tilt follows the flight: forward leans forward, backwards leans back, sideways rolls; none on the ground. */
    @GameTest(template = "empty", batch = "round11544_lean", timeoutTicks = 20)
    public static void leanFollowsTheFlight(GameTestHelper h) {
        float[] ahead = lean(0.6F, 0, 1), back = lean(-0.6F, 0, 1), right = lean(0, 0.6F, 1), ground = lean(0.6F, 0.6F, 0);
        h.assertTrue(ahead[0] > 30 && Math.abs(ahead[1]) < 1e-3, "ahead " + ahead[0] + "/" + ahead[1]);
        h.assertTrue(back[0] < -10 && back[0] > -ahead[0], "back " + back[0]);
        h.assertTrue(Math.abs(right[0]) < 1e-3 && right[1] > 15, "sideways " + right[0] + "/" + right[1]);
        h.assertTrue(ground[0] == 0 && ground[1] == 0, "on the ground " + ground[0] + "/" + ground[1]);
        float[] south = (float[]) call("bodyFrame", 0.0, 1.0, 0F);        // facing south (+z), moving south
        float[] west = (float[]) call("bodyFrame", -1.0, 0.0, 0F);        // facing south, moving west: to its right
        float[] facingWest = (float[]) call("bodyFrame", -1.0, 0.0, 90F); // facing west, moving west: ahead
        h.assertTrue(near(south[0], 1) && near(south[1], 0), "south " + south[0] + "/" + south[1]);
        h.assertTrue(near(west[0], 0) && near(west[1], 1), "west " + west[0] + "/" + west[1]);
        h.assertTrue(near(facingWest[0], 1) && near(facingWest[1], 0), "facing west " + facingWest[0] + "/" + facingWest[1]);
        h.succeed();
    }

    private static boolean near(float a, float b) { return Math.abs(a - b) < 1e-4; }

    /**
     * 300 ticks of a synthetic flight at 4 frames per tick, within the Wingman's limits (acceleration 0.14 blocks per tick,
     * turning 40 degrees per tick; a thruster boost is a deliberate burst and not part of it): the smoothed tilt changes by
     * at most 15 degrees per tick, the
     * smoothed arms by at most their rate limit, and every value is finite.
     */
    @GameTest(template = "empty", batch = "round11544_sequence", timeoutTicks = 20)
    public static void flightSequenceHasNoJumps(GameTestHelper h) {
        Class<?> holdType = Round33GameTests.common(POSE + "$Hold");
        Object[] holds = holdType.getEnumConstants();
        float maxRate;
        try {
            maxRate = Round33GameTests.common(POSE).getField("MAX_RATE").getFloat(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("MAX_RATE", e);
        }
        float moveF = 0, moveR = 0, yaw = 0;
        double vx = 0, vz = 0;
        float[] state = new float[7], lastPose = null, lastLean = null;
        double worstLean = 0, worstArm = 0;
        for (int tick = 0; tick < 300; tick++) {
            // velocity in the world: accelerate ahead, circle strafe, reverse, stop
            double speed = tick < 60 ? tick / 60.0 * 0.7 : tick < 160 ? 0.5 : tick < 200 ? -0.4 : 0;
            double heading = tick < 60 ? 0 : tick < 160 ? (tick - 60) * 0.06 : Math.PI;
            float wantYaw = tick < 60 ? 0 : tick < 160 ? (float) Math.toDegrees(heading) - 90 : 0;   // strafing: facing 90 degrees off
            // as the Wingman flies: the velocity changes by at most 0.14 per tick (ACCEL), the facing by at most 40 degrees
            yaw += Math.max(-40, Math.min(40, (float) Math.IEEEremainder(wantYaw - yaw, 360)));
            double wx = -Math.sin(heading) * speed - vx, wz = Math.cos(heading) * speed - vz, step = Math.sqrt(wx * wx + wz * wz);
            if (step > 0.14) { wx *= 0.14 / step; wz *= 0.14 / step; }
            vx += wx;
            vz += wz;
            double dx = vx, dz = vz;
            float air = tick < 260 ? 1 : Math.max(0, 1 - (tick - 260) / 10F);
            float[] frame = (float[]) call("bodyFrame", dx, dz, yaw);
            moveF += (frame[0] - moveF) * 0.2F;
            moveR += (frame[1] - moveR) * 0.2F;
            float[] tilt = lean(moveF, moveR, air);
            if (lastLean != null) worstLean = Math.max(worstLean, Math.max(Math.abs(tilt[0] - lastLean[0]), Math.abs(tilt[1] - lastLean[1])));
            lastLean = tilt;
            Object hold = holds[(tick / 75) % holds.length];
            float combat = (tick / 40) % 2 == 0 ? 0 : 1;
            for (int frameStep = 0; frameStep < 4; frameStep++) {
                float time = tick + frameStep / 4F;
                float[] pose = (float[]) call("base", hold, false, (float) Math.min(1, Math.abs(speed) * 1.6), air, combat,
                    (float) Math.sin(time * 0.08) * 0.05F, 0.2F, 0.1F, Math.max(0, tilt[0]) * (float) Math.PI / 180);
                call("smooth", state, pose, time);
                for (float v : pose) h.assertTrue(Float.isFinite(v), "a non-finite angle at tick " + tick);
                if (lastPose != null) for (int i = 0; i < 6; i++) worstArm = Math.max(worstArm, Math.abs(pose[i] - lastPose[i]));
                lastPose = pose.clone();
            }
        }
        h.assertTrue(worstLean <= 15, "the tilt jumped by " + worstLean + " degrees in a tick");
        h.assertTrue(worstArm <= maxRate * 0.25 + 1e-4, "an arm moved " + worstArm + " rad in a quarter tick (limit " + maxRate * 0.25 + ")");
        h.succeed();
    }
}
