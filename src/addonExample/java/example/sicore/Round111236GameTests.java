package example.sicore;

import com.google.gson.JsonObject;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.236 (SlashBlade collab B4): the singularity blade's event horizon and tidal shear without SlashBlade or a client:
 * the horizon opens over 6 ticks, holds until the burst and closes over 4; the Hawking flash starts full at the burst and
 * fades over 10; the effect ends after both; the tidal shear is mass / 20, at most 5. The blade names its own slash art and
 * special effect, named in every language; the lens shader is shipped.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111236GameTests {
    private Round111236GameTests() {}

    private static Class<?> fx() { return Round33GameTests.common("armory.blade.SingularityFxPacket"); }

    private static float f(String name, float age) { return (Float) Round33GameTests.call(fx(), name, age, 21); }

    @GameTest(template = "empty", batch = "round111236_horizon", timeoutTicks = 20)
    public static void theHorizonOpensHoldsAndBursts(GameTestHelper h) {
        h.assertTrue(f("strength", 0) == 0 && Math.abs(f("strength", 3) - 0.5F) < 1.0E-4 && f("strength", 6) == 1, "the horizon does not open over 6 ticks");
        h.assertTrue(f("strength", 20) == 1, "the horizon does not hold until the burst");
        h.assertTrue(Math.abs(f("strength", 23) - 0.5F) < 1.0E-4 && f("strength", 25) == 0, "the horizon does not close over 4 ticks after the burst");
        h.assertTrue(f("burst", 20) == 0 && f("burst", 21) == 1 && Math.abs(f("burst", 26) - 0.5F) < 1.0E-4 && f("burst", 32) == 0, "the Hawking flash is not full at the burst, fading over 10 ticks");
        h.assertTrue((Boolean) Round33GameTests.call(fx(), "alive", 31F, 21) && !(Boolean) Round33GameTests.call(fx(), "alive", 31.5F, 21), "the effect does not end 10 ticks after the burst");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111236_shear", timeoutTicks = 20)
    public static void theTidalShearGrowsWithMass(GameTestHelper h) {
        Object kind = Round1102GameTests.constant("armory.blade.CollabBlade", "SINGULARITY");
        Object c = Round33GameTests.call(Round33GameTests.common("armory.blade.BladeCore"), "fresh", kind);
        Class<?> res = Round33GameTests.common("armory.blade.BladeResources");
        for (float[] m : new float[][]{{0, 0}, {40, 2}, {100, 5}}) {
            Object with = Round33GameTests.call(c, "withMass", m[0], false);
            float got = (Float) Round33GameTests.call(res, "tidalDamage", with);
            h.assertTrue(got == m[1], "mass " + m[0] + " shears " + got + " instead of " + m[1]);
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111236_named", timeoutTicks = 20)
    public static void theSingularityBladeNamesItsArtAndEffect(GameTestHelper h) {
        JsonObject p = Round111231GameTests.json("data/mio_icif/slashblade/named_blades/singularity_blade.json").getAsJsonObject("properties");
        h.assertTrue("mio_icif:event_horizon".equals(p.get("slash_art").getAsString()), "the singularity blade's slash art is " + p.get("slash_art"));
        h.assertTrue(p.getAsJsonArray("special_effects").toString().contains("mio_icif:tidal_shear"), "the singularity blade has no tidal shear");
        for (String lang : new String[]{"zh_cn", "en_us", "ja_jp", "ru_ru"}) {
            JsonObject l = Round111231GameTests.json("assets/mio_icif/lang/" + lang + ".json");
            for (String k : new String[]{"slash_art.mio_icif.event_horizon", "slash_art.mio_icif.event_horizon.desc", "se.mio_icif.tidal_shear",
                "se.mio_icif.tidal_shear.desc"})
                h.assertTrue(l.has(k), lang + " has no " + k);
        }
        JsonObject post = Round111231GameTests.json("assets/mio_icif/shaders/post/singularity_lens.json");
        h.assertTrue(post.toString().contains("mio_icif:singularity_lens"), "the lens post chain does not run the lens program");
        Round111231GameTests.json("assets/mio_icif/shaders/program/singularity_lens.json");
        h.succeed();
    }
}
