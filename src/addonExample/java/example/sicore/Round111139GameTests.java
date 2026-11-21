package example.sicore;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.139 (fusion overhaul F7): the diagnostic viewport stands in for a blanket module or a vacuum vessel wall (the
 * demonstration's sixteen form; one in a coil's place does not), it lets the view through, and the console sends the plasma's
 * state to clients in its update tag. (Classes by name.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111139GameTests {
    private Round111139GameTests() {}

    private static final String STRUCTURE = "fusion.FusionStructure";

    private static Block block(String name) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "fusion/block_fusion_" + name)); }
    private static Class<?> cls(String name) { return Round33GameTests.common(name); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static List<BlockPos> build(GameTestHelper h, BlockPos controller) {
        var level = h.getLevel();
        List<BlockPos> placed = new ArrayList<>();
        String[] ports = {"power_port", "cryo_port", "nbi_port", "ecrh_port", "exhaust_port", "fuel_port", "mitigation_port", "nbi_port", "ecrh_port"};
        int port = 0;
        for (Object c : (List<?>) call(call(cls(STRUCTURE), "tokamak"), "cells")) {
            BlockPos at = controller.offset(((Number) call(c, "x")).intValue(), ((Number) call(c, "y")).intValue(), ((Number) call(c, "z")).intValue());
            BlockState state = switch (call(c, "part").toString()) {
                case "PLASMA" -> Blocks.AIR.defaultBlockState();
                case "CONTROLLER" -> block("controller").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
                case "PORT_SLOT" -> block(ports[port++ % ports.length]).defaultBlockState();
                default -> ((Block) call(cls(STRUCTURE), "required", call(c, "part"))).defaultBlockState();
            };
            level.setBlock(at, state, Block.UPDATE_ALL);
            placed.add(at);
        }
        return placed;
    }

    @GameTest(template = "empty", batch = "round111139_viewport", timeoutTicks = 200)
    public static void viewportStandsInForBlanketAndVessel(GameTestHelper h) throws Exception {
        BlockPos at = h.absolutePos(new BlockPos(2, 6, 2));
        List<BlockPos> placed = build(h, at);
        h.runAfterDelay(3, () -> {
            List<String> wrong = new ArrayList<>();
            try {
                var level = h.getLevel();
                Block viewport = block("viewport");
                if (viewport == Blocks.AIR) wrong.add("no viewport block");
                if (viewport.defaultBlockState().canOcclude()) wrong.add("the viewport hides what is behind it");
                Object structure = call(cls(STRUCTURE), "tokamak");
                BlockPos centre = (BlockPos) call(structure, "centre", at, Direction.EAST);
                int[][] viewports = (int[][]) cls("fusion.FusionDemo").getField("VIEWPORTS").get(null);
                for (int[] v : viewports) level.setBlock(centre.offset(v[0], v[1], v[2]), viewport.defaultBlockState(), Block.UPDATE_ALL);
                BlockEntity c = level.getBlockEntity(at);
                call(c, "validate");
                if (!(Boolean) call(c, "isFormed")) wrong.add("sixteen viewports in blanket and vessel places broke it: " + call(c, "status"));
                // each of the demonstration's viewports sits in a blanket or vessel place, in line with the plasma
                for (int[] v : viewports) {
                    BlockPos p = centre.offset(v[0], v[1], v[2]);
                    String part = null;
                    for (Object cell : (List<?>) call(structure, "cells"))
                        if (at.offset(((Number) call(cell, "x")).intValue(), ((Number) call(cell, "y")).intValue(), ((Number) call(cell, "z")).intValue()).equals(p))
                            part = call(cell, "part").toString();
                    if (!"BLANKET".equals(part) && !"VACUUM_VESSEL".equals(part)) wrong.add("viewport at " + p + " in a " + part + " place");
                }
                // one in a coil's place does not count
                Object coil = Enum.valueOf((Class) cls(STRUCTURE + "$Part"), "TOROIDAL_COIL");
                BlockPos coilAt = (BlockPos) ((List<?>) call(structure, "positions", at, Direction.EAST, coil)).get(0);
                level.setBlock(coilAt, viewport.defaultBlockState(), Block.UPDATE_ALL);
                call(c, "validate");
                if ((Boolean) call(c, "isFormed")) wrong.add("a viewport in a coil's place formed");
                // the plasma's state goes to clients in the update tag
                CompoundTag tag = (CompoundTag) c.getClass().getMethod("getUpdateTag", net.minecraft.core.HolderLookup.Provider.class).invoke(c, level.registryAccess());
                if (!tag.contains("Fx") || !tag.getCompound("Fx").contains("State") || !tag.getCompound("Fx").contains("Elms")) wrong.add("no plasma state in the update tag: " + tag);
                for (String lang : new String[]{"en_us", "zh_cn", "ja_jp", "ru_ru"}) {
                    String json = Round33GameTests.resource("/assets/mio_icif/lang/" + lang + ".json");
                    if (json == null || !json.contains("\"block.mio_icif.fusion.block_fusion_viewport\"")) wrong.add(lang + " lacks the viewport");
                }
                if (Round33GameTests.resource("/data/mio_icif/recipe/fusion/viewport.json") == null) wrong.add("no viewport recipe");
            } catch (ReflectiveOperationException e) {
                wrong.add(e.toString());
            }
            for (BlockPos p : placed) h.getLevel().setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            h.assertTrue(wrong.isEmpty(), "viewport: " + wrong);
            h.succeed();
        });
    }
}
