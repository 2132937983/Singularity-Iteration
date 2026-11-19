package example.sicore;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.85: the turbocharged and twin turbocharged kinetic generators give the full EU per KU (0.85 and 1.2) only for a
 * wind or water kinetic generator in front, and the lower rate (0.35 and 0.45) for every other source. The source check
 * went through IKineticAPI.hasKineticTile, which knows only the core API kinetic interfaces; no kinetic source of the mod
 * implements them, so the check saw "nothing in front" and every source got the full rate.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11585GameTests {
    private Round11585GameTests() {}

    private static final long KU = 1_000L;

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static double constant(String generator, String name) {
        try {
            var f = Round33GameTests.common("blockentity.generator.mio_icif_" + generator).getDeclaredField(name);
            f.setAccessible(true);
            return ((Number) f.get(null)).doubleValue();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }

    private static long stored(BlockEntity generator) {
        return ((Number) call(call(generator, "getEnergyStorage"), "getAmount")).longValue();
    }

    /** Source block, natural (wind or water), and whether the test measures the EU it makes (no EU acceptor beside it). */
    private record Source(String block, boolean natural, boolean measured) {}

    // pairs at x 0-1 and 2-3, z 0, 2 and 4: the measured generators (x 0) have nothing that takes EU beside them
    private static final Source[] SOURCES = {
        new Source("kugenerator/block_wind_kinetic_generator", true, true),
        new Source("kugenerator/block_water_kinetic_generator", true, false),
        new Source("kugenerator/block_stirling_kinetic_generator", false, true),
        new Source("kugenerator/block_manual_kinetic_generator", false, false),
        new Source("producer/block_steam_kinetic_generator", false, false),
        new Source("kugenerator/block_kinetic_generator_elc", false, false),
    };

    private static BlockPos generatorPos(int i) { return new BlockPos(i % 2 == 0 ? 0 : 2, 1, 2 * (i / 2)); }

    private static void rates(GameTestHelper h, String generator) {
        double full = constant(generator, "EU_PER_KU"), other = constant(generator, "ARTIFICIAL_EU_PER_KU");
        for (int i = 0; i < SOURCES.length; i++) {
            BlockPos at = generatorPos(i);
            h.setBlock(at.east(), BuiltInRegistries.BLOCK.get(id(SOURCES[i].block())));
            BlockState state = BuiltInRegistries.BLOCK.get(id("generator/block_" + generator)).defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.EAST);
            h.setBlock(at, state);
        }
        long[] before = new long[SOURCES.length];
        h.runAfterDelay(3, () -> {
            for (int i = 0; i < SOURCES.length; i++) {
                if (!SOURCES[i].measured()) continue;
                BlockEntity g = h.getBlockEntity(generatorPos(i));
                before[i] = stored(g);
                long in = ((Number) call(call(g, "getKineticStorage"), "receiveKinetic", KU, false)).longValue();
                h.assertTrue(in == KU, generator + " took " + in + " of " + KU + " KU");
            }
        });
        h.runAfterDelay(6, () -> {
            List<String> wrong = new ArrayList<>();
            for (int i = 0; i < SOURCES.length; i++) {
                Source s = SOURCES[i];
                BlockEntity g = h.getBlockEntity(generatorPos(i));
                double rate = s.natural() ? full : other;
                double perKu = ((Number) call(g, "getEuPerKu")).doubleValue();
                if (Math.abs(perKu - rate) > 1e-9) wrong.add(s.block() + ": " + perKu + " EU per KU, expected " + rate);
                if (s.measured()) {
                    double scale = ((Number) call(Round33GameTests.common("energy.GeneratorOutput"), "multiplier", g)).doubleValue();
                    long made = stored(g) - before[i], expected = Math.round(KU * rate * scale);
                    if (Math.abs(made - expected) > 1) wrong.add(s.block() + ": " + KU + " KU made " + made + " EU, expected " + expected);
                }
            }
            h.assertTrue(wrong.isEmpty(), generator + " rates by source: " + wrong);
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "round11585_kinetic_rate", timeoutTicks = 40)
    public static void turboGivesTheFullRateOnlyToWindAndWater(GameTestHelper h) { rates(h, "turbo_kinetic_generator"); }

    @GameTest(template = "empty", batch = "round11585_kinetic_rate", timeoutTicks = 40)
    public static void twinTurboGivesTheFullRateOnlyToWindAndWater(GameTestHelper h) { rates(h, "twin_turbo_kinetic_generator"); }
}
