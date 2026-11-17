package example.sicore;

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
 * 0.1.11.60 (gaze and Iron Man phase P8, decisions A / A): remote piloting on the server. The piloted Wingman takes its
 * owner's controls (flight along the look, sprint, a melee strike at the creature in the crosshair); the other Wingmen
 * escort the body and get their modes back; a hurt body or a Wingman out of range ends the flight; the controls from the
 * client are cleaned (no NaN, movement within -1..1).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11560GameTests {
    private Round11560GameTests() {}

    private static final Class<?> PILOT = Round33GameTests.common("armory.WingmanPilot");

    private static Object input(float fwd, float strafe, float up, boolean sprint, float yaw, float pitch, boolean attack) {
        try {
            return Round33GameTests.common("armory.WingmanPilot$Input").getConstructors()[0]
                .newInstance(fwd, strafe, up, sprint, yaw, pitch, attack, false);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Input", e);
        }
    }

    private static String mode(Object w) { return ((Enum<?>) Round33GameTests.call(w, "mode")).name(); }

    /** The flight: along the look, faster with sprint; the other Wingman escorts the body and is given its mode back. */
    @GameTest(template = "empty", batch = "round11560_flight", timeoutTicks = 40)
    public static void pilotFliesAndTheSquadGuardsTheBody(GameTestHelper h) {
        ServerPlayer owner = Round1102GameTests.commander(h);
        Mob w = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(1.5, 3, 1.5));
        Mob other = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(3.5, 3, 1.5));
        h.assertTrue(Round33GameTests.call(PILOT, "start", owner, w) == null, "the flight did not start");
        h.assertTrue(owner.getUUID().equals(Round33GameTests.call(w, "pilot")), "the Wingman has no pilot");
        h.assertTrue("ESCORT".equals(mode(other)), "the other Wingman does not guard the body");
        Round33GameTests.call(PILOT, "input", owner, input(1, 0, 0, false, 0, 0, false));   // ahead: south (+z)
        for (int i = 0; i < 10; i++) Round33GameTests.call(w, "tickPiloted");
        Vec3 v = w.getDeltaMovement();
        h.assertTrue(v.z > 0.7 && Math.abs(v.x) < 0.05, "not flying ahead: " + v);
        Round33GameTests.call(PILOT, "input", owner, input(1, 0, 0, true, 0, 0, false));
        for (int i = 0; i < 10; i++) Round33GameTests.call(w, "tickPiloted");
        h.assertTrue(w.getDeltaMovement().z > 1.4, "sprint is not faster: " + w.getDeltaMovement());
        Round33GameTests.call(PILOT, "stop", owner, null);
        h.assertTrue(Round33GameTests.call(w, "pilot") == null && "OFFENSIVE".equals(mode(other)), "the stop did not restore: " + mode(other));
        w.discard();
        other.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** Attack strikes the creature in the crosshair. */
    @GameTest(template = "empty", batch = "round11560_attack", timeoutTicks = 40)
    public static void attackStrikesTheCrosshair(GameTestHelper h) {
        ServerPlayer owner = Round1102GameTests.commander(h);
        Mob w = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(1.5, 2, 1.5));
        w.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        Zombie zombie = EntityType.ZOMBIE.create(h.getLevel());
        zombie.moveTo(w.getX(), w.getY(), w.getZ() + 2.0);
        zombie.setNoAi(true);
        h.getLevel().addFreshEntity(zombie);
        float health = zombie.getHealth();
        Round33GameTests.call(PILOT, "start", owner, w);
        Round33GameTests.call(PILOT, "input", owner, input(0, 0, 0, false, 0, 10, true));
        Round33GameTests.call(w, "tickPiloted");
        h.assertTrue(zombie.getHealth() < health, "the zombie in the crosshair was not hit");
        Round33GameTests.call(PILOT, "stop", owner, null);
        zombie.discard();
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** A hurt body ends the flight; so does a Wingman out of range; bad controls are cleaned. */
    @GameTest(template = "empty", batch = "round11560_safety", timeoutTicks = 40)
    public static void hurtBodyAndRangeEndTheFlight(GameTestHelper h) {
        ServerPlayer owner = Round1102GameTests.commander(h);
        Mob w = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(1.5, 3, 1.5));
        Round33GameTests.call(PILOT, "start", owner, w);
        owner.hurt(h.getLevel().damageSources().generic(), 1);
        h.assertTrue(!(Boolean) Round33GameTests.call(PILOT, "piloting", owner) && Round33GameTests.call(w, "pilot") == null,
            "the flight goes on with a hurt body");
        Round33GameTests.call(PILOT, "start", owner, w);
        w.moveTo(w.getX() + 200, w.getY(), w.getZ());
        h.assertTrue(Round33GameTests.call(PILOT, "broken", owner, w) != null, "a Wingman 200 blocks away keeps the link");
        Round33GameTests.call(PILOT, "stop", owner, null);
        Object dirty = input(Float.NaN, 9, -9, false, Float.POSITIVE_INFINITY, 400, false);
        Object clean = Round33GameTests.call(dirty, "clean");
        h.assertTrue((Float) Round33GameTests.call(clean, "forward") == 0 && (Float) Round33GameTests.call(clean, "strafe") == 1
            && (Float) Round33GameTests.call(clean, "vertical") == -1 && (Float) Round33GameTests.call(clean, "yaw") == 0
            && (Float) Round33GameTests.call(clean, "pitch") == 90, "the controls were not cleaned");
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }
}
