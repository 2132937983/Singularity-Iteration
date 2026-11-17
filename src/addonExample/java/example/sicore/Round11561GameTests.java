package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.61 (hit feel and constructivist FX phase Q0): a Wingman's swing runs out (vanilla advanced it only for monsters
 * and players: the first swing never ended and every later one was dropped), so a second strike swings again; the weapon
 * weight classes behind the strike sounds.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11561GameTests {
    private Round11561GameTests() {}

    /** The swing ends after its duration and the next one starts anew. */
    @GameTest(template = "empty", batch = "round11561_swing", timeoutTicks = 40)
    public static void swingRunsOutAndSwingsAgain(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Mob w = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(1.5, 3, 1.5));
        w.swing(InteractionHand.MAIN_HAND);
        h.assertTrue(w.swinging, "the swing did not start");
        for (int i = 0; i < 10; i++) w.aiStep();
        h.assertTrue(!w.swinging, "the swing never ends (swing time " + w.swingTime + ")");
        w.swing(InteractionHand.MAIN_HAND);
        h.assertTrue(w.swinging && w.swingTime <= 0, "the second swing was dropped");
        for (int i = 0; i < 3; i++) w.aiStep();   // a swing starts at -1 (vanilla)
        h.assertTrue(w.swinging && w.swingTime == 2, "the second swing does not run: " + w.swingTime);
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** Axes and maces strike heavy, swords and tridents as blades, the rest light. */
    @GameTest(template = "empty", batch = "round11561_heft", timeoutTicks = 20)
    public static void weaponWeightClasses(GameTestHelper h) {
        Class<?> entity = Round33GameTests.common("armory.WingmanEntity");
        h.assertTrue("HEAVY".equals(((Enum<?>) Round33GameTests.call(entity, "heftOf", new ItemStack(Items.DIAMOND_AXE))).name()), "axe");
        h.assertTrue("HEAVY".equals(((Enum<?>) Round33GameTests.call(entity, "heftOf", new ItemStack(Items.MACE))).name()), "mace");
        h.assertTrue("BLADE".equals(((Enum<?>) Round33GameTests.call(entity, "heftOf", new ItemStack(Items.IRON_SWORD))).name()), "sword");
        h.assertTrue("LIGHT".equals(((Enum<?>) Round33GameTests.call(entity, "heftOf", new ItemStack(Items.STICK))).name()), "stick");
        h.succeed();
    }
}
