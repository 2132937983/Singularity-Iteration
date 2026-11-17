package example.sicore;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.52 (gaze and Iron Man phase P0, decisions one A and three A): through the whole mob AI step (the vanilla look
 * control included) a fighting Wingman keeps its head on the enemy, up and down too; a new lock starts with a stare in
 * which it does not strike; the neck turns at most 75 degrees from the body; the idle glances of a squad never start
 * together, come every 4 to 10 seconds per Wingman and stop in a fight.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11552GameTests {
    private Round11552GameTests() {}

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }

    /** The full AI step keeps the head on the enemy's eyes (yaw and pitch); the first 6 ticks are a stare without a hit. */
    @GameTest(template = "empty", batch = "round11552_gaze", timeoutTicks = 40)
    public static void headStaysOnTheEnemy(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Mob w = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(1.5, 2, 1.5));
        Zombie zombie = EntityType.ZOMBIE.create(h.getLevel());
        zombie.moveTo(w.getX() + 3.0, w.getY() + 2.5, w.getZ() + 1.0);
        zombie.setNoAi(true);
        h.getLevel().addFreshEntity(zombie);
        float health = zombie.getHealth();
        setField(w, "target", zombie);
        Round33GameTests.call(w, "serverAiStep");
        h.assertTrue((Integer) Round33GameTests.call(w, "glareTicks") == 5, "the stare did not start: " + Round33GameTests.call(w, "glareTicks"));
        for (int i = 0; i < 5; i++) Round33GameTests.call(w, "serverAiStep");
        h.assertTrue(zombie.getHealth() == health, "struck during the stare");
        for (int i = 0; i < 8; i++) Round33GameTests.call(w, "serverAiStep");
        Vec3 d = zombie.getEyePosition().subtract(w.getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        h.assertTrue(Math.abs(Mth.wrapDegrees(w.yHeadRot - yaw)) < 3, "head yaw " + w.yHeadRot + ", the enemy at " + yaw);
        h.assertTrue(Math.abs(w.getXRot() - pitch) < 3 && pitch < -10, "head pitch " + w.getXRot() + ", the enemy at " + pitch);
        zombie.discard();
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** A look behind turns the head at once and pulls the body along: at most 75 degrees between them. */
    @GameTest(template = "empty", batch = "round11552_neck", timeoutTicks = 20)
    public static void neckTurnsAtMost75Degrees(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Mob w = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(1.5, 4, 1.5));
        w.setYRot(0);
        w.yHeadRot = 0;
        w.yBodyRot = 0;
        Round33GameTests.call(w, "face", w.getEyePosition().add(1, 0, -10));   // nearly behind (south-facing body)
        float neck = Math.abs(Mth.wrapDegrees(w.yHeadRot - w.getYRot()));
        h.assertTrue(neck <= 75.01F, "the neck turned " + neck + " degrees");
        h.assertTrue(Math.abs(Mth.wrapDegrees(w.yBodyRot - w.getYRot())) < 1e-3, "the body yaw " + w.yBodyRot + " / " + w.getYRot());
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** Four Wingmen glance on their own: never two starts within a second, every 4 to 10 s, none in a fight. */
    @GameTest(template = "empty", batch = "round11552_glance", timeoutTicks = 20)
    public static void squadGlancesAreNotInStep(GameTestHelper h) {
        Class<?> type = Round33GameTests.common("armory.WingmanGlance");
        Object shared;
        Object[] squad = new Object[4];
        try {
            shared = Round33GameTests.common("armory.WingmanGlance$Shared").getConstructor().newInstance();
            for (int i = 0; i < 4; i++) squad[i] = type.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("WingmanGlance", e);
        }
        RandomSource[] random = new RandomSource[4];
        for (int i = 0; i < 4; i++) random[i] = RandomSource.create(1234 + i);   // as a squad deployed together
        List<long[]> starts = new ArrayList<>();
        long[] last = {-1, -1, -1, -1};
        for (long now = 0; now < 6000; now++) {
            float idle = now >= 3000 && now < 3600 ? 0F : 1F;   // a fight from 3000 to 3600
            for (int i = 0; i < 4; i++) {
                boolean started = (Boolean) Round33GameTests.call(squad[i], "tick", now, idle, random[i], shared, null);
                if (!started) continue;
                h.assertTrue(idle > 0.5F, "a glance started in a fight at " + now);
                if (last[i] >= 0) h.assertTrue(now - last[i] >= 100, "Wingman " + i + " glanced again after " + (now - last[i]) + " ticks");
                last[i] = now;
                starts.add(new long[]{now, i});
            }
            if (now == 3100) {
                for (int i = 0; i < 4; i++) {
                    float[] off = (float[]) Round33GameTests.call(squad[i], "offset", 1F);
                    h.assertTrue(Math.abs(off[0]) < 0.05F && Math.abs(off[1]) < 0.05F, "Wingman " + i + " still looks aside in a fight");
                }
            }
        }
        for (int a = 1; a < starts.size(); a++)
            h.assertTrue(starts.get(a)[0] - starts.get(a - 1)[0] >= 20, "two glances started together at " + starts.get(a)[0]);
        int[] count = new int[4];
        for (long[] s : starts) count[(int) s[1]]++;
        for (int i = 0; i < 4; i++) h.assertTrue(count[i] >= 15 && count[i] <= 60, "Wingman " + i + " glanced " + count[i] + " times");
        h.succeed();
    }
}
