package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.45 (Wingman motion audit M1, decision two A): the flight attitude banks into turns, raises the head in a climb,
 * flares back when braking, stays within its bounds; the legs tuck in a boost and come back without a step; a turning,
 * climbing and braking flight changes the attitude smoothly. {@code WingmanPose} is reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11545GameTests {
    private Round11545GameTests() {}

    private static Object call(String name, Object... args) { return Round33GameTests.call(Round33GameTests.common("armory.WingmanPose"), name, args); }

    private static float[] att(float f, float r, float up, float turn, float brake) { return (float[]) call("attitude", f, r, up, turn, brake, 1F); }

    /** Bank, climb, brake and the bounds. */
    @GameTest(template = "empty", batch = "round11545_attitude", timeoutTicks = 20)
    public static void attitudeBanksClimbsAndFlares(GameTestHelper h) {
        float[] level = att(0.5F, 0, 0, 0, 0), right = att(0.5F, 0, 0, 12, 0), left = att(0.5F, 0, 0, -12, 0);
        float[] climb = att(0.5F, 0, 0.3F, 0, 0), dive = att(0.5F, 0, -0.3F, 0, 0), brake = att(0.5F, 0, 0, 0, 0.05F);
        float[] hoverTurn = att(0, 0, 0, 12, 0), wild = att(5, -5, 5, 500, 5);
        h.assertTrue(level[0] > 25 && level[0] <= 45, "level pitch " + level[0]);
        h.assertTrue(right[1] > 10 && left[1] < -10, "bank " + right[1] + " / " + left[1]);
        h.assertTrue(climb[0] < level[0] - 5 && dive[0] > level[0] + 5, "climb " + climb[0] + ", dive " + dive[0]);
        h.assertTrue(brake[0] < level[0] - 5, "brake " + brake[0]);
        h.assertTrue(Math.abs(hoverTurn[1]) < 1e-3, "a hover turn banks: " + hoverTurn[1]);
        h.assertTrue(wild[0] >= -32 && wild[0] <= 55 && Math.abs(wild[1]) <= 42 && Float.isFinite(wild[0]), "bounds " + wild[0] + "/" + wild[1]);
        h.succeed();
    }

    /** The boost tuck pulls both thighs forward and is zero before and after it; the legs are straight on the ground. */
    @GameTest(template = "empty", batch = "round11545_legs", timeoutTicks = 20)
    public static void legsTuckInABoost(GameTestHelper h) {
        float[] before = (float[]) call("legs", 0.5F, 1F, 0F, 0F, -5F), mid = (float[]) call("legs", 0.5F, 1F, 0F, 0F, 6F);
        float[] after = (float[]) call("legs", 0.5F, 1F, 0F, 0F, 20F), ground = (float[]) call("legs", 0.5F, 0F, 0.1F, 30F, 6F);
        h.assertTrue(mid[0] < before[0] - 0.6F && mid[3] < before[3] - 0.5F, "no tuck: " + mid[0] + " / " + before[0]);
        h.assertTrue(Math.abs(after[0] - before[0]) < 1e-5, "the tuck stays");
        h.assertTrue(Math.abs(ground[0]) < 1e-5 && Math.abs(ground[3]) < 1e-5, "legs swing on the ground");
        float worst = 0;
        float[] last = null;
        for (int i = -20; i <= 80; i++) {
            float[] l = (float[]) call("legs", 0.5F, 1F, 0F, 10F, i / 4F);
            if (last != null) worst = Math.max(worst, Math.max(Math.abs(l[0] - last[0]), Math.abs(l[3] - last[3])));
            last = l;
        }
        h.assertTrue(worst < 0.2F, "the tuck steps by " + worst + " rad in a quarter tick");
        h.succeed();
    }

    /** A turning, climbing, braking flight within the Wingman's limits: the attitude changes by at most 15 degrees per tick. */
    @GameTest(template = "empty", batch = "round11545_sequence", timeoutTicks = 20)
    public static void attitudeSequenceIsSmooth(GameTestHelper h) {
        float moveF = 0, moveR = 0, moveU = 0, turn = 0, brake = 0, yaw = 0, lastYaw = 0;
        double vx = 0, vy = 0, vz = 0;
        float[] last = null;
        double worst = 0;
        for (int tick = 0; tick < 300; tick++) {
            double speed = tick < 50 ? 0.7 : tick < 150 ? 0.6 : tick < 180 ? 0.0 : 0.5;
            double heading = tick < 50 ? 0 : tick < 150 ? (tick - 50) * 0.08 : tick < 180 ? Math.PI * 0.8 : Math.PI * 0.8 + (tick - 180) * -0.05;
            double wantUp = tick < 100 ? 0.2 : tick < 200 ? -0.25 : 0;
            float wantYaw = (float) Math.toDegrees(heading);
            lastYaw = yaw;
            yaw += Math.max(-40, Math.min(40, (float) Math.IEEEremainder(wantYaw - yaw, 360)));
            double wx = -Math.sin(heading) * speed - vx, wz = Math.cos(heading) * speed - vz, wy = wantUp - vy;
            double step = Math.sqrt(wx * wx + wy * wy + wz * wz);
            if (step > 0.14) { wx *= 0.14 / step; wy *= 0.14 / step; wz *= 0.14 / step; }
            vx += wx; vy += wy; vz += wz;
            float[] frame = (float[]) call("bodyFrame", vx, vz, yaw);
            float oldF = moveF;
            moveF += (frame[0] - moveF) * 0.2F;
            moveR += (frame[1] - moveR) * 0.2F;
            moveU += ((float) vy - moveU) * 0.2F;
            turn += ((float) Math.IEEEremainder(yaw - lastYaw, 360) - turn) * 0.10F;   // WingmanEntity.TURN_SMOOTH
            brake += (Math.min(0.12F, Math.max(0, oldF - moveF) * 5) - brake) * 0.15F;   // as WingmanEntity (FULL_BRAKE, BRAKE_SMOOTH)
            float[] a = att(moveF, moveR, moveU, turn, brake);
            for (float v : a) h.assertTrue(Float.isFinite(v), "non-finite at tick " + tick);
            if (last != null) worst = Math.max(worst, Math.max(Math.abs(a[0] - last[0]), Math.abs(a[1] - last[1])));
            last = a;
        }
        h.assertTrue(worst <= 15, "the attitude jumped by " + worst + " degrees in a tick");
        h.succeed();
    }
}
