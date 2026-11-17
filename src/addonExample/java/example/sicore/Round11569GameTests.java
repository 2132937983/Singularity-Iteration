package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.69 (quantum flight phase R3, decision four B): the shockwave of a hard landing pushes, never hurts. Only a hard
 * touchdown out of flight counts; the push is strongest at the centre and gone past the reach; creatures near the lander
 * are thrown away from it with their health untouched, while players, armour stands, far creatures and the lander's own
 * Wingmen stay put.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11569GameTests {
    private Round11569GameTests() {}

    private static Class<?> wave() { return Round33GameTests.common("suit.LandingShockwave"); }

    private static float touchdown(int since, boolean wasGround, boolean ground, double fall) {
        return (Float) Round33GameTests.call(wave(), "touchdown", since, wasGround, ground, fall);
    }

    private static float[] shove(float d, float s) {
        return (float[]) Round33GameTests.call(Round33GameTests.common("suit.FlightImpact"), "shove", d, s);
    }

    @GameTest(template = "empty", batch = "round11569_rule", timeoutTicks = 20)
    public static void onlyHardTouchdownsAndFalloff(GameTestHelper h) {
        h.assertTrue(touchdown(1, false, true, 1.0) > 0 && touchdown(3, false, true, 1.0) > 0, "a hard touchdown is missed");
        h.assertTrue(touchdown(1, false, true, 0.2) == 0, "a soft touchdown counts");
        h.assertTrue(touchdown(4, false, true, 1.0) == 0, "a fall long after the flight counts");
        h.assertTrue(touchdown(1, false, false, 1.0) == 0, "a dive in the air counts");
        h.assertTrue(touchdown(1, true, true, 1.0) == 0, "standing on the ground counts again");
        h.assertTrue(touchdown(1, false, true, -1.0) == 0, "a rise counts");
        float[] near = shove(0.5F, 1F), mid = shove(3F, 1F), weakMid = shove(3F, 0F), far = shove(5.5F, 1F);
        h.assertTrue(near[0] > mid[0] && mid[0] > 0 && near[1] > 0, "the push does not fall off");
        h.assertTrue(weakMid[0] == 0 && far[0] == 0 && far[1] == 0, "the push reaches past its radius");
        h.assertTrue(shove(Float.NaN, 1F)[0] > 0 || shove(Float.NaN, 1F)[0] == 0, "NaN");
        h.succeed();
    }

    private static Mob mob(GameTestHelper h, EntityType<? extends Mob> type, double x, double z) {
        Mob m = h.spawnWithNoFreeWill(type, new Vec3(x, 2, z));
        return m;
    }

    @GameTest(template = "empty", batch = "round11569_wave", timeoutTicks = 40)
    public static void shockwavePushesWithoutHarm(GameTestHelper h) {
        ServerPlayer lander = Round33GameTests.survivalPlayer(h);
        Vec3 at = h.absoluteVec(new Vec3(4.5, 2, 4.5));
        lander.teleportTo(at.x, at.y, at.z);
        Mob pig = mob(h, EntityType.PIG, 6.5, 4.5), cow = mob(h, EntityType.COW, 4.5, 2.5), far = mob(h, EntityType.SHEEP, 4.5, 12.5);
        ArmorStand stand = h.spawn(EntityType.ARMOR_STAND, new Vec3(3.5, 2, 4.5));
        ServerPlayer other = Round33GameTests.survivalPlayer(h);
        other.teleportTo(at.x - 1, at.y, at.z + 1);
        LivingEntity own = (LivingEntity) Round1102GameTests.wingman(h, lander, new Vec3(4.5, 2, 6.5));
        own.teleportTo(at.x, at.y, at.z + 2);
        for (LivingEntity e : new LivingEntity[]{pig, cow, far, stand, own}) e.setDeltaMovement(Vec3.ZERO);
        other.setDeltaMovement(Vec3.ZERO);
        float pigHealth = pig.getHealth(), cowHealth = cow.getHealth();
        int moved = (Integer) Round33GameTests.call(wave(), "shockwave", lander, 1F);
        h.assertTrue(moved == 2, "pushed " + moved + " creatures, not the two near animals");
        h.assertTrue(pig.getDeltaMovement().x > 0.2 && pig.getDeltaMovement().y > 0, "the pig east is not thrown east: " + pig.getDeltaMovement());
        h.assertTrue(cow.getDeltaMovement().z < -0.2, "the cow north is not thrown north: " + cow.getDeltaMovement());
        h.assertTrue(pig.getHealth() == pigHealth && cow.getHealth() == cowHealth && pig.hurtTime == 0, "the shockwave hurts");
        h.assertTrue(far.getDeltaMovement().lengthSqr() == 0 && stand.getDeltaMovement().lengthSqr() == 0
            && own.getDeltaMovement().lengthSqr() == 0 && other.getDeltaMovement().lengthSqr() == 0, "something out of reach or spared moved");
        own.discard();
        Round33GameTests.remove(other);
        Round33GameTests.remove(lander);
        h.succeed();
    }
}
