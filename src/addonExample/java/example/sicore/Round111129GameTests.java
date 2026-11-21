package example.sicore;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.129 (armour and flight overhaul E6): the quantum chestplate's jetpack sound is the electromagnetic drive's field
 * hum (a new thruster profile, placeholder sound on the vanilla beacon hum) with its subtitle in every language; the manuals
 * describe the electromagnetic flight and the Wingmen's. (Items and sounds through the registries.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111129GameTests {
    private Round111129GameTests() {}

    @GameTest(template = "empty", timeoutTicks = 5)
    public static void quantumDriveHumsElectromagnetically(GameTestHelper h) {
        var chest = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "armor/item_armor_quantum_chestplate"));
        h.assertTrue(chest instanceof com.miophas.singularity_iteration.core.api.item.IJetpackItem j
            && "electromagnetic".equals(j.getThrusterSound()), "the quantum chestplate's thruster sound");
        h.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.fromNamespaceAndPath("mio_icif", "suit.em_hum")), "the hum is not registered");
        String sounds = Round33GameTests.resource("/assets/mio_icif/sounds.json");
        h.assertTrue(sounds != null && sounds.contains("\"suit.em_hum\"") && sounds.contains("minecraft:block/beacon/ambient"), "sounds.json has no hum");
        for (String lang : new String[]{"en_us", "zh_cn", "ja_jp", "ru_ru"}) {
            String json = Round33GameTests.resource("/assets/mio_icif/lang/" + lang + ".json");
            h.assertTrue(json != null && json.contains("\"mio_icif.subtitle.suit.em_hum\""), lang + " has no subtitle for the hum");
        }
        for (String page : new String[]{"", "_zh_cn/", "_ja_jp/"}) {
            String md = Round33GameTests.resource("/assets/mio_icif/guides/mio_icif/manual/" + page + "suit/maneuver_mode.md");
            h.assertTrue(md != null && md.contains("Mach 0.95") && md.contains("Mach 1.3"), page + " maneuver page lacks the electromagnetic flight");
            String armory = Round33GameTests.resource("/assets/mio_icif/guides/mio_icif/manual/" + page + "machines/producer_block_armory.md");
            h.assertTrue(armory != null && armory.contains("0.1.11.129"), page + " armory page lacks the Wingmen's flight");
        }
        h.succeed();
    }
}
