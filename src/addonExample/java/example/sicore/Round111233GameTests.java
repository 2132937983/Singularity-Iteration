package example.sicore;

import com.google.gson.JsonObject;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.233 (SlashBlade collab B1): the resources of the collab blades, on {@code BladeResources} and {@code BladeCore}
 * alone (SlashBlade is not in the GameTests). The nano edge heats with each slash art, overheats and refuses until it has
 * vented to the resume level; the quantum blade's slash art is paid with coherence when the suit has it, refused when the
 * suit is short, and paid with EU without a quantum core; the singularity takes mass from kills, its containment costs EU
 * every tick and fails (the mass leaks) when the EU runs out; a blade takes FE (4 FE per EU) up to its capacity. The common
 * classes are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111233GameTests {
    private Round111233GameTests() {}

    private static Class<?> res() { return Round33GameTests.common("armory.blade.BladeResources"); }

    private static Object kind(String name) { return Round1102GameTests.constant("armory.blade.CollabBlade", name); }

    private static Object coherence(String name) { return Round1102GameTests.constant("armory.blade.BladeResources$Coherence", name); }

    private static Object fresh(String kind) { return Round33GameTests.call(Round33GameTests.common("armory.blade.BladeCore"), "fresh", kind(kind)); }

    private static Object withEu(Object core, long eu) { return Round33GameTests.call(core, "withEu", eu); }

    private static long eu(Object core) { return (Long) Round33GameTests.call(core, "eu"); }

    private static float heat(Object core) { return (Float) Round33GameTests.call(core, "heat"); }

    private static float mass(Object core) { return (Float) Round33GameTests.call(core, "mass"); }

    private static boolean flag(Object core, String name) { return (Boolean) Round33GameTests.call(core, name); }

    private static long longField(String kind, String field) {
        try {
            return (Long) Round33GameTests.common("armory.blade.CollabBlade").getField(field).get(kind(kind));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(field, e);
        }
    }

    /** A slash art: [refusal name, core after]. */
    private static Object[] art(Object core, String kind, String coherence) {
        Object spend = Round33GameTests.call(res(), "slashArt", core, kind(kind), coherence(coherence));
        return new Object[]{((Enum<?>) Round33GameTests.call(spend, "refusal")).name(), Round33GameTests.call(spend, "core")};
    }

    private static Object tick(Object core, String kind, int ticks) {
        for (int i = 0; i < ticks; i++) core = Round33GameTests.call(res(), "tick", core, kind(kind));
        return core;
    }

    @GameTest(template = "empty", batch = "round111233_nano", timeoutTicks = 20)
    public static void theNanoEdgeOverheatsAndVents(GameTestHelper h) {
        Object c = withEu(fresh("NANO"), 200_000);
        Object[] a = art(c, "NANO", "NO_CORE");
        h.assertTrue("OK".equals(a[0]), "a charged, cool nano blade refuses its slash art: " + a[0]);
        h.assertTrue(eu(a[1]) == 200_000 - longField("NANO", "saEu") && heat(a[1]) == 35, "the slash art took " + (200_000 - eu(a[1])) + " EU and left heat " + heat(a[1]));
        c = a[1];
        for (int i = 0; i < 2; i++) c = art(c, "NANO", "NO_CORE")[1];
        h.assertTrue(flag(c, "overheated"), "three slash arts in a row do not overheat the edge (heat " + heat(c) + ")");
        h.assertTrue("OVERHEATED".equals(art(c, "NANO", "NO_CORE")[0]), "an overheated edge takes a fourth slash art");
        // 0.3 heat a tick: from 100 to 40 takes 200 ticks
        Object cooled = tick(c, "NANO", 190);
        h.assertTrue(flag(cooled, "overheated"), "the edge resumes before it has vented to 40 (heat " + heat(cooled) + ")");
        cooled = tick(cooled, "NANO", 12);
        h.assertTrue(!flag(cooled, "overheated") && heat(cooled) <= 40, "the edge is still overheated at heat " + heat(cooled));
        h.assertTrue("NO_EU".equals(art(withEu(fresh("NANO"), 100), "NANO", "NO_CORE")[0]), "an empty nano blade fires its slash art");
        Object hit = Round33GameTests.call(res(), "hit", withEu(fresh("NANO"), 1000), kind("NANO"));
        h.assertTrue(heat(hit) == 3 && eu(hit) == 950, "a hit heats the edge by " + heat(hit) + " and takes " + (1000 - eu(hit)) + " EU");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111233_quantum", timeoutTicks = 20)
    public static void theQuantumCollapseIsPaidWithCoherenceOrEu(GameTestHelper h) {
        Object c = withEu(fresh("QUANTUM"), 50_000);
        Object[] paid = art(c, "QUANTUM", "PAID");
        h.assertTrue("OK".equals(paid[0]) && eu(paid[1]) == 50_000, "a slash art the suit paid for takes EU too: " + paid[0] + ", " + eu(paid[1]));
        h.assertTrue("NO_COHERENCE".equals(art(c, "QUANTUM", "SHORT")[0]), "a suit short of coherence still collapses");
        Object[] forced = art(c, "QUANTUM", "NO_CORE");
        h.assertTrue("OK".equals(forced[0]) && eu(forced[1]) == 50_000 - longField("QUANTUM", "saEu"), "without a quantum core the collapse is not paid with EU");
        h.assertTrue("NO_EU".equals(art(fresh("QUANTUM"), "QUANTUM", "NO_CORE")[0]), "an empty quantum blade collapses without a core");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111233_singularity", timeoutTicks = 20)
    public static void theSingularityFeedsOnKillsAndNeedsItsContainment(GameTestHelper h) {
        Object c = withEu(fresh("SINGULARITY"), 1_000_000);
        h.assertTrue("NO_MASS".equals(art(c, "SINGULARITY", "NO_CORE")[0]), "a singularity without mass fires its slash art");
        for (int i = 0; i < 9; i++) c = Round33GameTests.call(res(), "kill", c, kind("SINGULARITY"), false);
        c = Round33GameTests.call(res(), "kill", c, kind("SINGULARITY"), true);
        h.assertTrue(mass(c) == 14, "nine kills and a boss give mass " + mass(c) + " instead of 14");
        Object[] a = art(c, "SINGULARITY", "NO_CORE");
        h.assertTrue("OK".equals(a[0]) && mass(a[1]) == 4 && eu(a[1]) == 1_000_000 - longField("SINGULARITY", "saEu"),
            "the event horizon does not take 10 mass and its EU: " + a[0] + ", mass " + mass(a[1]));
        // the containment: 8 EU a tick while there is mass
        Object held = tick(a[1], "SINGULARITY", 100);
        h.assertTrue(eu(held) == eu(a[1]) - 800 && !flag(held, "unstable"), "100 ticks of containment took " + (eu(a[1]) - eu(held)) + " EU");
        Object empty = tick(withEu(held, 20), "SINGULARITY", 2);
        h.assertTrue(eu(empty) == 4, "the containment did not take 8 EU a tick from the last 20 (" + eu(empty) + " left)");
        empty = tick(empty, "SINGULARITY", 41);
        h.assertTrue(flag(empty, "unstable") && mass(empty) < 4 && mass(empty) > 2.9F,
            "without EU the containment holds or the mass does not leak (unstable " + flag(empty, "unstable") + ", mass " + mass(empty) + ")");
        Object again = tick(withEu(empty, 1000), "SINGULARITY", 1);
        h.assertTrue(!flag(again, "unstable"), "recharged, the containment does not hold again");
        Object massless = tick(withEu(fresh("SINGULARITY"), 1000), "SINGULARITY", 100);
        h.assertTrue(eu(massless) == 1000, "an empty singularity costs containment EU");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111233_energy", timeoutTicks = 20)
    public static void aBladeTakesFeUpToItsCapacity(GameTestHelper h) {
        ItemStack stack = new ItemStack(Items.IRON_SWORD);
        Round33GameTests.call(Round33GameTests.common("armory.blade.CollabBlade"), "set", stack, fresh("NANO"));
        Object fe = Round33GameTests.call(Round33GameTests.common("armory.blade.BladeEnergyStorage"), "of", stack);
        h.assertTrue(fe != null, "a stack with a nano blade core has no energy storage");
        int took = (Integer) Round33GameTests.call(fe, "receiveEnergy", 4002, false);
        h.assertTrue(took == 4000, "4002 FE moved " + took + " FE (whole EU only)");
        Object core = Round33GameTests.call(Round33GameTests.common("armory.blade.CollabBlade"), "core", stack);
        h.assertTrue(eu(core) == 1000, "4000 FE gave " + eu(core) + " EU instead of 1000");
        int max = (Integer) Round33GameTests.call(fe, "getMaxEnergyStored");
        h.assertTrue(max == 800_000, "the nano blade holds " + max + " FE instead of 800000");
        Round33GameTests.call(fe, "receiveEnergy", Integer.MAX_VALUE, false);
        h.assertTrue(eu(Round33GameTests.call(Round33GameTests.common("armory.blade.CollabBlade"), "core", stack)) == 200_000, "the blade took more than its capacity");
        h.assertTrue(Round33GameTests.call(Round33GameTests.common("armory.blade.BladeEnergyStorage"), "of", new ItemStack(Items.IRON_SWORD)) == null,
            "a plain sword has a blade energy storage");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111233_lang", timeoutTicks = 20)
    public static void theRefusalsAndReadoutsAreNamed(GameTestHelper h) {
        String[] keys = {"message.mio_icif.blade.no_eu", "message.mio_icif.blade.overheated", "message.mio_icif.blade.no_mass",
            "message.mio_icif.blade.no_coherence", "hud.mio_icif.blade.heat", "hud.mio_icif.blade.overheated", "hud.mio_icif.blade.coherence",
            "hud.mio_icif.blade.decoherent", "hud.mio_icif.blade.no_core", "hud.mio_icif.blade.mass", "hud.mio_icif.blade.unstable",
            "hud.mio_icif.blade.contained"};
        for (String lang : new String[]{"zh_cn", "en_us", "ja_jp", "ru_ru"}) {
            JsonObject l = Round111231GameTests.json("assets/mio_icif/lang/" + lang + ".json");
            for (String k : keys) h.assertTrue(l.has(k), lang + " has no " + k);
        }
        h.succeed();
    }
}
