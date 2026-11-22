package example.sicore;

import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.152: the EU detector, splitter and breaker cables' outline and collision follow their models (an 8 px core, 6 px arms;
 * they were a 4 px box like every special cable), the fuse cable's too (6 px throughout); and automation: a hopper feeds the
 * nuclear reactor through a reactor chamber (chambers and access hatches hand on the reactor's 54 slots, as in IC2) and the
 * fusion console's fuel slots (deuterium in; a neon pellet is refused: it goes through the mitigation port).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111152GameTests {
    private Round111152GameTests() {}

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static ItemStack item(String path, int count) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)), count);
    }

    /** Whether the point (in pixels of the block) lies in the shape of the block at {@code at}. */
    private static boolean inShape(GameTestHelper h, BlockPos at, double x, double y, double z) {
        var abs = h.absolutePos(at);
        Vec3 p = new Vec3(x / 16.0, y / 16.0, z / 16.0);
        for (AABB box : h.getLevel().getBlockState(abs).getShape(h.getLevel(), abs).toAabbs()) if (box.contains(p)) return true;
        return false;
    }

    @GameTest(template = "empty", batch = "round111152_cables", timeoutTicks = 20)
    public static void specialCableShapesFollowTheirModels(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        BlockPos at = new BlockPos(1, 1, 1);
        // detector, splitter, breaker: an 8 px core (4..12), 6 px arms (5..11); the fuse 6 px throughout
        Object[][] cables = {{"wiring/block_eu_detector_cable", 4, 3}, {"wiring/block_eu_splitter_cable", 4, 3},
            {"wiring/block_eu_breaker_cable", 4, 3}, {"wiring/block_eu_fuse_cable", 3, 3}};
        for (Object[] c : cables) {
            Block cable = block((String) c[0]);
            int core = (Integer) c[1], arm = (Integer) c[2];
            // the cables' own connection property (not vanilla's)
            var eastSide = (net.minecraft.world.level.block.state.properties.BooleanProperty) cable.getStateDefinition().getProperty("east");
            BlockState lone = cable.defaultBlockState(), east = lone.setValue(eastSide, true);
            h.setBlock(at, lone);
            if (!inShape(h, at, 8 - core + 0.5, 8, 8) || inShape(h, at, 8 - core - 0.5, 8, 8)) wrong.add(c[0] + ": the core is not " + 2 * core + " px");
            h.setBlock(at, east);
            if (!inShape(h, at, 15.5, 8 + arm - 0.5, 8)) wrong.add(c[0] + ": the east arm is not " + 2 * arm + " px");
            if (inShape(h, at, 15.5, 8 + arm + 0.5, 8)) wrong.add(c[0] + ": the east arm is thicker than " + 2 * arm + " px");
            if (core > arm && !inShape(h, at, 8, 8 + core - 0.5, 8)) wrong.add(c[0] + ": the core is not wider than the arm");
        }
        h.setBlock(at, Blocks.AIR);
        h.assertTrue(wrong.isEmpty(), "special cables: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111152_automation", timeoutTicks = 200)
    public static void hopperFeedsTheReactorThroughAChamber(GameTestHelper h) {
        BlockPos core = new BlockPos(2, 2, 2), chamber = core.above(), hopper = chamber.above();
        h.setBlock(core, block("generator/block_nuclear_reactor_generator"));
        h.setBlock(chamber, block("reactor/block_reactor_chamber"));
        h.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(BlockStateProperties.FACING_HOPPER, Direction.DOWN));
        ((HopperBlockEntity) h.getBlockEntity(hopper)).setItem(0, item("reactor/item_reactor_uranium_simple", 1));
        IReactorController reactor = (IReactorController) h.getBlockEntity(core);
        h.succeedWhen(() -> {
            int rods = 0;
            for (int i = 0; i < reactor.getItemHandler().getSlots(); i++) rods += reactor.getItemHandler().getStackInSlot(i).getCount();
            h.assertTrue(rods == 1, "the hopper on the chamber did not feed the reactor (" + rods + " components inside)");
        });
    }

    @GameTest(template = "empty", batch = "round111152_automation", timeoutTicks = 200)
    public static void hopperFeedsTheFusionConsoleFuelOnly(GameTestHelper h) {
        BlockPos console = new BlockPos(2, 2, 2), hopper = console.above();
        h.setBlock(console, block("fusion/block_fusion_controller").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        h.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(BlockStateProperties.FACING_HOPPER, Direction.DOWN));
        var hopperBe = (HopperBlockEntity) h.getBlockEntity(hopper);
        hopperBe.setItem(0, item("normal/item_frozen_neon_pellet", 1));
        hopperBe.setItem(1, item("normal/item_deuterium_cell", 1));
        var consoleBe = h.getBlockEntity(console);
        var items = (net.neoforged.neoforge.items.IItemHandler) Round33GameTests.call(consoleBe, "getItems");
        h.succeedWhen(() -> {
            int rods = 0, pellets = 0;
            for (int i = 0; i < items.getSlots(); i++) {
                ItemStack s = items.getStackInSlot(i);
                if (s.is(item("normal/item_deuterium_cell", 1).getItem())) rods += s.getCount();
                if (s.is(item("normal/item_frozen_neon_pellet", 1).getItem())) pellets += s.getCount();
            }
            // the console takes a rod from its slot into its deuterium tank at once
            long tank = ((Number) Round33GameTests.call(consoleBe, "deuterium")).longValue();
            h.assertTrue(hopperBe.getItem(1).isEmpty() && (rods == 1 || tank > 0), "the hopper did not put the deuterium rod into the console");
            h.assertTrue(pellets == 0 && !hopperBe.getItem(0).isEmpty(), "the console took the neon pellet from the hopper");
        });
    }
}
