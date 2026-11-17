package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.56 (gaze and Iron Man phase P4, decisions A / A / A): the dangers (a lethal fall, near death without armour),
 * the choice of the nearest Wingman (one at a time), the catch (no fall, no fall distance, Slow Falling) followed by a suit
 * swap when the owner wears less armour, and the suit swap at once near death.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11556GameTests {
    private Round11556GameTests() {}

    private static final Class<?> RESCUE = Round33GameTests.common("armory.WingmanRescue");

    private static String danger(ServerPlayer p) { return ((Enum<?>) Round33GameTests.call(RESCUE, "danger", p)).name(); }

    private static void fallFrom(GameTestHelper h, ServerPlayer p, double height) {
        p.moveTo(h.absoluteVec(new Vec3(1.5, height, 1.5)));
        p.setOnGround(false);
        p.setDeltaMovement(0, -1.2, 0);
        p.fallDistance = 4;
    }

    /** The fall damage rule and the two dangers. */
    @GameTest(template = "empty", batch = "round11556_danger", timeoutTicks = 20)
    public static void dangersAreRecognised(GameTestHelper h) {
        h.assertTrue((Float) Round33GameTests.call(RESCUE, "fallDamage", 23.0, 3.0, 1.0) == 20F
            && (Float) Round33GameTests.call(RESCUE, "fallDamage", 2.0, 3.0, 1.0) == 0F, "the fall damage rule");
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        fallFrom(h, p, 40);
        h.assertTrue("FALL".equals(danger(p)), "a 40-block fall is not lethal: " + danger(p));
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100));
        h.assertTrue("NONE".equals(danger(p)), "Slow Falling still counts as a fall");
        p.removeAllEffects();
        fallFrom(h, p, 6);
        h.assertTrue(!"FALL".equals(danger(p)), "a short fall counts as lethal");
        p.moveTo(h.absoluteVec(new Vec3(1.5, 2, 1.5)));
        p.setOnGround(true);
        p.setDeltaMovement(Vec3.ZERO);
        p.setHealth(4);
        h.assertTrue("LOW_HEALTH".equals(danger(p)), "near death without armour: " + danger(p));
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        h.assertTrue("NONE".equals(danger(p)), "near death in armour counts");
        Round33GameTests.remove(p);
        h.succeed();
    }

    private static void suit(Mob w) {
        w.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        w.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        w.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
    }

    /** In a fall the nearest Wingman goes (only one), catches the owner, holds them, then brings the suit. */
    @GameTest(template = "empty", batch = "round11556_catch", timeoutTicks = 40)
    public static void nearestWingmanCatchesAndSuitsUp(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        fallFrom(h, owner, 40);
        Mob near = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(2.5, 40, 1.5));
        Mob far = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(1.5, 30, 6.5));
        suit(near);
        suit(far);
        Object sent = Round33GameTests.call(RESCUE, "check", owner);
        h.assertTrue(sent == near, "not the nearest Wingman: " + sent);
        h.assertTrue(Round33GameTests.call(RESCUE, "check", owner) == null && !(Boolean) Round33GameTests.call(far, "rescuing"),
            "a second Wingman was sent");
        Round33GameTests.call(near, "tickRescue", owner, true);   // within reach: the catch
        h.assertTrue(owner.hasEffect(MobEffects.SLOW_FALLING) && owner.fallDistance == 0 && owner.getDeltaMovement().y > -0.1,
            "not caught: " + owner.getDeltaMovement() + ", fall " + owner.fallDistance);
        for (int i = 0; i < 20 && (Boolean) Round33GameTests.call(near, "rescuing"); i++) Round33GameTests.call(near, "tickRescue", owner, true);
        h.assertTrue(!(Boolean) Round33GameTests.call(near, "rescuing"), "the rescue did not end");
        h.assertTrue((Byte) Round33GameTests.call(near, "swapPhase") == 1, "no suit swap for an owner without armour");
        near.discard();
        far.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** Near death without armour: a Wingman with a suit comes to swap at once; a Wingman without one does not go. */
    @GameTest(template = "empty", batch = "round11556_low", timeoutTicks = 40)
    public static void nearDeathBringsTheSuit(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        owner.setHealth(3);
        Mob bare = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(1.5, 2, 2.5));
        h.assertTrue(Round33GameTests.call(RESCUE, "check", owner) == null, "a Wingman without a suit was sent");
        suit(bare);
        h.assertTrue(Round33GameTests.call(RESCUE, "check", owner) == bare && (Byte) Round33GameTests.call(bare, "swapPhase") == 1,
            "no suit swap near death");
        h.assertTrue(Round33GameTests.call(RESCUE, "check", owner) == null, "sent again during the swap");
        bare.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }
}
