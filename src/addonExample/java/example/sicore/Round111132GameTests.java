package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.132 (fusion overhaul F0): the principles section of the fusion reactor console's page in the three manuals, before
 * the power data, naming the D-T reaction, the Lawson criterion and the fusion-fission hybrid blanket.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111132GameTests {
    private Round111132GameTests() {}

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void fusionPrinciplesSection(GameTestHelper h) {
        String[][] pages = {
            {"", "## Principles", "## Power data", "17.6 MeV", "Lawson", "fusion-fission hybrid"},
            {"_zh_cn/", "## 原理", "## 电力数据", "17.6 MeV", "劳森判据", "聚变-裂变混合堆"},
            {"_ja_jp/", "## 原理", "## 電力データ", "17.6 MeV", "ローソン条件", "核融合・核分裂ハイブリッド炉"}};
        for (String[] p : pages) {
            String md = Round33GameTests.resource("/assets/mio_icif/guides/mio_icif/manual/" + p[0] + "machines/fusion_block_fusion_controller.md");
            h.assertTrue(md != null, p[0] + " page missing");
            int at = md.indexOf(p[1]), data = md.indexOf(p[2]);
            h.assertTrue(at >= 0 && data > at, p[0] + " principles section missing or after the power data");
            for (int i = 3; i < p.length; i++) h.assertTrue(md.indexOf(p[i], at) >= 0 && md.indexOf(p[i], at) < data, p[0] + " lacks " + p[i]);
        }
        h.succeed();
    }
}
