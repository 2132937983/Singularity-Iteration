package example.sicore;

import java.lang.reflect.Field;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.50 (escort and open Armory phase F1, decision three B): the escort flight law keeps a Wingman at its place behind
 * an owner sprinting, flying, gliding or in maneuver flight up to 4 blocks per tick (offline, with the server's uneven
 * owner positions too); its limits and the speed cost; the owner velocity measured from positions; the speed cost paid
 * with the upkeep, and the Wingman slows to cruise (without flying home) when only the speed cannot be paid; the catch-up
 * jump keeps the owner's velocity. {@code WingmanFlight} is reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11550GameTests {
    private Round11550GameTests() {}

    private static final Class<?> FLIGHT = Round33GameTests.common("armory.WingmanFlight");

    private static Object f(String name, Object... args) { return Round33GameTests.call(FLIGHT, name, args); }

    private static double d(String name, Object... args) { return (Double) f(name, args); }

    private interface Profile { double at(int tick); }

    /**
     * Flies the law offline: the owner moves at {@code speed(t)} along {@code heading(t)}; the server sees its moves as they
     * are, or ({@code uneven}) with every third tick missing and the next one doubled. Returns {worst distance from the place
     * after tick 30, top speed}.
     */
    private static double[] fly(Profile speed, Profile heading, boolean uneven) {
        Vec3 owner = Vec3.ZERO, ownerVel = Vec3.ZERO, pending = Vec3.ZERO, pos = new Vec3(-1.6, 1.2, -1.8), vel = Vec3.ZERO;
        double worst = 0, top = 0;
        for (int t = 0; t < 400; t++) {
            double s = speed.at(t), hd = heading.at(t);
            Vec3 move = new Vec3(-Math.sin(hd) * s, 0, Math.cos(hd) * s);
            if (uneven) {
                pending = pending.add(move);
                move = t % 3 == 1 ? Vec3.ZERO : pending;
                if (t % 3 != 1) pending = Vec3.ZERO;
            }
            owner = owner.add(move);
            ownerVel = (Vec3) f("ownerVelocity", ownerVel, move);
            double fx = -Math.sin(hd), fz = Math.cos(hd), rx = -Math.cos(hd), rz = -Math.sin(hd);
            Vec3 place = owner.add(rx * -1.6 + fx * -1.8, 1.2, rz * -1.6 + fz * -1.8);   // slot 0 of the formation
            if (t > 30) worst = Math.max(worst, place.distanceTo(pos));
            double os = ownerVel.length();
            Vec3 want = (Vec3) f("want", place.subtract(pos), ownerVel, d("cap", os, 0.9));
            vel = (Vec3) f("accelerate", vel, want, d("accel", os, 0.14));
            pos = pos.add(vel);
            top = Math.max(top, vel.length());
        }
        return new double[]{worst, top};
    }

    /** The Wingman stays at its place at every owner speed, through a turn and a stop; never faster than the escort limit. */
    @GameTest(template = "empty", batch = "round11550_flight", timeoutTicks = 20)
    public static void escortKeepsUpAtEverySpeed(GameTestHelper h) {
        Profile straight = t -> 0;
        Object[][] cases = {
            {"sprint", (Profile) t -> 0.28, straight, 0.5, 1.0},
            {"creative flight", (Profile) t -> 1.09, straight, 0.5, 2.5},
            {"elytra", (Profile) t -> 1.7, straight, 0.5, 3.5},
            {"to the shock barrier", (Profile) t -> Math.min(2.4, t * 4 / 59.0), straight, 1.0, 4.5},
            {"full maneuver", (Profile) t -> Math.min(4.0, t * 4 / 59.0), straight, 1.0, 7.0},
            {"turn at full speed", (Profile) t -> Math.min(4.0, t * 4 / 59.0), (Profile) t -> t < 100 ? 0 : Math.min(Math.PI, (t - 100) * 0.06), 2.0, 7.0},
            {"stop from full speed", (Profile) t -> t < 150 ? 4.0 : Math.max(0, 4.0 - (t - 150) * 0.4), straight, 4.5, 9.0}};
        for (Object[] c : cases) {
            double[] even = fly((Profile) c[1], (Profile) c[2], false), uneven = fly((Profile) c[1], (Profile) c[2], true);
            h.assertTrue(even[0] <= (Double) c[3], c[0] + ": " + even[0] + " blocks from the place");
            h.assertTrue(uneven[0] <= (Double) c[4], c[0] + " (uneven positions): " + uneven[0] + " blocks from the place");
            h.assertTrue(even[1] <= 6.0 + 1e-9 && uneven[1] <= 6.0 + 1e-9, c[0] + ": top speed " + Math.max(even[1], uneven[1]));
        }
        h.succeed();
    }

    /** The limits grow with the owner's speed from the old values; the speed cost starts at cruise. */
    @GameTest(template = "empty", batch = "round11550_limits", timeoutTicks = 20)
    public static void limitsAndSpeedCost(GameTestHelper h) {
        h.assertTrue(d("cap", 0.0, 0.9) == 0.9 && d("accel", 0.0, 0.14) == 0.14, "an owner at rest: the old limits");
        h.assertTrue(Math.abs(d("cap", 4.0, 0.9) - 5.5) < 1e-9 && d("cap", 20.0, 0.9) == 6.0, "the cap follows the owner up to 6");
        h.assertTrue(d("accel", 4.0, 0.14) > 0.7 && d("accel", 20.0, 0.14) == 0.9, "the acceleration grows up to 0.9");
        h.assertTrue(d("speedCost", 0.9, 0.9) == 0 && d("speedCost", 0.5, 0.9) == 0, "no cost at or below cruise");
        h.assertTrue(Math.abs(d("speedCost", 4.0, 0.9) - 30 * (16 - 0.81)) < 1e-6, "the maneuver formula above cruise");
        Vec3 v = (Vec3) f("ownerVelocity", Vec3.ZERO, new Vec3(2, 0, 0));
        h.assertTrue(Math.abs(v.x - 1.0) < 1e-9, "the owner velocity is smoothed");
        h.assertTrue(((Vec3) f("ownerVelocity", new Vec3(2, 0, 0), new Vec3(40, 0, 0))).equals(Vec3.ZERO), "a teleport resets it");
        h.succeed();
    }

    private static void setDouble(Object target, String field, double value) {
        try {
            Field fld = target.getClass().getDeclaredField(field);
            fld.setAccessible(true);
            fld.setDouble(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(field, e);
        }
    }

    private static int intField(Object target, String field) {
        try {
            Field fld = target.getClass().getDeclaredField(field);
            fld.setAccessible(true);
            return fld.getInt(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(field, e);
        }
    }

    /** The speed flown is paid with the upkeep; when only the speed cannot be paid, the Wingman slows but does not go home. */
    @GameTest(template = "empty", batch = "round11550_upkeep", timeoutTicks = 20)
    public static void speedIsPaidWithTheUpkeep(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Entity w = Round1102GameTests.wingman(h, owner, new Vec3(1.5, 4, 1.5));
        Round33GameTests.call(w, "setReserve", 100_000L);
        setDouble(w, "speedDue", 1000);
        Round33GameTests.call(w, "upkeep", h.getLevel(), owner);
        long reserve = (Long) Round33GameTests.call(w, "reserve");
        h.assertTrue(reserve == 100_000L - 160 - 1000, "the reserve pays upkeep and speed: " + reserve);
        h.assertTrue(!(Boolean) Round33GameTests.call(w, "speedStarved"), "paid: full escort speed");
        Round33GameTests.call(w, "setReserve", 200L);
        setDouble(w, "speedDue", 1000);
        Round33GameTests.call(w, "upkeep", h.getLevel(), owner);
        h.assertTrue((Boolean) Round33GameTests.call(w, "speedStarved"), "the speed could not be paid: cruise only");
        h.assertTrue(intField(w, "lowPowerSeconds") == 0, "the upkeep itself was paid: no low power");
        h.assertTrue((Byte) Round33GameTests.call(w, "state") == 0, "still active");
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** The owner's velocity comes from its moves; the catch-up jump keeps it (the Wingman does not stop dead). */
    @GameTest(template = "empty", batch = "round11550_catchup", timeoutTicks = 20)
    public static void ownerVelocityAndCatchUp(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Entity w = Round1102GameTests.wingman(h, owner, new Vec3(1.5, 4, 1.5));
        Vec3 start = owner.position();
        for (int i = 0; i < 8; i++) {
            owner.setPos(start.x + 1.5 * i, start.y + 2, start.z);
            Round33GameTests.call(w, "trackOwner", owner);
        }
        Vec3 v = (Vec3) Round33GameTests.call(w, "ownerVelocity");
        h.assertTrue(Math.abs(v.x - 1.5) < 0.02 && Math.abs(v.z) < 1e-6, "owner velocity " + v);
        Round33GameTests.call(w, "catchUp", owner);
        h.assertTrue(w.getDeltaMovement().distanceTo(v) < 1e-9, "the jump keeps the owner's velocity: " + w.getDeltaMovement());
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }
}
