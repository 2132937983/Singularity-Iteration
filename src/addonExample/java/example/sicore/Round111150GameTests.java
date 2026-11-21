package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.150, player feedback: a Large Fabricator's IV input module is a load, not storage (a player saw only a few hundred EU/t
 * over IV cables): it takes a creative generator's 8,192 EU/t whole and draws on a charged MFSU like any machine; and the quantum
 * helmet gives night vision in any light (it flickered where the light changes). (The mod's common classes by reflection: addon
 * examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111150GameTests {
    private Round111150GameTests() {}

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    /**
     * 0.1.11.150: the quantum helmet gives night vision in any light (it renewed it only at a light level of 8 or less, so in a
     * bright spot the effect ran down into vanilla's flicker, all the time in the Nether).
     */
    @GameTest(template = "empty", batch = "round111150_helmet", timeoutTicks = 40)
    public static void quantumHelmetGivesNightVisionInBrightLight(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        for (var side : net.minecraft.core.Direction.Plane.HORIZONTAL) h.setBlock(at.relative(side), net.minecraft.world.level.block.Blocks.GLOWSTONE);
        BlockPos abs = h.absolutePos(at);
        h.runAfterDelay(5, () -> {
            int light = h.getLevel().getMaxLocalRawBrightness(abs);
            h.assertTrue(light > 8, "the spot is not bright: light " + light);
            var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
            var helmet = new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "armor/item_armor_quantum_helmet")));
            Object armor = helmet.getItem();
            call(armor, "setEnergy", helmet, ((Number) call(armor, "getMaxEnergy")).longValue());
            call(Round33GameTests.common("item.armor.ArmorFeatureToggle"), "setEnabled", helmet, "night_vision", true);
            call(Round33GameTests.common("item.armor.mio_icif_helmet_quantum"), "applyQuantumHelmetTraits", helmet, armor, h.getLevel(), player);
            var effect = player.getEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            h.assertTrue(effect != null, "no night vision at light " + light);
            h.assertTrue(effect.getDuration() > 200, "night vision of " + effect.getDuration() + " ticks flickers (vanilla flickers below 200)");
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "round111150_fabricator", timeoutTicks = 200)
    public static void largeFabricatorModuleTakesWhatIvCablesCarry(GameTestHelper h) {
        BlockPos core = new BlockPos(3, 2, 6);
        BlockPos[] inputs = {core.north(), core.south(), core.east(), core.west()};
        for (BlockPos p : inputs) h.setBlock(p, block("producer/block_large_fabricator_input_iv"));
        h.setBlock(core.above(), block("producer/block_large_fabricator_tank"));
        h.setBlock(core.below(), block("producer/block_large_fabricator_scrap"));
        h.setBlock(core, block("producer/block_large_fabricator_core"));
        h.setBlock(core.north(2), block("wiring/cable/block_glass_cable"));
        h.setBlock(core.north(3), block("wiring/cable/block_glass_cable"));
        h.setBlock(core.north(4), block("generator/block_creative_generator"));
        BlockEntity coreBe = h.getBlockEntity(core), module = h.getBlockEntity(core.north()), gen = h.getBlockEntity(core.north(4));
        call(gen, "configure", 8_192L, true);
        long[] start = new long[1];
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue((Boolean) call(coreBe, "isStructureComplete"), "the structure did not form"))
            .thenIdle(5)
            .thenExecute(() -> start[0] = (Long) call(module, "getStoredEnergy"))
            .thenIdle(40)
            .thenExecute(() -> {
                long now = (Long) call(module, "getStoredEnergy");
                double perTick = (now - start[0]) / 40.0;
                System.out.println("[Round111150] IV input module took " + perTick + " EU/t from an 8,192 EU/t creative generator over IV cables (stored " + now + ")");
                h.assertTrue(perTick >= 8_000, "the module took only " + perTick + " EU/t of 8,192 EU/t over IV cables");
            })
            .thenSucceed();
    }

    /**
     * The cause: the engine counted the input module as storage, and storage gets only the generators' surplus, shared with the
     * batteries that charge. Now it is a load and comes before them: a 2,048 EU/t generator, an empty MFSU and the module on one
     * line: the module takes the whole 2,048 EU/t, the MFSU charges only from what is left (nothing here).
     */
    @GameTest(template = "empty", batch = "round111150_fabricator", timeoutTicks = 200)
    public static void largeFabricatorModuleComesBeforeChargingBatteries(GameTestHelper h) {
        BlockPos core = new BlockPos(3, 2, 6);
        BlockPos[] inputs = {core.north(), core.south(), core.east(), core.west()};
        for (BlockPos p : inputs) h.setBlock(p, block("producer/block_large_fabricator_input_iv"));
        h.setBlock(core.above(), block("producer/block_large_fabricator_tank"));
        h.setBlock(core.below(), block("producer/block_large_fabricator_scrap"));
        h.setBlock(core, block("producer/block_large_fabricator_core"));
        h.setBlock(core.north(2), block("wiring/cable/block_glass_cable"));
        h.setBlock(core.north(3), block("wiring/cable/block_glass_cable"));
        h.setBlock(core.north(4), block("generator/block_creative_generator"));
        // an empty MFSU beside the line (it takes EV packets: the generator sends 2,048 EU), its output facing away
        h.setBlock(core.north(3).east(), block("wiring/block_mfsu").defaultBlockState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.core.Direction.EAST));
        BlockEntity coreBe = h.getBlockEntity(core), module = h.getBlockEntity(core.north()), gen = h.getBlockEntity(core.north(4)),
            mfsu = h.getBlockEntity(core.north(3).east());
        call(gen, "configure", 2_048L, true);
        long[] start = new long[2];
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue((Boolean) call(coreBe, "isStructureComplete"), "the structure did not form"))
            .thenIdle(5)
            .thenExecute(() -> { start[0] = (Long) call(module, "getStoredEnergy"); start[1] = (Long) call(mfsu, "apiGetStoredEnergy"); })
            .thenIdle(40)
            .thenExecute(() -> {
                double module40 = ((Long) call(module, "getStoredEnergy") - start[0]) / 40.0, mfsu40 = ((Long) call(mfsu, "apiGetStoredEnergy") - start[1]) / 40.0;
                System.out.println("[Round111150] 2,048 EU/t between the module and a charging MFSU: module " + module40 + " EU/t, MFSU " + mfsu40 + " EU/t");
                h.assertTrue(module40 >= 2_000, "the module took only " + module40 + " EU/t (the charging MFSU took " + mfsu40 + " EU/t)");
            })
            .thenSucceed();
    }

    /** A charged MFSU, its only source, feeds the module, as it feeds any machine (IC2's Mass Fabricator drains it too). */
    @GameTest(template = "empty", batch = "round111150_fabricator", timeoutTicks = 200)
    public static void largeFabricatorModuleDrawsOnStorageLikeAMachine(GameTestHelper h) {
        BlockPos core = new BlockPos(3, 2, 6);
        BlockPos[] inputs = {core.north(), core.south(), core.east(), core.west()};
        for (BlockPos p : inputs) h.setBlock(p, block("producer/block_large_fabricator_input_iv"));
        h.setBlock(core.above(), block("producer/block_large_fabricator_tank"));
        h.setBlock(core.below(), block("producer/block_large_fabricator_scrap"));
        h.setBlock(core, block("producer/block_large_fabricator_core"));
        h.setBlock(core.north(2), block("wiring/cable/block_glass_cable"));
        h.setBlock(core.north(3), block("wiring/cable/block_glass_cable"));
        h.setBlock(core.north(4), block("wiring/block_mfsu").defaultBlockState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.core.Direction.SOUTH));
        BlockEntity coreBe = h.getBlockEntity(core), module = h.getBlockEntity(core.north()), mfsu = h.getBlockEntity(core.north(4));
        call(mfsu, "apiGenerateEnergy", 10_000_000L, false);
        long[] start = new long[3];
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue((Boolean) call(coreBe, "isStructureComplete"), "the structure did not form"))
            .thenIdle(5)
            .thenExecute(() -> {
                start[0] = (Long) call(module, "getStoredEnergy");
                start[1] = (Long) call(mfsu, "apiGetStoredEnergy");
                start[2] = (Integer) call(coreBe, "getTotalUuMatterAmount");
            })
            .thenIdle(40)
            .thenExecute(() -> {
                // what reached the module: its buffer's growth plus the EU the core turned into UU-matter (800,000 a mB)
                long made = (Integer) call(coreBe, "getTotalUuMatterAmount") - start[2];
                long took = (Long) call(module, "getStoredEnergy") - start[0] + made * 800_000L, gave = start[1] - (Long) call(mfsu, "apiGetStoredEnergy");
                System.out.println("[Round111150] IV input module took " + took / 40.0 + " EU/t from a charged MFSU (it gave " + gave / 40.0 + " EU/t)");
                h.assertTrue(took >= 40 * 2_000L, "the module took only " + took / 40.0 + " EU/t from a charged MFSU (storage did not feed it)");
            })
            .thenSucceed();
    }
}
