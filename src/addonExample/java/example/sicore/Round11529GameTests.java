package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.29 (cable audit W0): rubber on glass fibre, the outline of the 6 px cables, foam drives water out, the
 * superconductor takes no dye, cutter and rubber do not reach through foam, and every cable blockstate draws each casing
 * once. Blocks and block entities are reached by registry id and reflection (example code uses the public API only).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11529GameTests {
    private Round11529GameTests() {}

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path))); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static BooleanProperty flag(BlockState state, String name) { return (BooleanProperty) state.getBlock().getStateDefinition().getProperty(name); }

    private static void use(GameTestHelper h, BlockPos at, ItemStack stack) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = h.absolutePos(at);
        var hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        h.getBlockState(at).useItemOn(stack, h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    }

    /** Rubber on glass fibre does nothing (it made a splitter cable); tin still insulates, the special cables have no counterparts. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void rubberCounterparts(GameTestHelper h) {
        h.assertTrue(call(block("wiring/cable/block_glass_cable"), "getInsulatedCounterpart") == null, "glass fibre has no insulated form");
        h.assertTrue(call(block("wiring/cable/block_tin_cable"), "getInsulatedCounterpart") == block("wiring/cable/block_tin_cable_1"), "tin insulates");
        for (String special : new String[]{"wiring/block_eu_detector_cable", "wiring/block_eu_splitter_cable", "wiring/block_eu_breaker_cable", "wiring/block_eu_fuse_cable"})
            h.assertTrue(call(block(special), "getBareCounterpart") == null, special + " strips to nothing");
        BlockPos at = new BlockPos(1, 2, 1);
        h.setBlock(at, block("wiring/cable/block_glass_cable"));
        use(h, at, item("resource/item_rubber"));
        h.assertTrue(h.getBlockState(at).getBlock() == block("wiring/cable/block_glass_cable"), "glass fibre stays glass fibre: " + h.getBlockState(at));
        h.succeed();
    }

    /** Glass fibre and the superconductor are outlined 6 px thick, as drawn. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void sixPixelOutlines(GameTestHelper h) {
        for (String id : new String[]{"wiring/cable/block_glass_cable", "wiring/cable/block_superconducting_cable"}) {
            BlockPos at = new BlockPos(1, 2, 1);
            h.setBlock(at, block(id));
            var box = h.getBlockState(at).getShape(h.getLevel(), h.absolutePos(at)).bounds();
            h.assertTrue(Math.abs(box.minX * 16 - 5) < 1e-6 && Math.abs(box.maxX * 16 - 11) < 1e-6, id + " outline " + box);
            h.assertTrue((int) call(block(id), "getShapeRadius") == 3, id + " radius");
        }
        h.succeed();
    }

    /** Foam on a waterlogged cable drives the water out; a scaffolded cable keeps its scaffold. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void foamDrivesWaterOut(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        BlockState wet = block("wiring/cable/block_tin_cable").defaultBlockState();
        wet = wet.setValue(flag(wet, "waterlogged"), true).setValue(flag(wet, "foam_reinforced"), true);
        h.setBlock(at, wet);
        use(h, at, item("build/block_construction_foam"));
        BlockState now = h.getBlockState(at);
        h.assertTrue(now.getValue(flag(now, "foamlogged")), "foamed: " + now);
        h.assertTrue(!now.getValue(flag(now, "waterlogged")) && now.getFluidState().isEmpty(), "no water left: " + now);
        h.assertTrue(now.getValue(flag(now, "foam_reinforced")), "scaffold kept");
        h.succeed();
    }

    /** The superconductor takes no dye; the cutter and rubber do not reach a cable inside foam. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void dyeCutterAndRubberRules(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        h.setBlock(at, block("wiring/cable/block_superconducting_cable"));
        h.assertTrue(!(boolean) call(h.getBlockEntity(at), "canColor", DyeColor.RED), "superconductor dye");
        BlockState foamed = block("wiring/cable/block_tin_cable_1").defaultBlockState();
        foamed = foamed.setValue(flag(foamed, "foamlogged"), true);
        h.setBlock(at, foamed);
        use(h, at, item("item_tool_cutter"));
        h.assertTrue(h.getBlockState(at).getBlock() == block("wiring/cable/block_tin_cable_1"), "the cutter does not strip through foam");
        BlockState bare = block("wiring/cable/block_tin_cable").defaultBlockState();
        h.setBlock(at, bare.setValue(flag(bare, "foamlogged"), true));
        use(h, at, item("resource/item_rubber"));
        h.assertTrue(h.getBlockState(at).getBlock() == block("wiring/cable/block_tin_cable"), "rubber does not insulate through foam");
        h.succeed();
    }

    /** Every cable blockstate (the special cables too) draws each casing once: no wall under wet foam, no second scaffold. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void casingPartsOnce(GameTestHelper h) {
        String[] files = {"cable/block_tin_cable", "cable/block_tin_cable_1", "cable/block_cable_o", "cable/block_cable", "cable/block_gold_cable",
            "cable/block_gold_cable_1", "cable/block_gold_cable_2", "cable/block_iron_cable", "cable/block_iron_cable_1", "cable/block_iron_cable_2",
            "cable/block_iron_cable_3", "cable/block_glass_cable", "cable/block_superconducting_cable", "block_eu_detector_cable",
            "block_eu_splitter_cable", "block_eu_breaker_cable", "block_eu_fuse_cable"};
        for (String f : files) {
            String json = Round33GameTests.resource("/assets/mio_icif/blockstates/wiring/" + f + ".json");
            h.assertTrue(json != null, f + " missing");
            var parts = com.google.gson.JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("multipart");
            int casing = 0, walls = 0, scaffolds = 0;
            for (var p : parts) {
                var apply = p.getAsJsonObject().get("apply");
                String model = (apply.isJsonArray() ? apply.getAsJsonArray().get(0) : apply).getAsJsonObject().get("model").getAsString();
                if (!model.startsWith("mio_icif:block/build/")) continue;
                casing++;
                if (model.endsWith("_wall")) walls++;
                if (model.endsWith("scaffold_iron")) scaffolds++;
                var when = p.getAsJsonObject().getAsJsonObject("when");
                if (model.endsWith("_wall")) h.assertTrue("true".equals(when.get("foam_hardened").getAsString()), f + ": a wall under wet foam");
            }
            h.assertTrue(casing == 5 && walls == 2 && scaffolds == 1, f + ": casing parts " + casing + " walls " + walls + " scaffolds " + scaffolds);
        }
        h.succeed();
    }
}
