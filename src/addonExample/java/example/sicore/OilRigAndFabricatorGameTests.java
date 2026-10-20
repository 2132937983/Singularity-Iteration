package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Oil Rig strikes oil often in ore (the {@code c:ores} tag), not in every block whose name holds
 * "ore"; the Large Fabricator lets its scrap module pay part of a millibucket, so the input only
 * needs the EU the scrap leaves.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class OilRigAndFabricatorGameTests {
    private OilRigAndFabricatorGameTests() {}

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> type(String name) {
        try {
            return Class.forName("com.miophas.singularity_iteration.common." + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private static double chance(BlockState state) {
        return (Double) call(type("blockentity.oilrig.mio_icif_oil_rig_core_entity"), "strikeChance", state);
    }

    @GameTest(batch = "oil_rig", template = "empty", timeoutTicks = 20)
    public static void strikeChanceTakesOreFromTheTag(GameTestHelper h) {
        double ore = chance(Blocks.IRON_ORE.defaultBlockState()), other = chance(Blocks.STONE.defaultBlockState());
        h.assertTrue(ore == 0.8 && other == 0.2, "iron ore " + ore + ", stone " + other);
        h.assertTrue(chance(BuiltInRegistries.BLOCK.get(rl("block_ore_tin")).defaultBlockState()) == ore, "SI tin ore is not ore");
        h.assertTrue(chance(Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState()) == ore, "deepslate diamond ore is not ore");
        // "ore" inside these names (core, spore) used to make them count as ore
        for (var block : new net.minecraft.world.level.block.Block[] {Blocks.HEAVY_CORE, Blocks.SPORE_BLOSSOM,
                BuiltInRegistries.BLOCK.get(rl("oilrig/block_oil_rig_core"))}) {
            h.assertTrue(chance(block.defaultBlockState()) == other, BuiltInRegistries.BLOCK.getKey(block) + " counts as ore");
        }
        h.assertTrue(chance(Blocks.OBSIDIAN.defaultBlockState()) == 0.95 && chance(Blocks.SAND.defaultBlockState()) == 0.1,
            "obsidian / sand");
        h.succeed();
    }

    /** A core with an input on one side and a scrap module on the other; stored EU and scrap value set directly. */
    private static BlockEntity[] fabricator(GameTestHelper h, long stored, float scrap) {
        BlockPos core = new BlockPos(2, 2, 2);
        h.setBlock(core, BuiltInRegistries.BLOCK.get(rl("producer/block_large_fabricator_core")));
        h.setBlock(core.east(), BuiltInRegistries.BLOCK.get(rl("producer/block_large_fabricator_input_iv")));
        h.setBlock(core.west(), BuiltInRegistries.BLOCK.get(rl("producer/block_large_fabricator_scrap")));
        BlockEntity input = h.getBlockEntity(core.east()), module = h.getBlockEntity(core.west());
        call(call(input, "getEnergyStorageInternal"), "setStored", stored);
        try {
            var field = module.getClass().getDeclaredField("scrapValue");
            field.setAccessible(true);
            field.setFloat(module, scrap);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return new BlockEntity[] {h.getBlockEntity(core), input, module};
    }

    private static void fabricate(GameTestHelper h, BlockEntity core) {
        call(core, "processFabrication", h.getLevel(), core.getBlockPos());
    }

    private static int uuMatter(BlockEntity core) {
        return ((FluidTank) call(core, "getUuMatterTank")).getFluidAmount();
    }

    @GameTest(batch = "large_fabricator", template = "empty", timeoutTicks = 20)
    public static void scrapPaysPartOfTheEnergy(GameTestHelper h) {
        // 795,000 EU + 5,000 scrap = 800,000: one millibucket, both spent
        BlockEntity[] parts = fabricator(h, 795_000L, 5_000F);
        fabricate(h, parts[0]);
        h.assertTrue(uuMatter(parts[0]) == 1, "UU-matter " + uuMatter(parts[0]));
        h.assertTrue((Long) call(parts[1], "getStoredEnergy") == 0L, "EU left " + call(parts[1], "getStoredEnergy"));
        h.assertTrue((Float) call(parts[2], "getScrapValue") == 0F, "scrap left " + call(parts[2], "getScrapValue"));
        h.succeed();
    }

    @GameTest(batch = "large_fabricator", template = "empty", timeoutTicks = 20)
    public static void notEnoughEnergyAndScrapMakesNothing(GameTestHelper h) {
        BlockEntity[] parts = fabricator(h, 794_999L, 5_000F);
        fabricate(h, parts[0]);
        h.assertTrue(uuMatter(parts[0]) == 0, "UU-matter " + uuMatter(parts[0]));
        h.assertTrue((Long) call(parts[1], "getStoredEnergy") == 794_999L && (Float) call(parts[2], "getScrapValue") == 5_000F,
            "spent EU " + call(parts[1], "getStoredEnergy") + " / scrap " + call(parts[2], "getScrapValue"));
        h.succeed();
    }

    /** A Thorium Scrapbox (1,200,000) covers a whole millibucket without stored EU. */
    @GameTest(batch = "large_fabricator", template = "empty", timeoutTicks = 20)
    public static void scrapAloneCoversAWholeMillibucket(GameTestHelper h) {
        BlockEntity[] parts = fabricator(h, 0L, 1_200_000F);
        fabricate(h, parts[0]);
        h.assertTrue(uuMatter(parts[0]) == 1, "UU-matter " + uuMatter(parts[0]));
        h.assertTrue((Float) call(parts[2], "getScrapValue") == 400_000F, "scrap left " + call(parts[2], "getScrapValue"));
        h.succeed();
    }
}
