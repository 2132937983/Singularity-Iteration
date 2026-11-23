package example.sicore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.168: the TESU is a 7 x 7 x 13 box (it was a cylinder). Every layer is the full 7 x 7; the 189 cells keep the old
 * 3-5-5-5-3 inside, so a cylinder built before lacks only casing: an empty corner breaks the TESU, and the screen names it.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111168GameTests {
    private Round111168GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    @GameTest(template = "empty", batch = "round111168_box")
    public static void theTesuIsAFullBox(GameTestHelper h) {
        @SuppressWarnings("unchecked")
        List<Object> cells = (List<Object>) call(call(Round33GameTests.common("multiblock.TesuStructure"), "get"), "cells");
        Map<Integer, Integer> perLayer = new HashMap<>();
        Map<String, Integer> parts = new HashMap<>();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (Object cell : cells) {
            int x = (Integer) call(cell, "x"), y = (Integer) call(cell, "y"), z = (Integer) call(cell, "z");
            perLayer.merge(y, 1, Integer::sum);
            parts.merge(String.valueOf(call(cell, "part")), 1, Integer::sum);
            minX = Math.min(minX, x); maxX = Math.max(maxX, x); minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
        }
        h.assertTrue(perLayer.size() == 13 && perLayer.values().stream().allMatch(n -> n == 49), "layers: " + perLayer);
        h.assertTrue(maxX - minX == 6 && maxZ - minZ == 6, "the footprint spans " + (maxX - minX + 1) + " x " + (maxZ - minZ + 1));
        h.assertTrue(parts.getOrDefault("CELL", 0) == 189 && parts.getOrDefault("CASING", 0) == 331 && parts.getOrDefault("GLASS", 0) == 108
            && parts.getOrDefault("PORT_SLOT", 0) == 8 && parts.getOrDefault("CONTROLLER", 0) == 1, "parts: " + parts);
        h.succeed();
    }

    /** An empty corner (a cylinder from before has none) breaks the TESU and the screen names it; casing there forms it again. */
    @GameTest(template = "reactor_loop", batch = "round111168_corner", timeoutTicks = 300)
    public static void anEmptyCornerBreaksTheTesu(GameTestHelper h) {
        BlockPos controllerAt = Round111157GameTests.controller(h);
        Round111157GameTests.build(h, controllerAt, Direction.NORTH, 1, 1);
        BlockEntity controller = Round111157GameTests.be(h, controllerAt);
        // the top corner at the back right (layout x 6, y 12, z 6: from the controller x +3, y +11, z +6)
        BlockPos corner = controllerAt.offset(3, 11, 6);
        BlockState casing = h.getLevel().getBlockState(corner);
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue((Boolean) call(controller, "isStructureComplete"), "the box did not form"))
            .thenExecute(() -> h.getLevel().setBlock(corner, Blocks.AIR.defaultBlockState(), 3))
            .thenWaitUntil(() -> h.assertTrue(!(Boolean) call(controller, "isStructureComplete"), "an empty corner did not break the TESU"))
            .thenExecute(() -> h.assertTrue(corner.equals(call(controller, "wrongAt")) && "CASING".equals(String.valueOf(call(controller, "expected"))),
                "the screen names " + call(controller, "wrongAt") + " (" + call(controller, "expected") + "), not the corner " + corner))
            .thenExecute(() -> h.getLevel().setBlock(corner, casing, 3))
            .thenWaitUntil(() -> h.assertTrue((Boolean) call(controller, "isStructureComplete"), "casing in the corner did not form it again"))
            .thenSucceed();
    }
}
