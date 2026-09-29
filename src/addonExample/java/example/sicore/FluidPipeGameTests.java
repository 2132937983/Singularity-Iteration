package example.sicore;

import com.miophas.singularity_iteration.core.api.machine.IPipeBlock;
import com.miophas.singularity_iteration.core.api.transport.PipeGrade;
import com.miophas.singularity_iteration.core.api.transport.PipeSize;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;

/** Uses registry IDs and public core/NeoForge APIs, just as a separate addon does. */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class FluidPipeGameTests {
    private static final int[] CAPACITIES = {400, 800, 2400, 4800, 800, 1600, 4800, 9600};

    @GameTest(template = "reactor_loop", batch = "fluid_pipes", timeoutTicks = 130)
    public static void allEightVariantsPlaceReloadDropAndRespectAggregateRate(GameTestHelper h) {
        Player player = h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var positions = new BlockPos[8];
        var outputs = new IFluidHandler[8][2];
        var inputs = new IFluidHandler[8];
        int[] rates = new int[8];
        int i = 0;
        for (PipeGrade grade : PipeGrade.values()) for (PipeSize size : PipeSize.values()) {
            var pos = new BlockPos(2 + i % 3 * 4, 2, 2 + i / 3 * 4);
            positions[i] = pos;
            h.setBlock(pos.west(), block("build/block_iron_tank"));
            h.setBlock(pos.east(), block("build/block_iron_tank"));
            String path = "pipe/fluid_pipe_" + grade.getSerializedName() + "_" + size.getSerializedName();
            var stack = new ItemStack(BuiltInRegistries.ITEM.get(id(path)));
            h.assertTrue(stack.getItem() instanceof BlockItem && stack.getMaxStackSize() == 64, "Missing/incorrect pipe item: " + path);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var support = h.absolutePos(pos.west());
            var hit = new BlockHitResult(Vec3.atCenterOf(support).add(0.5, 0, 0), Direction.EAST, support, false);
            var context = new BlockPlaceContext(h.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit);
            h.assertTrue(((BlockItem) stack.getItem()).place(context).consumesAction(), "Placement failed: " + path);
            BlockState state = h.getBlockState(pos);
            h.assertTrue(state.getValue(sizeProperty(state)) == size, "Placement lost size: " + path);
            h.assertTrue(port(h, pos, Direction.WEST) != null && port(h, pos, Direction.EAST) == null
                && port(h, pos, null) == null, "Placement must open only clicked port");
            wrench(h, player, pos, Direction.EAST);
            IFluidHandler handler = port(h, pos, Direction.WEST);
            h.assertTrue(handler.getTankCapacity(0) == CAPACITIES[i], "Wrong actual tank capacity: " + path);
            rates[i] = CAPACITIES[i] / 20;
            h.assertTrue(((IPipeBlock) h.getBlockEntity(pos)).getTransferRate() == rates[i], "Wrong addon rate");
            h.assertTrue(handler.fill(new FluidStack(Fluids.WATER, 20000), SIMULATE) == CAPACITIES[i]
                && handler.getFluidInTank(0).isEmpty(), "Simulation changed storage");
            h.assertTrue(handler.fill(new FluidStack(Fluids.WATER, 20000), EXECUTE) == CAPACITIES[i], "Wrong fill capacity");
            h.assertTrue(handler.fill(new FluidStack(Fluids.LAVA, 1), EXECUTE) == 0, "Mixed fluids accepted");
            wrench(h, player, pos, Direction.WEST);
            h.assertTrue(port(h, pos, Direction.WEST) == null && handler.drain(1, EXECUTE).isEmpty(), "Closed cached port still drains");
            wrench(h, player, pos, Direction.WEST);

            BlockEntity original = h.getBlockEntity(pos);
            var saved = original.saveWithFullMetadata(h.getLevel().registryAccess());
            var absolute = h.absolutePos(pos);
            h.getLevel().removeBlockEntity(absolute);
            var restored = BlockEntity.loadStatic(absolute, h.getBlockState(pos), saved, h.getLevel().registryAccess());
            h.assertTrue(restored != null, "Pipe entity did not load");
            h.getLevel().setBlockEntity(restored);
            inputs[i] = port(h, pos, Direction.WEST);
            h.assertTrue(inputs[i] != null && inputs[i].getFluidInTank(0).getAmount() == CAPACITIES[i], "Reload lost fluid or ports");
            var drops = Block.getDrops(state, h.getLevel(), absolute, restored);
            h.assertTrue(drops.size() == 1 && drops.getFirst().is(BuiltInRegistries.ITEM.get(id(path))), "Wrong size/material drop");
            var clone = state.getBlock().getCloneItemStack(state, hit, h.getLevel(), absolute, player);
            h.assertTrue(clone.is(drops.getFirst().getItem()), "Pick block lost size");
            var recipe = h.getLevel().getRecipeManager().byKey(id(path));
            h.assertTrue(recipe.isPresent() && recipe.get().value().getResultItem(h.getLevel().registryAccess()).is(clone.getItem()), "Missing/wrong recipe");
            int count = switch (size) { case TINY -> 6; case SMALL -> 3; case MEDIUM -> 2; case LARGE -> 1; };
            h.assertTrue(recipe.get().value().getResultItem(h.getLevel().registryAccess()).getCount() == count, "Wrong IC2 recipe yield");
            outputs[i][0] = port(h, pos.west(), null);
            outputs[i][1] = port(h, pos.east(), null);
            i++;
        }
        // Measure real server tickers. Two open machine outputs share ONE rated budget.
        int[] previous = new int[8], ticks = {0};
        h.onEachTick(() -> {
            if (++ticks[0] > 100) { h.succeed(); return; }
            for (int n = 0; n < 8; n++) {
                int amount = outputs[n][0].getFluidInTank(0).getAmount() + outputs[n][1].getFluidInTank(0).getAmount();
                int delta = amount - previous[n];
                h.assertTrue(delta <= rates[n], "Multiple faces exceeded aggregate rate: " + n + " / " + delta);
                if (ticks[0] > 3) h.assertTrue(delta == rates[n], "Pipe failed rated output: " + n + " / " + delta);
                int refilled = inputs[n].fill(new FluidStack(Fluids.WATER, 20000), EXECUTE);
                h.assertTrue(refilled == delta, "Fluid lost/duplicated: " + n);
                previous[n] = amount;
            }
        });
    }

    @GameTest(template = "empty", batch = "fluid_pipes", timeoutTicks = 50)
    public static void pipeBalancingAndLegacyStorageRemainCompatible(GameTestHelper h) {
        Player player = h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var a = new BlockPos(1, 1, 2);
        var b = a.east();
        h.setBlock(a, sized("bronze", PipeSize.TINY));
        h.setBlock(b, sized("steel", PipeSize.LARGE));
        wrench(h, player, a, Direction.EAST);
        var first = port(h, a, Direction.EAST);
        var second = port(h, b, Direction.WEST);
        h.assertTrue(first != null && second != null, "Pipe joint not opened at both ends");
        first.fill(new FluidStack(Fluids.WATER, 400), EXECUTE);
        var legacy = new BlockPos(1, 1, 0);
        h.setBlock(legacy, block("pipe/block_pipe_water"));
        var old = port(h, legacy, null);
        h.assertTrue(old != null && old.getTankCapacity(0) == 1000, "Legacy pipe capacity changed");
        old.fill(new FluidStack(Fluids.LAVA, 700), EXECUTE);
        var original = h.getBlockEntity(legacy);
        var saved = original.saveWithFullMetadata(h.getLevel().registryAccess());
        h.assertTrue(saved.getString("id").equals("mio_icif:pipe_water"), "Legacy BE ID changed");
        h.getLevel().removeBlockEntity(h.absolutePos(legacy));
        h.getLevel().setBlockEntity(BlockEntity.loadStatic(h.absolutePos(legacy), h.getBlockState(legacy), saved, h.getLevel().registryAccess()));
        h.assertTrue(port(h, legacy, null).getFluidInTank(0).getAmount() == 700, "Legacy save lost fluid");
        h.runAfterDelay(3, () -> {
            h.assertTrue(first.getFluidInTank(0).getAmount() == 200 && second.getFluidInTank(0).getAmount() == 200,
                "Pipe equalization must not use the 20 mB/t machine-output limit");
            wrench(h, player, a, Direction.EAST);
            h.assertTrue(port(h, a, Direction.EAST) == null && port(h, b, Direction.WEST) == null, "Disconnect left one end open");
            h.assertTrue(first.fill(new FluidStack(Fluids.WATER, 1), EXECUTE) == 0 && second.drain(1, EXECUTE).isEmpty(), "Stale joint handlers still work");
            h.succeed();
        });
    }

    @GameTest(template = "reactor_loop", batch = "fluid_pipes", timeoutTicks = 50)
    public static void passivePortsAndSpillThresholdMatchIc2(GameTestHelper h) {
        Player player = h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        BlockPos passive = new BlockPos(2, 2, 2);
        h.setBlock(passive, sized("bronze", PipeSize.SMALL));
        h.setBlock(passive.west(), block("build/block_iron_tank"));
        wrench(h, player, passive, Direction.WEST);
        port(h, passive.west(), null).fill(new FluidStack(Fluids.WATER, 4000), EXECUTE);
        int[] amounts = {1000, 1001, 2400};
        var spills = new BlockPos[3];
        for (int n = 0; n < 3; n++) {
            spills[n] = new BlockPos(2 + n * 4, 2, 8);
            h.setBlock(spills[n].below(), Blocks.STONE);
            h.setBlock(spills[n], sized("bronze", PipeSize.MEDIUM));
            wrench(h, player, spills[n], Direction.UP);
            port(h, spills[n], Direction.UP).fill(new FluidStack(Fluids.WATER, amounts[n]), EXECUTE);
        }
        h.runAfterDelay(4, () -> {
            h.assertTrue(port(h, passive, Direction.WEST).getFluidInTank(0).isEmpty(), "Passive pipe pulled fluid without a pump");
            for (var pos : spills) h.getLevel().destroyBlock(h.absolutePos(pos), false);
        });
        h.runAfterDelay(8, () -> {
            h.assertTrue(h.getBlockState(spills[0]).isAir(), "Exactly 1000 mB must not create a world source");
            for (int n = 1; n < 3; n++) h.assertTrue(h.getBlockState(spills[n]).getFluidState().isSource(), "Above 1000 mB did not spill a source");
            h.succeed();
        });
    }

    private static void wrench(GameTestHelper h, Player player, BlockPos pos, Direction side) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.get(id("item_tool_wrench"))));
        var absolute = h.absolutePos(pos);
        var hit = new BlockHitResult(Vec3.atCenterOf(absolute), side, absolute, false);
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, absolute, hit);
        NeoForge.EVENT_BUS.post(event);
        h.assertTrue(event.isCanceled(), "Wrench did not handle pipe");
    }

    private static IFluidHandler port(GameTestHelper h, BlockPos pos, Direction side) {
        return h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), side);
    }
    @SuppressWarnings("unchecked")
    private static EnumProperty<PipeSize> sizeProperty(BlockState state) {
        return (EnumProperty<PipeSize>) state.getBlock().getStateDefinition().getProperty("size");
    }
    private static BlockState sized(String grade, PipeSize size) {
        var state = block("pipe/fluid_pipe_" + grade).defaultBlockState();
        return state.setValue(sizeProperty(state), size);
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
}
