package example.sicore;

import java.lang.reflect.Field;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.58 (gaze and Iron Man phase P6): the battle damage levels and the unsteady flight stay within their bounds; the
 * callout limits (one line per Wingman per 3 s, per squad per 1.5 s, urgent lines always); a piece worn below a quarter
 * is called once as it crosses.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11558GameTests {
    private Round11558GameTests() {}

    private static Object pose(String name, Object... args) { return Round33GameTests.call(Round33GameTests.common("armory.WingmanPose"), name, args); }

    private static Object callouts(String name, Object... args) { return Round33GameTests.call(Round33GameTests.common("armory.WingmanCallouts"), name, args); }

    /** Damage levels by the worst piece; the shake only when badly damaged and in the air, at most 3.5 degrees. */
    @GameTest(template = "empty", batch = "round11558_damage", timeoutTicks = 20)
    public static void damageLevelsAndShake(GameTestHelper h) {
        h.assertTrue((Integer) pose("damageLevel", 0.8F) == 0 && (Integer) pose("damageLevel", 0.4F) == 1
            && (Integer) pose("damageLevel", 0.2F) == 2 && (Integer) pose("damageLevel", Float.NaN) == 0, "the damage levels");
        float[] calm = (float[]) pose("damageShake", 10F, 1, 1F), ground = (float[]) pose("damageShake", 10F, 2, 0F);
        h.assertTrue(calm[0] == 0 && calm[1] == 0 && ground[0] == 0 && ground[1] == 0, "a shake when not badly damaged, or on the ground");
        float worst = 0, moved = 0;
        float[] last = null;
        for (int i = 0; i < 2000; i++) {
            float[] s = (float[]) pose("damageShake", i / 4F, 2, 1F);
            worst = Math.max(worst, Math.max(Math.abs(s[0]), Math.abs(s[1])));
            if (last != null) moved = Math.max(moved, Math.max(Math.abs(s[0] - last[0]), Math.abs(s[1] - last[1])));
            last = s;
        }
        h.assertTrue(worst <= 3.5F && worst > 1F, "the shake reaches " + worst + " degrees");
        h.assertTrue(moved < 1.5F, "the shake jumps by " + moved + " degrees in a quarter tick");
        h.succeed();
    }

    /** The limits of the callouts. */
    @GameTest(template = "empty", batch = "round11558_limits", timeoutTicks = 20)
    public static void calloutLimits(GameTestHelper h) {
        h.assertTrue((Boolean) callouts("allow", 100L, 0L, 0L, false), "a first line is held back");
        h.assertTrue(!(Boolean) callouts("allow", 100L, 50L, 0L, false), "a Wingman spoke again within 3 s");
        h.assertTrue(!(Boolean) callouts("allow", 100L, 0L, 80L, false), "the squad spoke again within 1.5 s");
        h.assertTrue((Boolean) callouts("allow", 100L, 99L, 99L, true), "an urgent line was held back");
        h.succeed();
    }

    private static long lastCallout(Object w) {
        try {
            Field f = w.getClass().getDeclaredField("lastCallout");
            f.setAccessible(true);
            return f.getLong(w);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("lastCallout", e);
        }
    }

    /** A helmet worn from 30 % to 20 % is called once; worn further (still above the repair line) it is not called again. */
    @GameTest(template = "empty", batch = "round11558_damaged", timeoutTicks = 20)
    public static void damagedPieceIsCalledOnce(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Mob w = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(1.5, 2, 2.5));
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        int max = helmet.getMaxDamage();
        helmet.setDamageValue(max - (int) (max * 0.30F));
        w.setItemSlot(EquipmentSlot.HEAD, helmet);
        long before = lastCallout(w);
        Round33GameTests.call(w, "wear", helmet, EquipmentSlot.HEAD, (int) (max * 0.10F));
        long first = lastCallout(w);
        h.assertTrue(first != before, "the damaged helmet was not called");
        Round33GameTests.call(w, "wear", helmet, EquipmentSlot.HEAD, 1);
        h.assertTrue(lastCallout(w) == first, "the damaged helmet was called again");
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }
}
