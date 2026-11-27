package example.sicore;

import java.lang.reflect.Field;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.46 (Wingman motion audit M2, decision three A): the hold kind of each weapon, the melee cut from the wind-up to
 * the strike, the bow draw, the crossbow reload and the trident throw start and end at zero and stay bounded, and a melee
 * Wingman stays at its target for a moment after a hit before it boosts away. {@code WingmanPose} is reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11546GameTests {
    private Round11546GameTests() {}

    private static final String POSE = "armory.WingmanPose";

    private static Object pose(String name, Object... args) { return Round33GameTests.call(Round33GameTests.common(POSE), name, args); }

    private static String hold(ItemStack stack) { return String.valueOf(pose("holdOf", stack, false)); }

    /** Every weapon gets its own pose kind. */
    @GameTest(template = "empty", batch = "round11546_kinds", timeoutTicks = 20)
    public static void weaponsHaveTheirHoldKinds(GameTestHelper h) {
        h.assertTrue("BOW".equals(hold(new ItemStack(Items.BOW))), "bow " + hold(new ItemStack(Items.BOW)));
        h.assertTrue("CROSSBOW".equals(hold(new ItemStack(Items.CROSSBOW))), "crossbow");
        h.assertTrue("SPEAR".equals(hold(new ItemStack(Items.TRIDENT))), "trident");
        h.assertTrue("RANGED".equals(hold(Round33GameTests.item("item_tool_laser_miner"))), "laser");
        h.assertTrue("HELD".equals(hold(new ItemStack(Items.IRON_SWORD))), "sword");
        h.succeed();
    }

    /** The cut goes from the raised wind-up to the strike angle and back, zero at both ends, continuous. */
    @GameTest(template = "empty", batch = "round11546_cut", timeoutTicks = 20)
    public static void cutStrikesFromTheWindup(GameTestHelper h) {
        Object held = Enum.valueOf((Class) Round33GameTests.common(POSE + "$Hold"), "HELD");
        float[] raised = (float[]) pose("base", held, false, 0F, 1F, 1F, 0F, 0F, 0F, 0F, 1F, 0F);
        float strike;
        try {
            strike = Round33GameTests.common(POSE).getField("STRIKE_X").getFloat(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        h.assertTrue(raised[0] < -2.0F, "no wind-up: " + raised[0]);
        float[] last = null;
        float worst = 0, deepest = -10;
        for (int i = 0; i <= 40; i++) {
            float[] p = raised.clone();
            pose("cut", p, false, i / 40F, 1F);
            if (i == 0 || i == 40) h.assertTrue(p[0] == raised[0] && p[1] == raised[1], "not zero at " + i);
            if (last != null) worst = Math.max(worst, Math.abs(p[0] - last[0]));
            deepest = Math.max(deepest, p[0]);
            last = p;
        }
        h.assertTrue(Math.abs(deepest - strike) < 1e-3, "the strike reaches " + deepest + ", not " + strike);
        h.assertTrue(worst < 0.8F, "the cut steps by " + worst + " rad in 1/40 of the swing");
        h.succeed();
    }

    /** Draw, reload and throw: bounded, the reload and the throw end at zero. */
    @GameTest(template = "empty", batch = "round11546_ranged", timeoutTicks = 20)
    public static void rangedMotionsAreBounded(GameTestHelper h) {
        float[] zero = new float[6];
        for (int i = -5; i <= 50; i++) {
            float t = i / 2F;
            float[] d = zero.clone(), r = zero.clone(), s = zero.clone();
            pose("draw", d, false, t / 20F, 1F);
            pose("reload", r, true, t / 22F, 1F);
            pose("spearThrow", s, false, t, 1F);
            for (float[] p : new float[][]{d, r, s}) for (float v : p) h.assertTrue(Float.isFinite(v) && Math.abs(v) < 3, "unbounded at " + t);
            if (t >= 22) for (float v : r) h.assertTrue(Math.abs(v) < 1e-5, "the reload stays at " + t);
            if (t > 10 || t < 0) for (float v : s) h.assertTrue(Math.abs(v) < 1e-5, "the throw outside its time at " + t);
        }
        h.succeed();
    }

    /**
     * After a hit the Wingman pauses ({@code hitPause}), then boosts away. 0.1.11.229 (AN1 part 2): only after the combo's
     * finisher, which winds up first and lands on its fifth tick (the first two cuts chain on without a pause); the dive slam
     * is kept off here.
     */
    @GameTest(template = "empty", batch = "round11546_pause", timeoutTicks = 20)
    public static void meleeHitPausesBeforeTheBoost(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Mob w = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(1.5, 2, 1.5));
        w.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        Zombie zombie = EntityType.ZOMBIE.create(h.getLevel());
        zombie.moveTo(w.getX() + 1.0, w.getY(), w.getZ());
        zombie.setNoAi(true);
        h.getLevel().addFreshEntity(zombie);
        setIntField(w, "slamCooldown", 1000);
        setIntField(w, "comboStep", 2);              // the next strike is the finisher
        setIntField(w, "lastStrikeAt", w.tickCount); // within the combo
        int boosts = (Integer) Round33GameTests.call(w, "boostCount");
        Round33GameTests.call(w, "engageMelee", zombie, owner);   // the wind-up starts
        h.assertTrue(intField(w, "hitPause") == 0 && ((Enum<?>) Round33GameTests.call(w, "action")).name().equals("MELEE_3"),
            "the finisher does not wind up: pause " + intField(w, "hitPause") + ", action " + Round33GameTests.call(w, "action"));
        for (int i = 0; i < 5; i++) Round33GameTests.call(w, "engageMelee", zombie, owner);   // it lands on the fifth tick
        h.assertTrue(intField(w, "hitPause") == 3 && (Integer) Round33GameTests.call(w, "boostCount") == boosts,
            "after the hit: pause " + intField(w, "hitPause") + ", boosts " + Round33GameTests.call(w, "boostCount"));
        for (int i = 0; i < 3; i++) Round33GameTests.call(w, "engageMelee", zombie, owner);
        h.assertTrue(intField(w, "hitPause") == 0 && (Integer) Round33GameTests.call(w, "boostCount") == boosts + 1,
            "after the pause: " + intField(w, "hitPause") + ", boosts " + Round33GameTests.call(w, "boostCount"));
        zombie.discard();
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** 3-hit melee combo alternates forehand/backhand/cleave, shotKick peaks cleanly, and endoskeleton textures exist. */
    @GameTest(template = "empty", batch = "round11546_combo", timeoutTicks = 20)
    public static void meleeComboShotKickAndEndoskeletonTextures(GameTestHelper h) {
        Class<?> pose = Round33GameTests.common(POSE);
        float[] p0 = new float[6], p1 = new float[6], p2 = new float[6];
        float t0 = (Float) Round33GameTests.call(pose, "cutCombo", p0, false, 0.45F, 1.0F, 0, false);
        float t1 = (Float) Round33GameTests.call(pose, "cutCombo", p1, false, 0.45F, 1.0F, 1, false);
        float t2 = (Float) Round33GameTests.call(pose, "cutCombo", p2, false, 0.45F, 1.0F, 2, true);
        h.assertTrue(t0 > 0.2F, "forehand combo step 0 must twist positively, got " + t0);
        h.assertTrue(t1 < -0.2F, "backhand combo step 1 must twist negatively, got " + t1);
        h.assertTrue(t2 > 0.2F && p2[0] > 0.1F, "heavy cleave combo step 2 must slam down, got twist=" + t2 + " rx=" + p2[0]);

        // Zero at boundaries
        float[] zero = new float[6];
        float tz = (Float) Round33GameTests.call(pose, "cutCombo", zero, false, 0.0F, 1.0F, 0, false);
        h.assertTrue(tz == 0F && zero[0] == 0F, "cutCombo must be zero at a=0");

        // Per-shot kick peaks inside 0..3.5 ticks and settles to 0 outside
        float[] kick = new float[6];
        Round33GameTests.call(pose, "shotKick", kick, false, 0.8F);
        h.assertTrue(kick[0] < -0.20F && kick[3] < -0.10F, "shotKick must kick both arms up at peak");
        float[] settled = new float[6];
        Round33GameTests.call(pose, "shotKick", settled, false, 4.0F);
        h.assertTrue(settled[0] == 0F && settled[3] == 0F, "shotKick must settle to 0 after 3.5 ticks");

        // Both endoskeleton textures must be packaged in mod resources
        var modFile = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
        h.assertTrue(java.nio.file.Files.exists(modFile.findResource("assets/mio_icif/textures/entity/wingman_endoskeleton.png")),
            "missing wingman_endoskeleton.png");
        h.assertTrue(java.nio.file.Files.exists(modFile.findResource("assets/mio_icif/textures/entity/wingman_endoskeleton_glow.png")),
            "missing wingman_endoskeleton_glow.png");
        h.succeed();
    }

    private static void setIntField(Object o, String name, int v) {
        try {
            java.lang.reflect.Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.setInt(o, v);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }

    private static int intField(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }
}
