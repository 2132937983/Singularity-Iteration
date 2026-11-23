package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.131: the quantum generator screen in the vanilla-frame generator layout: the player inventory at (44, 162) and
 * the hotbar at y 220 (248 x 244; 0.1.11.181: 176 x 244, the inventory at x 8), the EU made in the last tick synced as a sixth value, the screen's texts in every
 * language; 0.1.11.130's background section in the three manuals. (Classes by name.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111131GameTests {
    private Round111131GameTests() {}

    private static final String COMMON = "com.miophas.singularity_iteration" + ".common.";

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void quantumGeneratorScreenData(GameTestHelper h) throws Exception {
        BlockPos at = new BlockPos(1, 2, 1);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "generator/block_quantum_generator")));
        BlockEntity be = h.getBlockEntity(at);
        Class<?> menuClass = Class.forName(COMMON + "menu.generator.QuantumGeneratorMenu");
        Inventory inv = h.makeMockPlayer(GameType.SURVIVAL).getInventory();
        AbstractContainerMenu menu = (AbstractContainerMenu) menuClass
            .getConstructor(int.class, Inventory.class, be.getClass()).newInstance(0, inv, be);
        h.assertTrue(menu.slots.size() == 36, "slots " + menu.slots.size());
        // 0.1.11.181: x 8 (the screen is the vanilla 176 wide; it was 248 with the inventory at x 44)
        h.assertTrue(menu.slots.get(0).x == 8 && menu.slots.get(0).y == 162, "inventory at " + menu.slots.get(0).x + ", " + menu.slots.get(0).y);
        h.assertTrue(menu.slots.get(27).x == 8 && menu.slots.get(27).y == 220, "hotbar at " + menu.slots.get(27).x + ", " + menu.slots.get(27).y);
        h.succeedWhen(() -> {
            long made = (long) Round33GameTests.call(be, "getLastGenerated");
            h.assertTrue(made > 0, "nothing made yet");
            menu.broadcastChanges();
            int synced = (int) Round33GameTests.call(menu, "value", 5);
            h.assertTrue(synced == made, "synced " + synced + ", made " + made);
            h.assertTrue((int) Round33GameTests.call(menu, "value", 4) == 5, "tier IV");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 5)
    public static void quantumGeneratorTexts(GameTestHelper h) {
        String[] keys = {"line_output", "line_stored", "line_source", "state.run", "state.full", "state.off", "stored",
            "tip.bolt", "tip.core", "tip.resonance", "tip.concentrator", "tip.coil", "tip.cavity", "tip.casing", "tip.cable",
            "tip.core.title", "tip.cable.title"};
        for (String lang : new String[]{"en_us", "zh_cn", "ja_jp", "ru_ru"}) {
            String json = Round33GameTests.resource("/assets/mio_icif/lang/" + lang + ".json");
            for (String k : keys)
                h.assertTrue(json != null && json.contains("\"gui.mio_icif.quantum_generator." + k + "\""), lang + " lacks " + k);
        }
        String[][] pages = {{"", "## Background"}, {"_zh_cn/", "## 设定"}, {"_ja_jp/", "## 設定"}};
        for (String[] p : pages) {
            String md = Round33GameTests.resource("/assets/mio_icif/guides/mio_icif/manual/" + p[0] + "machines/generator_block_quantum_generator.md");
            h.assertTrue(md != null && md.contains(p[1]), p[0] + " lacks the background section");
        }
        h.succeed();
    }
}
