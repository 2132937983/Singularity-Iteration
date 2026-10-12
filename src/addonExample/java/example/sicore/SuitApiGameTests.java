package example.sicore;

import com.miophas.singularity_iteration.core.api.item.BallisticProfile;
import com.miophas.singularity_iteration.core.api.item.BallisticProfiles;
import com.miophas.singularity_iteration.core.api.suit.SuitModuleRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 0.1.8.0 public API contract: suit unit registry and ballistics lookup, seen from an addon. */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class SuitApiGameTests {
    private SuitApiGameTests() {}

    @GameTest(batch = "round180_api", template = "empty", timeoutTicks = 20)
    public static void suitUnitIsRegistered(GameTestHelper h) {
        var entry = SuitModuleRegistry.get(ExampleSonarUnit.ID);
        h.assertTrue(entry != null, "example unit not registered");
        h.assertTrue(entry.spec().featureKey().equals("fcs_si_core_example_sonar"), "feature key " + entry.spec().featureKey());
        h.assertTrue(entry.spec().nameKey().equals("module.si_core_example.sonar"), "name key " + entry.spec().nameKey());
        h.assertTrue(!SuitModuleRegistry.isActive(h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL), ExampleSonarUnit.ID),
            "a player without a suit must not have the unit active");
        h.succeed();
    }

    @GameTest(batch = "round180_api", template = "empty", timeoutTicks = 20)
    public static void ballisticProviderIsUsed(GameTestHelper h) {
        BallisticProfiles.register((stack, shooter, use) -> stack.is(net.minecraft.world.item.Items.STICK)
            ? BallisticProfile.straight("STICK", 2.0, 0.99, 0.8, true) : null);
        var p = BallisticProfiles.find(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK),
            h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL), 0);
        h.assertTrue(p != null && p.speed() == 2.0 && p.gravity() == 0.0, "provider profile not returned");
        h.assertTrue(Math.abs(BallisticProfile.drawPower(20, 20) - 1.0) < 1.0E-9, "full draw power must be 1");
        h.succeed();
    }
}
