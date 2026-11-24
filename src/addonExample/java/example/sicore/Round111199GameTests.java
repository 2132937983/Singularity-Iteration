package example.sicore;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GameTests of 0.1.11.199: tool overhaul T5b, the ore scanners (c:ores only, kinds by tag, nearest block). */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111199GameTests {
    private Round111199GameTests() {}

    /** U-07: only c:ores counts (a spore blossom, whose name holds "ore", does not), kinds by tag, the nearest block's offset. */
    @GameTest(template = "empty")
    @SuppressWarnings("unchecked")
    public static void scannerCountsTaggedOresAndFindsTheNearest(GameTestHelper h) {
        h.setBlock(new BlockPos(1, 1, 1), Blocks.IRON_ORE);
        h.setBlock(new BlockPos(3, 1, 1), Blocks.DEEPSLATE_IRON_ORE);
        h.setBlock(new BlockPos(1, 1, 3), Blocks.COPPER_ORE);
        h.setBlock(new BlockPos(2, 3, 2), Blocks.SPORE_BLOSSOM);
        BlockPos centre = h.absolutePos(new BlockPos(2, 1, 1));
        Class<?> scanner;
        try {
            scanner = Class.forName("com.miophas.singularity_iteration." + "common.item.tools.OreScannerItem");
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
        Map<String, Integer> found = (Map<String, Integer>) ArmoryServiceGameTests.call(scanner, "scan", h.getLevel(), centre, 3, 3);
        Integer iron = null, copper = null;
        String ironKey = null;
        for (var e : found.entrySet()) {
            if (e.getKey().startsWith("tooltip.mio_icif.ore.iron@")) { iron = e.getValue(); ironKey = e.getKey(); }
            if (e.getKey().startsWith("tooltip.mio_icif.ore.copper@")) copper = e.getValue();
        }
        h.assertTrue(iron != null && iron == 2, "both iron ores (stone and deepslate) are one kind, found " + found);
        h.assertTrue(copper != null && copper == 1, "copper is its own kind, found " + found);
        h.assertTrue(found.size() == 2, "nothing else counts (the spore blossom is no ore), found " + found);
        h.assertTrue(ironKey.endsWith("@-1,0,0") || ironKey.endsWith("@1,0,0"), "the nearest iron is one block away, got " + ironKey);
        h.succeed();
    }
}
